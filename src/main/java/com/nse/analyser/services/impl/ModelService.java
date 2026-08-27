package com.nse.analyser.services.impl;

import com.nse.analyser.dtos.nseResponse.*;
import com.nse.analyser.dtos.nseUnderLyingInfoDTOs.UnderLyingInfoResponse;
import com.nse.analyser.dtos.nseUnderLyingInfoDTOs.UnderlyingItem;
import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.enums.OptionType;
import com.nse.analyser.models.ChainSnapshot;
import com.nse.analyser.models.chainSnapShot.OptionLeg;
import com.nse.analyser.models.underlyingInfos.TradableFnO_Contracts;
import com.nse.analyser.records.OrderBookAnalytics;
import com.nse.analyser.services.schedulers.NseDataRetrievalService;
import com.nse.analyser.utils.DataTransformUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nse.analyser.services.impl.OptionMetricsService.determineOptionActivity;
import static com.nse.analyser.utils.NumberUtils.*;

@Service
@AllArgsConstructor
@Slf4j
public class ModelService {

    private final NseDataRetrievalService nseDataRetrievalService;

    public List<ChainSnapshot> getChainSnapshots(InstrumentType instrumentType, String symbol, String expiry) {
        log.info("Get option chain data for {} expiry {}",symbol,expiry);
        NseOptionChainResponse nseData = nseDataRetrievalService.fetchFromNse(instrumentType, symbol, expiry);
        log.info("Completed for {} expiry {}",symbol,expiry);
        return convertIntoDataModel(nseData);
    }

    public List<ChainSnapshot> convertIntoDataModel(NseOptionChainResponse optionChainResponses) {

        log.info("Converting into data model");

        List<ChainSnapshot> snapshots = new ArrayList<>();

        OptionChainRecords records = optionChainResponses.getRecords();
        OptionChainFiltered filtered = optionChainResponses.getFiltered();

        if(records == null || records.getData() == null) return snapshots;

        String nseTimestamp = records.getTimestamp();

        Double atmStrike = OptionMetricsService.determineAtmStrike(records.getUnderlyingValue(), DataTransformUtil.convertToIntegerList(records.getStrikePrices()));

        for(OptionStrikeData strikeData : records.getData()){
            ChainSnapshot chainSnapshot =
                    toChainSnapshot(strikeData,
                            Instant.now(),
                            filtered.getCE(),
                            filtered.getPE(),
                            atmStrike,
                            nseTimestamp);

            snapshots.add(chainSnapshot);
        }
        log.info("Completed converting into data model");
        return snapshots;

    }

    private ChainSnapshot toChainSnapshot(
            OptionStrikeData strikeData,
            Instant snapshotTime,
            Activity ceActivity,
            Activity peActivity,
            Double atmStrike,
            String nseTimestamp) {

        ChainSnapshot snapshot = new ChainSnapshot();

        snapshot.setUnderlying(
                strikeData.getCe() != null
                        ? strikeData.getCe().getUnderlying()
                        : strikeData.getPe() != null
                          ? strikeData.getPe().getUnderlying()
                          : null);

        snapshot.setUnderlyingValue(
                strikeData.getCe() != null
                        ? strikeData.getCe().getUnderlyingValue()
                        : strikeData.getPe() != null
                          ? strikeData.getPe().getUnderlyingValue()
                          : null);

        snapshot.setExpiryDate(strikeData.getExpiryDates());

        snapshot.setStrikePrice(strikeData.getStrikePrice());

        snapshot.setNseTimestamp(nseTimestamp);
        snapshot.setCE(toOptionLeg(strikeData.getCe(), OptionType.CE,ceActivity,peActivity,atmStrike,nseTimestamp,snapshotTime,strikeData.getExpiryDates()));
        snapshot.setPE(toOptionLeg(strikeData.getPe(),OptionType.PE,ceActivity,peActivity,atmStrike, nseTimestamp,snapshotTime,strikeData.getExpiryDates()));

        snapshot.setSnapshotTime(snapshotTime);

        snapshot.setPcr_oi(safeDivide(strikeData.getPe().getOpenInterest(),strikeData.getCe().getOpenInterest()));
        snapshot.setPcr_volume(safeDivide(strikeData.getPe().getTotalTradedVolume(), strikeData.getCe().getTotalTradedVolume()));

        return snapshot;
    }

    private OptionLeg toOptionLeg(
            OptionContract contract,
            OptionType type,
            Activity ceActivity,
            Activity peActivity,
            Double atmStrike,
            String nseTimestamp,
            Instant snapshotTime,
            String expiry) {

        if (contract == null) {
            return null;
        }

        OptionLeg leg = new OptionLeg();

        leg.setExpiryDate(
                LocalDate.parse(expiry)
        );
        leg.setNseTimestamp(LocalDateTime.parse(nseTimestamp));

        leg.setSnapshotTimestamp(snapshotTime);

        leg.setLTP(contract.getLastPrice());

        leg.setStrikePrice(contract.getStrikePrice());

        leg.setOpenInterest(contract.getOpenInterest());

        leg.setChangeInOpenInterest(
                contract.getChangeinOpenInterest());

        leg.setOiChangePercent(
                contract.getPchangeinOpenInterest());

        leg.setImpliedVolatility(
                contract.getImpliedVolatility());

        leg.setTotalTradedVolume(
                contract.getTotalTradedVolume());

        leg.setPriceChange(
                contract.getChange());

        leg.setPriceChangePercent(
                contract.getPChange());

        leg.setOptionRank(OptionMetricsService.determineRank(type,contract.getStrikePrice(),atmStrike));
//        OrderBookAnalytics orderBookAnalytics =  OrderBookAnalytics.calculate(contract.getTotalBuyQuantity(), contract.getTotalSellQuantity(), contract.getBuyQuantity1(), contract.getSellQuantity1());
//        leg.setOrderBookAnalytics(orderBookAnalytics);

        leg.setObi(
                roundTo2Decimals(OrderBookAnalytics.calculateImbalance(contract.getTotalBuyQuantity(), contract.getTotalSellQuantity()))
//                orderBookAnalytics.orderBookImbalance()
        );

        leg.setOiConcentration(
                safeDivideAndRound(contract.getOpenInterest(),
                        type == OptionType.CE ? ceActivity.getTotOI() : peActivity.getTotOI(),
                        2));

        leg.setVolumeConcentration(
                safeDivideAndRound(contract.getTotalTradedVolume(),
                        type == OptionType.CE ? ceActivity.getTotVol() : peActivity.getTotVol(),
                        2));

        leg.setBidQuantity(contract.getBuyQuantity1());
        leg.setAskQuantity(contract.getSellQuantity1());
        leg.setOptionActivity(determineOptionActivity(contract.getChange(), contract.getChangeinOpenInterest()));
        return leg;
    }

    public TradableFnO_Contracts getTradableFnOContracts() {
        TradableFnO_Contracts contracts = new TradableFnO_Contracts();
        UnderLyingInfoResponse allFnOContracts = nseDataRetrievalService.findAllFnO_Contracts();
        log.info("Fetched tradable FnO contracts from NSE");
        if(allFnOContracts == null) return contracts;

        List<UnderlyingItem> indexList = allFnOContracts.getData().getIndexList();
        List<String> indices = indexList.stream().map(UnderlyingItem::getSymbol).toList();
        List<UnderlyingItem> underlyingList = allFnOContracts.getData().getUnderlyingList();
        List<String> stocks = underlyingList.stream().map(UnderlyingItem::getSymbol).toList();
        contracts.setIndices(indices);
        contracts.setStocks(stocks);
        return contracts;
    }

    public Map<String, List<String>> mapIndicesAndExpiry(){
        Map<String, List<String>> mapOfSymbolsToExpiry = new HashMap<>();
        TradableFnO_Contracts tradableFnOContracts = getTradableFnOContracts();
        for(String index : tradableFnOContracts.getIndices()){
            List<String> strings = nseDataRetrievalService.fetchExpirationDates(index);
            if(!strings.isEmpty()) mapOfSymbolsToExpiry.put(index, strings);
        }
        return mapOfSymbolsToExpiry;
    }

    public Map<String, List<String>> mapSymbolAndExpiry(){
        Map<String, List<String>> mapOfSymbolsToExpiry = new HashMap<>();
        TradableFnO_Contracts tradableFnOContracts = getTradableFnOContracts();
        for(String index : tradableFnOContracts.getStocks()){
//            if(index.contains("&")) index = index.replace("&","%2526"); // Todo: unable to handle M&M and GT&C symbols
            List<String> strings = nseDataRetrievalService.fetchExpirationDates(index);
            if(!strings.isEmpty()) mapOfSymbolsToExpiry.put(index, strings);
        }
        return mapOfSymbolsToExpiry;
    }
}
