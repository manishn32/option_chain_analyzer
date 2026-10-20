package com.nse.analyser.services.schedulers;

import com.nse.analyser.dtos.nseResponse.Activity;
import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.enums.OptionType;
import com.nse.analyser.marketData.dto.fyers.OptionChainDataDTO;
import com.nse.analyser.marketData.dto.fyers.OptionChainItemDTO;
import com.nse.analyser.marketData.dto.fyers.OptionChainResponseDTO;
import com.nse.analyser.marketData.dto.fyers.StrikeContainer;
import com.nse.analyser.marketData.service.FyersMarketDataClient;
import com.nse.analyser.models.ChainSnapshot;
import com.nse.analyser.models.chainSnapShot.OptionLeg;
import com.nse.analyser.utils.DateTimeFormatters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.nse.analyser.utils.DateTimeFormatters.IST;

@Slf4j
@Service
@RequiredArgsConstructor
public class FyersOptionChainService {

    private final FyersMarketDataClient fyersMarketDataClient;

    public List<ChainSnapshot> getChainSnapshotsFromFyers(InstrumentType instrumentType, String symbol, Long expiry, Integer strikeCount, Boolean greeks){
        OptionChainResponseDTO optionChain = fyersMarketDataClient.getOptionChain(symbol, strikeCount, expiry, greeks);
        return convertIntoDataModel(optionChain,expiry);
    }

    public List<ChainSnapshot> convertIntoDataModel(
            OptionChainResponseDTO response,
            Long expiry) {

        log.info("Converting into data model - Fyers");

        OptionChainDataDTO data = response.getData();

        if (data == null || data.getOptionsChain() == null) {
            return Collections.emptyList();
        }

        Map<Double, StrikeContainer> strikeMap = new TreeMap<>();

        OptionChainItemDTO underlyingRow = null;

        long totalCallVolume = 0L;
        long totalPutVolume = 0L;

        long totalPutOi = 0L;
        long totalCallOi = 0L;

        for (OptionChainItemDTO dto : data.getOptionsChain()) {

            // Nifty row
            if (dto.getStrikePrice() < 0) {
                underlyingRow = dto;
                continue;
            }

            StrikeContainer container =
                    strikeMap.computeIfAbsent(
                            dto.getStrikePrice(),
                            ignored -> new StrikeContainer()
                    );

            if ("CE".equalsIgnoreCase(dto.getOptionType())) {

                if (container.getCe() != null) {
                    throw new IllegalStateException(
                            "Duplicate CE found for strike "
                                    + dto.getStrikePrice()
                    );
                }

                container.setCe(dto);

                totalCallVolume += dto.getVolume();
                totalCallOi +=dto.getOi();
            }
            else if ("PE".equalsIgnoreCase(dto.getOptionType())) {

                if (container.getPe() != null) {
                    throw new IllegalStateException(
                            "Duplicate PE found for strike "
                                    + dto.getStrikePrice()
                    );
                }

                container.setPe(dto);

                totalPutVolume += dto.getVolume();
                totalPutOi += dto.getOi();
            }
        }

        return buildSnapshots(
                strikeMap,
                underlyingRow,
                data,
                totalCallVolume,
                totalPutVolume,expiry, totalPutOi, totalCallOi
        );
    }

    private List<ChainSnapshot> buildSnapshots(
            Map<Double, StrikeContainer> strikeMap,
            OptionChainItemDTO underlyingRow,
            OptionChainDataDTO data,
            long totalCallVolume,
            long totalPutVolume,
            Long expiry,
            long totalPutOi,
            long totalCallOi) {

        String underlying =
                underlyingRow != null
                        ? underlyingRow.getExSymbol()
                        : null;

        String symbol = underlyingRow != null
                ? underlyingRow.getSymbol()
                : null;

        Double underlyingValue =
                underlyingRow != null
                        ? underlyingRow.getFp()
                        : null;

        Double pcrOi =
                data.getCallOi() > 0
                        ?
//                        (double) data.getPutOi() / data.getCallOi()
                        (double) totalPutOi / totalCallOi
                        : 0.0;

        Double pcrVolume =
                totalCallVolume > 0
                        ? (double) totalPutVolume / totalCallVolume
                        : 0.0;

        Activity ceActivity =
                new Activity(
                        data.getCallOi(),
                        totalCallVolume
                );

        Activity peActivity =
                new Activity(
                        data.getPutOi(),
                        totalPutVolume
                );

        Instant snapshotTime = Instant.now();

        List<ChainSnapshot> snapshots =
                new ArrayList<>(strikeMap.size());

        for (Map.Entry<Double, StrikeContainer> entry : strikeMap.entrySet()) {

            snapshots.add(
                    createSnapshot(
                            entry.getKey(),
                            entry.getValue(),
                            underlying,
                            underlyingValue,
                            pcrOi,
                            pcrVolume,
                            ceActivity,
                            peActivity,
                            snapshotTime,
                            expiry,
                            symbol
                    )
            );
        }

        return snapshots;
    }

    private ChainSnapshot createSnapshot(
            Double strike,
            StrikeContainer container,
            String underlying,
            Double underlyingValue,
            Double pcrOi,
            Double pcrVolume,
            Activity ceActivity,
            Activity peActivity,
            Instant snapshotTime,
            Long expiry,
            String symbol) {

        ChainSnapshot snapshot = new ChainSnapshot();

        snapshot.setSymbol(symbol);
        snapshot.setUnderlying(underlying);

        snapshot.setUnderlyingValue(underlyingValue);

        snapshot.setStrikePrice(strike);

        snapshot.setSnapshotTime(snapshotTime);

        snapshot.setPcr_oi(pcrOi);

        snapshot.setPcr_volume(pcrVolume);

        snapshot.setCeActivity(ceActivity);

        snapshot.setPeActivity(peActivity);

        snapshot.setExpiryDate(Instant.ofEpochSecond(expiry).atZone(IST).toLocalDate().toString());

        snapshot.setCE(
                container.getCe() == null
                        ? null
                        : mapOptionLeg(
                        container.getCe(),
                        underlying,
                        underlyingValue,
                        snapshotTime,
                        expiry
                )
        );

        snapshot.setPE(
                container.getPe() == null
                        ? null
                        : mapOptionLeg(
                        container.getPe(),
                        underlying,
                        underlyingValue,
                        snapshotTime,
                        expiry
                )
        );

        return snapshot;
    }

    private OptionLeg mapOptionLeg(
            OptionChainItemDTO dto,
            String underlying,
            Double underlyingValue,
            Instant snapshotTime,
            Long expiry) {

        return OptionLeg.builder()
                .underlying(underlying)
                .underlyingValue(
                        underlyingValue == null
                                ? 0.0
                                : underlyingValue
                )
                .LTP(dto.getLtp())
                .openInterest(dto.getOi())
                .changeInOpenInterest(dto.getOich())
                .oiChangePercent(dto.getOichp())
                .totalTradedVolume(dto.getVolume())
                .priceChange(dto.getLtpch())
                .priceChangePercent(dto.getLtpchp())
                .strikePrice(dto.getStrikePrice())
                .snapshotTimestamp(snapshotTime)
                .reference_oi(dto.getPrevOi())
                .expiryDate(LocalDate.from(Instant.ofEpochSecond(expiry).atZone(IST)))
                .optionType(
                        "CE".equalsIgnoreCase(dto.getOptionType())
                                ? OptionType.CE
                                : OptionType.PE
                )
                .build();
    }

    private Double calculatePcr(
            long putOi,
            long callOi) {

        if (callOi <= 0) {
            return 0.0;
        }

        return (double) putOi / callOi;
    }

}
