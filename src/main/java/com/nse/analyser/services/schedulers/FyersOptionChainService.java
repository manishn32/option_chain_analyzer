package com.nse.analyser.services.schedulers;

import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.enums.OptionType;
import com.nse.analyser.marketData.dto.fyers.OptionChainDataDTO;
import com.nse.analyser.marketData.dto.fyers.OptionChainItemDTO;
import com.nse.analyser.marketData.dto.fyers.OptionChainResponseDTO;
import com.nse.analyser.marketData.dto.fyers.StrikeContainer;
import com.nse.analyser.marketData.service.FyersMarketDataClient;
import com.nse.analyser.models.ChainSnapshot;
import com.nse.analyser.models.chainSnapShot.OptionLeg;
import com.nse.analyser.models.chainSnapShot.StrikeSnapshot;
import com.nse.analyser.utils.NumberUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.nse.analyser.utils.DateTimeFormatters.IST;

@Slf4j
@Service
@RequiredArgsConstructor
public class FyersOptionChainService {

    private final FyersMarketDataClient fyersMarketDataClient;

    public ChainSnapshot getChainSnapshotsFromFyers(InstrumentType instrumentType, String symbol, Long expiry, Integer strikeCount, Boolean greeks){
        OptionChainResponseDTO optionChain = fyersMarketDataClient.getOptionChain(symbol, strikeCount, expiry, greeks);
        return convertIntoDataModel(optionChain,expiry);
    }

    public ChainSnapshot convertIntoDataModel(
            OptionChainResponseDTO response,
            Long expiry) {

        log.info("Converting into data model - Fyers");

        OptionChainDataDTO data = response.getData();

        if (data == null || data.getOptionsChain() == null) {
            return null;
        }

        Map<Double, StrikeContainer> strikeMap = new TreeMap<>();

        OptionChainItemDTO underlyingRow = null;

        long totalCallVolume = 0L;
        long totalPutVolume = 0L;

        long totalPutOi = 0L;
        long totalCallOi = 0L;

        long totalPutOiChange = 0L;
        long totalCallOiChange = 0L;

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
                totalCallOiChange += dto.getOich();
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
                totalPutOiChange += dto.getOich();
            }
        }

        return buildSnapshots(
                strikeMap,
                underlyingRow,
                data,
                totalCallVolume,
                totalPutVolume,expiry, totalPutOi, totalCallOi , totalPutOiChange, totalCallOiChange
        );
    }

    private ChainSnapshot buildSnapshots(
            Map<Double, StrikeContainer> strikeMap,
            OptionChainItemDTO underlyingRow,
            OptionChainDataDTO data,
            long totalCallVolume,
            long totalPutVolume,
            Long expiry,
            long totalPutOi,
            long totalCallOi,
            long totalPutOiChange,
            long totalCallOiChange) {

        String underlying =
                underlyingRow != null
                        ? underlyingRow.getExSymbol()
                        : null;

        String symbol = underlyingRow != null
                ? underlyingRow.getSymbol()
                : null;

        Double underlyingValue =
                underlyingRow != null
                        ? underlyingRow.getLtp()
                        : null;

        Double pcrOi =
                data.getCallOi() > 0
                        ?
                        NumberUtils.roundTo2Decimals((double) totalPutOi / totalCallOi)
                        : 0.0;

        Double pcrVolume =
                totalCallVolume > 0
                        ? NumberUtils.roundTo2Decimals((double) totalPutVolume / totalCallVolume)
                        : 0.0;


        Instant snapshotTime = Instant.now();

//        ChainSnapshot snapshots =
//                new ChainSnapshot();
        ChainSnapshot snapshot = new ChainSnapshot();
        snapshot.setSymbol(symbol);
        snapshot.setUnderlying(underlying);
        snapshot.setIndiavixData(data.getIndiavixData());
        snapshot.setUnderlyingValue(underlyingValue);
        snapshot.setUnderlyingChange(underlyingRow != null
                ? underlyingRow.getLtpch()
                : null);
        snapshot.setUnderlyingChangeP(underlyingRow != null
                ? underlyingRow.getLtpchp()
                : null);
        snapshot.setFuturesPrice(underlyingRow != null
                ? underlyingRow.getFp()
                : null);
        snapshot.setFuturesChange(underlyingRow != null
                ? underlyingRow.getFpch()
                : null);
        snapshot.setFuturesChangeP(underlyingRow != null
                ? underlyingRow.getFpchp()
                : null);
        snapshot.setPremium((underlyingRow != null
                ? underlyingRow.getFp()
                : 0.0 ) - (underlyingValue != null ? underlyingValue:0.0) );


        snapshot.setSnapshotTime(snapshotTime);




        snapshot.setPcr(NumberUtils.safeDivide(data.getPutOi(),data.getCallOi()));
        snapshot.setPcr_oi(pcrOi);
        snapshot.setPcr_oi_change(NumberUtils.safeDivide(totalPutOiChange,totalCallOiChange));
        snapshot.setPcr_volume(pcrVolume);
        snapshot.setExpiryDate(Instant.ofEpochSecond(expiry).atZone(IST).toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")).toString());


             return        createSnapshot(
                            strikeMap.values().stream().toList(),
                            snapshotTime,
                            expiry,
                            snapshot,
                     underlyingRow != null
                             ? underlyingRow.getLtpchp()
                             : null
            );
    }

    private ChainSnapshot createSnapshot(
            List<StrikeContainer> containers,
            Instant snapshotTime,
            Long expiry,
            ChainSnapshot snapshot,
            Double underlyingChange) {

        List<StrikeSnapshot> strikeSnapshots = new ArrayList<>();

        for (StrikeContainer container : containers) {

            OptionLeg ce = null;
            OptionLeg pe = null;
            Double strikePrice = null;

            if (container.getCe() != null) {

                ce = mapOptionLeg(
                        container.getCe(),
                        snapshotTime,
                        expiry,
                        underlyingChange
                );

                strikePrice = ce.getStrikePrice();
            }

            if (container.getPe() != null) {

                pe = mapOptionLeg(
                        container.getPe(),
                        snapshotTime,
                        expiry,underlyingChange

                );

                if (strikePrice == null) {
                    strikePrice = pe.getStrikePrice();
                }
            }

            strikeSnapshots.add(
                    StrikeSnapshot.builder()
                            .strikePrice(strikePrice)
                            .ce(ce)
                            .pe(pe)
                            .build()
            );
        }

        snapshot.setStrikes(strikeSnapshots);

        return snapshot;
    }

    private OptionLeg mapOptionLeg(
            OptionChainItemDTO dto,
//            String underlying,
//            Double underlyingValue,
            Instant snapshotTime,
            Long expiry,
            Double underlyingChange) {

        return OptionLeg.builder()
//                .underlying(underlying)
//                .underlyingValue(
//                        underlyingValue == null
//                                ? 0.0
//                                : underlyingValue
//                )
                .symbol(dto.getSymbol())
                .underlyingChange(underlyingChange)
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
                .greeks(dto.getGreeks())
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
