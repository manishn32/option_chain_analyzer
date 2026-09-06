package com.nse.analyser.services.jobs;

import com.nse.analyser.enums.TimeOfDay;
import com.nse.analyser.models.holidayInfo.Holiday;
import com.nse.analyser.models.holidayInfo.HolidayList;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import com.mongodb.MongoBulkWriteException;
import com.nse.analyser.constants.CollectionNames;
import com.nse.analyser.enums.DataCategory;
import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.enums.OptionRank;
import com.nse.analyser.models.ChainSnapshot;
import com.nse.analyser.models.SymbolsAndExpiry;
import com.nse.analyser.models.chainSnapShot.OptionLeg;
import com.nse.analyser.repositories.NseSnapshotRepository;
import com.nse.analyser.services.impl.ModelService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.mongodb.core.query.Query;

@Service
@Slf4j
@AllArgsConstructor
public class NseDataCollectionJob {
    private final ModelService modelService;
    private final NseSnapshotRepository nseSnapshotRepository;
    private final SnapshotIdGenerator snapshotIdGenerator;
    private final MongoTemplate mongoTemplate;

    public Map<InstrumentType, Map<String, List<String>>> findNfoExpires(){
        Map<InstrumentType, Map<String, List<String>>> symbolsAndExpires = new HashMap<>();
        Query queryForIndices = new Query();
        queryForIndices.addCriteria(Criteria.where("date").is(LocalDate.now()));
        queryForIndices.addCriteria(Criteria.where("instrumentType").is(InstrumentType.INDEX));
        Optional<SymbolsAndExpiry> resultForIndices = mongoTemplate.find(queryForIndices, SymbolsAndExpiry.class, CollectionNames.NSE_INDEX_EXPIRES).stream().findFirst();
        resultForIndices.ifPresent(symbolsAndExpiry -> symbolsAndExpires.put(InstrumentType.INDEX, symbolsAndExpiry.getSymbolsAndExpiry()));
        Query queryForStocks = new Query();
        queryForStocks.addCriteria(Criteria.where("date").is(LocalDate.now()));
        queryForStocks.addCriteria(Criteria.where("instrumentType").is(InstrumentType.EQUITY));
        Optional<SymbolsAndExpiry> stocksResults = mongoTemplate.find(queryForStocks, SymbolsAndExpiry.class, CollectionNames.NSE_EQUITY_EXPIRES).stream().findFirst();
        stocksResults.ifPresent(symbolsAndExpiry -> symbolsAndExpires.put(InstrumentType.EQUITY, symbolsAndExpiry.getSymbolsAndExpiry()));
        return symbolsAndExpires;
    }
    public void saveSymbolsAndExpires(Map<InstrumentType, Map<String, List<String>>> symbolsAndExpires){
        for (Map.Entry<InstrumentType, Map<String, List<String>>> instrumentTypeMapEntry : symbolsAndExpires.entrySet()) {
            switch (instrumentTypeMapEntry.getKey()) {
                case INDEX -> {
                    log.info("Saving into Mongo for instrumentType: {} symbolsAndExpiry: {}",instrumentTypeMapEntry.getKey(),instrumentTypeMapEntry.getValue());
                    SymbolsAndExpiry symbolsAndExpiry = new SymbolsAndExpiry();
                    symbolsAndExpiry.setDate(LocalDate.now());
                    symbolsAndExpiry.setInstrumentType(InstrumentType.INDEX);
                    symbolsAndExpiry.setSymbolsAndExpiry(instrumentTypeMapEntry.getValue());
                    mongoTemplate.save(symbolsAndExpiry, CollectionNames.NSE_INDEX_EXPIRES);
                }
                case EQUITY -> {
                    SymbolsAndExpiry symbolsAndExpiry = new SymbolsAndExpiry();
                    symbolsAndExpiry.setDate(LocalDate.now());
                    symbolsAndExpiry.setInstrumentType(InstrumentType.EQUITY);
                    symbolsAndExpiry.setSymbolsAndExpiry(instrumentTypeMapEntry.getValue());
                    mongoTemplate.save(symbolsAndExpiry, CollectionNames.NSE_EQUITY_EXPIRES);
                }
            }
        }
    }


    public int fillData(String symbol, String expiryDate, InstrumentType instrumentType){
        List<ChainSnapshot> snapshots =
                modelService.getChainSnapshots(
                        instrumentType,
                        symbol,
                        expiryDate
                );
        log.info("Saving into Mongo for symbol: {} expiry: {}",symbol,expiryDate);
        String collectionName =
                instrumentType == InstrumentType.INDEX
                        ? CollectionNames.NSE_INDEX_SNAPSHOT
                        : CollectionNames.NSE_STOCK_SNAPSHOT;

        final int batchSize = 500;

        for (int i = 0; i < snapshots.size(); i += batchSize) {

            int end = Math.min(i + batchSize, snapshots.size());

            List<ChainSnapshot> batch =
                    snapshots.subList(i, end);
            try {
                mongoTemplate.insert(batch, collectionName);
                log.info("Saved into Mongo for symbol: {} expiry: {} ",symbol,expiryDate);
            }
            catch (DuplicateKeyException dup){
                log.info("Data already present in mongoDB for symbol : {} and expiry {}",symbol,expiryDate);
            }
            catch (DataAccessException exp){
                    log.error("Writing failed in mongoDB for symbol : {} and expiry {} with message:{}",symbol,expiryDate,exp.getMessage());
            }
            catch (Exception ex){
                log.error("Actual exception class = {}", ex.getClass().getName(), ex);
                log.error("Exception class = {}", ex.getClass().getName(), ex);
//                log.error("Unexpected exception with Mongo bulk writes : {}",ex.getMessage());
            }

        }
        return snapshots.size();
//       mongoTemplate.save(snapshots, instrumentType == InstrumentType.INDEX ? CollectionNames.NSE_INDEX_SNAPSHOT : CollectionNames.NSE_STOCK_SNAPSHOT).size();
    }
    public void fillDataForNifty(String symbol, String expiryDate, DataCategory table, InstrumentType instrumentType) {
        log.info("Starting to Fill data for symbol: {} expiry: {}",symbol,expiryDate);
        List<ChainSnapshot> snapshots =
                modelService.getChainSnapshots(
                        instrumentType,
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
        nseSnapshotRepository.saveAll(newSnapshots,table.name());
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

    public List<LocalDate> findNseHolidays(){
        Query query = new Query();
        query.addCriteria(Criteria.where("year").is(String.valueOf(LocalDate.now().getYear())));
        Optional<HolidayList> first = mongoTemplate.find(query, HolidayList.class ,CollectionNames.NSE_TRADING_HOLIDAYS).stream().findAny();
        if(first.isPresent()){
            List<Holiday> holidays = first.get().getHolidays();
            return holidays.stream()
                    .map(Holiday::getDate)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public void saveNseHolidays(List<Holiday> nseHolidays){
        log.info("Saving Holidays to DB");
        HolidayList holidayList = new HolidayList();
        holidayList.setYear(String.valueOf(LocalDate.now().getYear()));
        holidayList.setHolidays(nseHolidays);
        mongoTemplate.save(holidayList, CollectionNames.NSE_TRADING_HOLIDAYS);
        log.info("Saved Holidays to DB");
    }
}
