package com.nse.analyser.services.jobs;

import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.enums.OptionRank;
import com.nse.analyser.models.ChainSnapshot;
import com.nse.analyser.models.chainSnapShot.OptionLeg;
import com.nse.analyser.repositories.NseSnapshotRepository;
import com.nse.analyser.services.impl.ModelService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@AllArgsConstructor
public class NseDataCollectionJob {
    private final ModelService modelService;
    private final NseSnapshotRepository nseSnapshotRepository;
    private final SnapshotIdGenerator snapshotIdGenerator;

    public void fillDataForNifty(String symbol, String expiryDate) {
        log.info("Starting to Fill data for symbol: {} expiry: {}",symbol,expiryDate);
        List<ChainSnapshot> snapshots =
                modelService.getChainSnapshots(
                        InstrumentType.INDEX,
                        symbol,
                        expiryDate
                );

        snapshots = filterAroundAtm(snapshots);

        List<OptionLeg> optionLegs = snapshots.stream()
                .flatMap(snapshot -> Stream.of(
                        snapshot.getCE(),
                        snapshot.getPE()
                ))
                .filter(Objects::nonNull)
                .toList();

        optionLegs.forEach(snapshot ->
                snapshot.setId(
                        snapshotIdGenerator.generate(
                                snapshot.getUnderlying(),
                                snapshot.getExpiryDate(),
//                                snapshot.getStrikePrice(),
//                                snapshot.getOptionType(),
                                snapshot.getNseTimestamp()
                        )
                ));
        Set<String> existingIds =
                nseSnapshotRepository.findExistingIds(optionLegs.stream().map(OptionLeg::getId).toList());

        List<OptionLeg> newSnapshots =
                optionLegs.stream()
                .filter(s -> !existingIds.contains(s.getId()))
                .toList();
        nseSnapshotRepository.saveAll(newSnapshots);
        if(optionLegs.isEmpty()) return;
        nseSnapshotRepository.insertKeys(List.of(optionLegs.getFirst()));
        log.info("Completed saving data into 1m table for symbol: {} expiry: {}",symbol,expiryDate);
    }
    private List<ChainSnapshot> filterAroundAtm(List<ChainSnapshot> snapshots) {

        if(snapshots == null || snapshots.isEmpty()) return Collections.emptyList();

        ChainSnapshot atm;
        Optional<ChainSnapshot> atmOptional = snapshots.stream()
                .filter(s -> s.getCE() != null)
                .filter(s -> s.getCE().getOptionRank() == OptionRank.ATM)
                .findFirst()
//                .orElseThrow()
                ;

        if(atmOptional.isPresent()){
            atm = atmOptional.get();
        }else{
            log.warn("No ATM strikes present for the given data {}",snapshots);
            return Collections.emptyList();
        }
        double atmStrike = atm.getCE().getStrikePrice();

        return snapshots.stream()
                .filter(s -> s.getCE() != null)
                .filter(s -> Math.abs(
                        s.getCE().getStrikePrice() - atmStrike
                ) <= 10 * 50) // NIFTY 50-point intervals
                .sorted(Comparator.comparingDouble(
                        s -> s.getCE().getStrikePrice()
                ))
                .toList();
    }
}
