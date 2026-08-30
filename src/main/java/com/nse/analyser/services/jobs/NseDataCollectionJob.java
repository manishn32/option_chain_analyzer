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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@AllArgsConstructor
public class NseDataCollectionJob {
    private final ModelService modelService;
    private final NseSnapshotRepository nseSnapshotRepository;

//    @EventListener(ApplicationReadyEvent.class)
//    public void fillDataForNifty(){
//        List<ChainSnapshot> nifty = modelService.getChainSnapshots(InstrumentType.INDEX, "NIFTY", "01-Sep-2026");
//        List<OptionLeg> collect1 = nifty.stream().map(ChainSnapshot::getCE).collect(Collectors.toList());
//        List<OptionLeg> collect = nifty.stream().map(ChainSnapshot::getPE).collect(Collectors.toList());
//        nseSnapshotRepository.saveAll(collect1);
//        nseSnapshotRepository.saveAll(collect);
//    }

    @EventListener(ApplicationReadyEvent.class)
    public void fillDataForNifty() {

        List<ChainSnapshot> snapshots =
                modelService.getChainSnapshots(
                        InstrumentType.INDEX,
                        "NIFTY",
                        "01-Sep-2026"
                );

        snapshots = filterAroundAtm(snapshots);

        List<OptionLeg> optionLegs = snapshots.stream()
                .flatMap(snapshot -> Stream.of(
                        snapshot.getCE(),
                        snapshot.getPE()
                ))
                .filter(Objects::nonNull)
                .toList();

        nseSnapshotRepository.saveAll(optionLegs);
    }
    private List<ChainSnapshot> filterAroundAtm(List<ChainSnapshot> snapshots) {

        ChainSnapshot atm = snapshots.stream()
                .filter(s -> s.getCE() != null)
                .filter(s -> s.getCE().getOptionRank() == OptionRank.ATM)
                .findFirst()
                .orElseThrow();

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
