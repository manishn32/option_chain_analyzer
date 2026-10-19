package com.nse.analyser.services.impl;

import com.nse.analyser.dtos.nseResponse.Activity;
import com.nse.analyser.dtos.nseResponse.NseOptionChainResponse;
import com.nse.analyser.dtos.nseResponse.OptionChainFiltered;
import com.nse.analyser.dtos.nseResponse.OptionChainRecords;
import com.nse.analyser.dtos.nseResponse.OptionContract;
import com.nse.analyser.dtos.nseResponse.OptionStrikeData;
import com.nse.analyser.dtos.nseUnderLyingInfoDTOs.UnderLyingInfoResponse;
import com.nse.analyser.dtos.nseUnderLyingInfoDTOs.UnderlyingItem;
import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.enums.OptionType;
import com.nse.analyser.models.ChainSnapshot;
import com.nse.analyser.models.chainSnapShot.OptionLeg;
import com.nse.analyser.models.contractSnapShot.MarketDepth;
import com.nse.analyser.models.holidayInfo.Holiday;
import com.nse.analyser.models.underlyingInfos.TradableFnO_Contracts;
import com.nse.analyser.records.OrderBookAnalytics;
import com.nse.analyser.services.schedulers.FyersOptionChainService;
import com.nse.analyser.services.schedulers.NseDataRetrievalService;
import com.nse.analyser.utils.DataTransformUtil;
import com.nse.analyser.utils.DateTimeFormatters;
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
import java.util.concurrent.ConcurrentHashMap;

import static com.nse.analyser.services.impl.OptionMetricsService.determineOptionActivity;
import static com.nse.analyser.utils.NumberUtils.roundTo2Decimals;
import static com.nse.analyser.utils.NumberUtils.safeDivide;
import static com.nse.analyser.utils.NumberUtils.safeDivideAndRound;

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
        if(records == null) return snapshots;
        OptionChainFiltered filtered = optionChainResponses.getFiltered();

        if( records.getData() == null) return snapshots;

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
            if( chainSnapshot != null) {
                chainSnapshot.setCeActivity(filtered.getCE());
                chainSnapshot.setPeActivity(filtered.getPE());
                snapshots.add(chainSnapshot);
            }
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

        if(strikeData.getCe().getUnderlying() == null || strikeData.getCe().getUnderlying().isEmpty() || strikeData.getCe().getUnderlying().isBlank()){
            return null;
        }
        ChainSnapshot snapshot = new ChainSnapshot();

//        snapshot.setId(
//                strikeData.getCe().getUnderlying()
//                        + "_"
//                        + strikeData.getCe().getExpiryDate()
//                        + "_"
//                        + nseTimestamp
//                        + "_"
//                        + strikeData.getStrikePrice()
//        );
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

        snapshot.setNseTimestamp(LocalDateTime.parse(nseTimestamp, DateTimeFormatters.nseTimestampFormatter));
        snapshot.setCE(
                toOptionLeg(strikeData.getCe(),
                        OptionType.CE,
                        ceActivity,
                        peActivity,
                        atmStrike,
                        nseTimestamp,
                        snapshotTime,
                        strikeData.getExpiryDates()));
        snapshot.setPE(
                toOptionLeg(strikeData.getPe(),
                        OptionType.PE,
                        ceActivity,
                        peActivity,
                        atmStrike,
                        nseTimestamp,
                        snapshotTime,
                        strikeData.getExpiryDates()));

        snapshot.setSnapshotTime(snapshotTime);

        snapshot.setPcr_oi(
                safeDivide(strikeData.getPe().getOpenInterest()
                        ,strikeData.getCe().getOpenInterest())
        );
        snapshot.setPcr_volume(
                safeDivide(strikeData.getPe().getTotalTradedVolume()
                        , strikeData.getCe().getTotalTradedVolume())
        );

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

        MarketDepth marketDepth = new MarketDepth();
        marketDepth.setBestAskPrice(contract.getBuyPrice1());
        marketDepth.setBestAskQuantity(contract.getBuyQuantity1());
        marketDepth.setBestBidPrice(contract.getSellPrice1());
        marketDepth.setBestBidQuantity(contract.getSellQuantity1());
        marketDepth.setTotalBuyQuantity(contract.getTotalBuyQuantity());
        marketDepth.setTotalSellQuantity(contract.getTotalSellQuantity());
        leg.setUnderlying(contract.getUnderlying());
        leg.setUnderlyingValue(contract.getUnderlyingValue());
        leg.setExpiryDate(
                LocalDate.parse(expiry, DateTimeFormatters.expiryDateFormatter)
        );
        leg.setOptionType(type);
        leg.setNseTimestamp(
//                nseTimestamp
                LocalDateTime.parse(nseTimestamp, DateTimeFormatters.nseTimestampFormatter)
        );

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
        leg.setMarketDepth(marketDepth);
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

    public Map<InstrumentType, Map<String, List<String>>> mapIndicesAndExpiry(){
        Map<InstrumentType, Map<String, List<String>>> mapOfSymbolsToExpiryBoth = new ConcurrentHashMap<>();
        Map<String, List<String>> mapOfSymbolsToExpiry = new ConcurrentHashMap<>();
        TradableFnO_Contracts tradableFnOContracts = getTradableFnOContracts();
        Map<String, List<String>> mapOfIndexesToExpiry = new ConcurrentHashMap<>();
        for (String index : tradableFnOContracts.getIndices()){
            List<String> strings = nseDataRetrievalService.fetchExpirationDates(index);
            if(!strings.isEmpty()) mapOfIndexesToExpiry.put(index, strings);
        }
        mapOfSymbolsToExpiryBoth.put(InstrumentType.INDEX,mapOfIndexesToExpiry);
        for(String index : tradableFnOContracts.getStocks()){
            List<String> strings = nseDataRetrievalService.fetchExpirationDates(index);
            if(!strings.isEmpty()){
                mapOfSymbolsToExpiry.put(index, strings);}
        }
        mapOfSymbolsToExpiryBoth.put(InstrumentType.EQUITY, mapOfSymbolsToExpiry);

        return mapOfSymbolsToExpiryBoth;
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

    public List<Holiday> fetchAllNseHolidays(){
       return nseDataRetrievalService.findAllNseHolidays();
    }
}
