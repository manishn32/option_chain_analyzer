package com.nse.analyser.services.jobs;

import com.nse.analyser.dtos.nseResponse.DataFillRecord;
import com.nse.analyser.enums.TimeOfDay;
import com.nse.analyser.models.holidayInfo.Holiday;
import com.nse.analyser.models.holidayInfo.HolidayList;
import com.nse.analyser.services.impl.ChainSnapshotServiceImpl;
import com.nse.analyser.services.schedulers.FyersOptionChainService;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import com.nse.analyser.constants.CollectionNames;
import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.enums.OptionRank;
import com.nse.analyser.models.ChainSnapshot;
import com.nse.analyser.models.SymbolsAndExpiry;
import com.nse.analyser.models.chainSnapShot.OptionLeg;
import com.nse.analyser.repositories.NseSnapshotRepository;
import com.nse.analyser.services.impl.ModelService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

import static com.nse.analyser.utils.DateTimeFormatters.IST;
import static com.nse.analyser.utils.DateTimeFormatters.expiryDateFormatter;

@Service
@Slf4j
@AllArgsConstructor
public class NseDataCollectionJob {
    private final ModelService modelService;
    private final NseSnapshotRepository nseSnapshotRepository;
    private final SnapshotIdGenerator snapshotIdGenerator;
    private final MongoTemplate mongoTemplate;
    private final FyersOptionChainService fyersOptionChainService;

    private final ChainSnapshotServiceImpl chainSnapshotServiceImpl;
    public Map<InstrumentType, Map<String, List<String>>> findNfoExpires(){
        Map<InstrumentType, Map<String, List<String>>> symbolsAndExpires = new HashMap<>();
        Query queryForIndices = new Query();
        queryForIndices.addCriteria(Criteria.where("date").is(LocalDate.now(IST)));
        queryForIndices.addCriteria(Criteria.where("instrumentType").is(InstrumentType.INDEX));
        Optional<SymbolsAndExpiry> resultForIndices = mongoTemplate.find(queryForIndices, SymbolsAndExpiry.class, CollectionNames.NSE_INDEX_EXPIRES).stream().findFirst();
        resultForIndices.ifPresent(symbolsAndExpiry -> symbolsAndExpires.put(InstrumentType.INDEX, symbolsAndExpiry.getSymbolsAndExpiry()));
        Query queryForStocks = new Query();
        queryForStocks.addCriteria(Criteria.where("date").is(LocalDate.now(IST)));
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
                    symbolsAndExpiry.setDate(LocalDate.now(IST));
                    symbolsAndExpiry.setInstrumentType(InstrumentType.INDEX);
                    symbolsAndExpiry.setSymbolsAndExpiry(instrumentTypeMapEntry.getValue());
                    mongoTemplate.save(symbolsAndExpiry, CollectionNames.NSE_INDEX_EXPIRES);
                }
                case EQUITY -> {
                    SymbolsAndExpiry symbolsAndExpiry = new SymbolsAndExpiry();
                    symbolsAndExpiry.setDate(LocalDate.now(IST));
                    symbolsAndExpiry.setInstrumentType(InstrumentType.EQUITY);
                    symbolsAndExpiry.setSymbolsAndExpiry(instrumentTypeMapEntry.getValue());
                    mongoTemplate.save(symbolsAndExpiry, CollectionNames.NSE_EQUITY_EXPIRES);
                }
            }
        }
    }


    public DataFillRecord fillData(String symbol, Long expiryDate, InstrumentType instrumentType, String intradayKey, TimeOfDay timeOfDay){

//        List<ChainSnapshot> snapshots =
//                modelService.getChainSnapshots(
//                        instrumentType,
//                        symbol,
//                        expiryDate
//                );
        List<ChainSnapshot> snapshots = fyersOptionChainService.getChainSnapshotsFromFyers(instrumentType,symbol,expiryDate,10,false);
        String expiryDateFormatted = Instant.ofEpochSecond(expiryDate).atZone(IST).toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        //Here we have to get ChainSnapshot exactly like the
        log.info("Saving into Mongo for symbol: {} expiry: {} candles: {}",symbol,expiryDateFormatted,snapshots.size());
        String collectionName =
                instrumentType == InstrumentType.INDEX
                        ? CollectionNames.NSE_INDEX_SNAPSHOT
                        : CollectionNames.NSE_STOCK_SNAPSHOT;
        LocalDateTime nseTimeStamp = null;
        final int batchSize = 500;
        for (int i = 0; i < snapshots.size(); i += batchSize) {
            /**
             *   strikeData.getCe().getUnderlying()
             * //                        + "_"
             * //                        + strikeData.getCe().getExpiryDate()
             * //                        + "_"
             * //                        + nseTimestamp
             * //                        + "_"
             * //                        + strikeData.getStrikePrice()
             * */
            nseTimeStamp = snapshots.get(i).getNseTimestamp();

            int end = Math.min(i + batchSize, snapshots.size());

            List<ChainSnapshot> batch =
                    snapshots.subList(i, end);

            for (int j = i; j < end; j++) {

                ChainSnapshot snapshot = batch.get(j);

                if(timeOfDay != TimeOfDay.DURING_DAY){
////                        snapshotIdGenerator.generate(symbol,LocalDate.parse(expiryDate,expiryDateFormatter),nseTimeStamp)
                    snapshot.setIdentification(
                            symbol + "_" +
                                    snapshot.getExpiryDate() + "_" +
                                    snapshot.getStrikePrice() + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    );
                }
                else{
                    snapshot.setIdentification(intradayKey);
                }
            }
            try {
                BulkOperations bulkOps =
                        mongoTemplate.bulkOps(
                                BulkOperations.BulkMode.UNORDERED,
                                ChainSnapshot.class,
                                collectionName
                        );

                for (ChainSnapshot snapshot : batch) {

                    Query query =
                            Query.query(
                                    Criteria.where("_id")
                                            .is(snapshot.getIdentification())
                            );

                    bulkOps.replaceOne(
                            query,
                            snapshot,
                            FindAndReplaceOptions.options().upsert()
                    );
                }

                bulkOps.execute();
//                mongoTemplate.upsert(batch, collectionName);
                log.info("Saved into Mongo for symbol: {} expiry: {} and collection: {} candles: {}",symbol, expiryDateFormatted ,collectionName,batch.size());
            }
            catch (DuplicateKeyException dup){
                log.info("Data already present in mongoDB for symbol : {} and expiry {} with Nse Timestamp = {}",symbol,expiryDate,nseTimeStamp);
                log.debug("Duplicate key found for symbol {} with key {} ", snapshots.getFirst().getUnderlying(), snapshots.getFirst().getSymbol());
                throw dup;
            }
            catch (DataAccessException exp){
                    log.error("Writing failed in mongoDB for symbol : {} and expiry {} with message:{}",symbol,expiryDate,exp.getMessage());
                throw exp;
            }
            catch (Exception ex){
                log.error("Actual exception class = {}", ex.getClass().getName(), ex);
                log.error("Exception class = {}", ex.getClass().getName(), ex);
                throw ex;
//                log.error("Unexpected exception with Mongo bulk writes : {}",ex.getMessage());
            }

        }

//        return snapshots.size();
        return new DataFillRecord(snapshots.size(),nseTimeStamp);
//       mongoTemplate.save(snapshots, instrumentType == InstrumentType.INDEX ? CollectionNames.NSE_INDEX_SNAPSHOT : CollectionNames.NSE_STOCK_SNAPSHOT).size();
    }
    public DataFillRecord fillDataIntoClickhouseEveryMinute(String symbol, Long expiryDate, InstrumentType instrumentType, TimeOfDay timeOfDay) {
        log.info("Starting to Fill data for clickHouse symbol: {} expiry: {}",symbol,expiryDate);

        List<ChainSnapshot> snapshots =
                chainSnapshotServiceImpl.findLatestCapturedSnapshot_soFar(symbol,expiryDate,instrumentType);
//                chainSnapshotServiceImpl.findToday_inserted_snapshots(
//                symbol,
//                expiryDate,
//                instrumentType        );

        snapshots = filterAroundAtm(snapshots,instrumentType == InstrumentType.EQUITY ? 10:15,instrumentType == InstrumentType.EQUITY ? 10:15);

        List<OptionLeg> optionLegs = snapshots.stream()
                .flatMap(snapshot -> Stream.of(
                        snapshot.getCE(),
                        snapshot.getPE()
                ))
                .filter(Objects::nonNull)
                .toList();
        if(optionLegs.isEmpty()) throw new RuntimeException("Empty data after filtering for ATM strikes for symbol: " + symbol + " expiry: " + expiryDate);
        optionLegs.forEach(snapshot ->
                snapshot.setId(
                        snapshotIdGenerator.generate(
                                snapshot.getUnderlying(),
                                snapshot.getExpiryDate(),
                                snapshot.getNseTimestamp(),
                                snapshot.getOptionType().name()
                        )
                ));
        Set<String> existingIds =
                nseSnapshotRepository.findExistingIds(optionLegs.stream().map(OptionLeg::getId).toList());
        if(existingIds.isEmpty()){
            log.info("No existing ids found in clickHouse ID table for symbol: {} expiry: {}. Saving all data.",symbol,expiryDate);
        }
        List<OptionLeg> newSnapshots =
                optionLegs.stream()
                .filter(s -> !existingIds.contains(s.getId()))
                .toList();
        if(newSnapshots.isEmpty()) throw new RuntimeException("Empty options leg after filtering existing ids.");
        nseSnapshotRepository.saveAll(newSnapshots,timeOfDay == TimeOfDay.PREVIOUS_DAY ? "1D":"1m");
        nseSnapshotRepository.insertKeys(List.of(optionLegs.getFirst()));
        log.info("Completed saving data into 1m table for symbol: {} expiry: {}",symbol,expiryDate);
//        return newSnapshots.size();
        return new DataFillRecord(newSnapshots.size(),snapshots.isEmpty() ? null : snapshots.getFirst().getNseTimestamp());
    }

    private List<ChainSnapshot> filterAroundAtm(List<ChainSnapshot> snapshots,
                                                int up,
                                                int down) {

        List<ChainSnapshot> sorted = snapshots.stream()
                .filter(s -> s.getCE() != null)
                .sorted(Comparator.comparingDouble(
                        s -> s.getCE().getStrikePrice()))
                .toList();

        int atmIndex = -1;

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getCE().getOptionRank() == OptionRank.ATM) {
                atmIndex = i;
                break;
            }
        }

        if (atmIndex == -1) {
            log.warn("No ATM strike found");
            throw new RuntimeException("No ATM strike found in the provided snapshots.");
        }

        int from = Math.max(0, atmIndex - down);
        int to = Math.min(sorted.size(), atmIndex + up + 1);

        return sorted.subList(from, to);
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
        query.addCriteria(Criteria.where("year").is(String.valueOf(LocalDate.now(IST).getYear())));
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
        holidayList.setYear(String.valueOf(LocalDate.now(IST).getYear()));
        holidayList.setHolidays(nseHolidays);
        mongoTemplate.save(holidayList, CollectionNames.NSE_TRADING_HOLIDAYS);
        log.info("Saved Holidays to DB");
    }


    public List<ChainSnapshot> findAllSnapshots(
            String underlying,
            LocalDate expiryDate,
            InstrumentType instrumentType) {

        Query query = new Query();

        query.addCriteria(
                Criteria.where("underlying").is(underlying)
        );

        query.addCriteria(
                Criteria.where("expiryDate")
                        .is(expiryDate.format(expiryDateFormatter))
        );

        query.with(Sort.by("nseTimestamp"));

        return mongoTemplate.find(
                query,
                ChainSnapshot.class
        );
    }
    public Optional<ChainSnapshot> findSnapshotAtTime(
            String underlying,
            LocalDate expiryDate,
            LocalDateTime timestamp) {

        Query query = new Query();

        query.addCriteria(
                Criteria.where("underlying").is(underlying)
        );

        query.addCriteria(
                Criteria.where("expiryDate")
                        .is(expiryDate.format(expiryDateFormatter))
        );

        query.addCriteria(
                Criteria.where("nseTimestamp").is(timestamp)
        );

        return mongoTemplate.find(query, ChainSnapshot.class)
                .stream()
                .findFirst();
    }

    public Optional<ChainSnapshot> findFirstSnapshotOfTheDay(
            String underlying,
            LocalDate expiryDate) {

        Query query = new Query();

        query.addCriteria(
                Criteria.where("underlying").is(underlying)
        );

        query.addCriteria(
                Criteria.where("expiryDate")
                        .is(expiryDate.format(expiryDateFormatter))
        );

        query.with(
                Sort.by(Sort.Direction.ASC,
                        "nseTimestamp")
        );

        query.limit(1);

        return mongoTemplate.find(query, ChainSnapshot.class)
                .stream()
                .findFirst();
    }
}
