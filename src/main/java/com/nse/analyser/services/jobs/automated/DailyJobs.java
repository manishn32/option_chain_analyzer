package com.nse.analyser.services.jobs.automated;

import com.nse.analyser.enums.DataBaseChoice;
import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.enums.TimeOfDay;
import com.nse.analyser.marketData.dto.InstrumentDTO;
import com.nse.analyser.marketData.dto.SymbolsAndExpiryDTO;
import com.nse.analyser.marketData.service.FyersMarketDataClient;
import com.nse.analyser.models.holidayInfo.Holiday;
import com.nse.analyser.services.impl.ModelService;
import com.nse.analyser.services.jobs.NseDataCollectionJob;
import com.nse.analyser.services.jobs.automated.jobAdministration.CollectionExecutorService;
import com.nse.analyser.services.jobs.automated.jobAdministration.CollectionTask;
import com.nse.analyser.services.jobs.automated.jobAdministration.CollectionTaskBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.nse.analyser.utils.DateTimeFormatters.IST;

@Service
@Slf4j
@AllArgsConstructor
public class DailyJobs {
    private final NseDataCollectionJob nseDataCollectionJob;
    private final ModelService modelService;
    private final Map<InstrumentType, Map<String, List<Long>>> mapOfSymbolToExpiry = new ConcurrentHashMap<>();
    private final FyersMarketDataClient fyersMarketDataClient;

    private final CollectionExecutorService collectionExecutorService;
    private final CollectionTaskBuilder taskBuilder;

    private Set<LocalDate> holidayDates;
    @EventListener(ApplicationReadyEvent.class)
    public void startDataCollection(){
        //Get all Symbols and expires
        mapSymbolsAndExpiresForNSE();
        mapTradingHolidays();
    }

    private void mapTradingHolidays() {
        log.info("Mapping NSE holidays");
        List<LocalDate> nseHolidays =
                nseDataCollectionJob.findNseHolidays();
        if(nseHolidays == null || !nseHolidays.isEmpty()){
            log.info("Holidays populated from DB");
            if(nseHolidays !=null) {
                holidayDates = new HashSet<>(nseHolidays);
            }
        }

        if(holidayDates == null || holidayDates.isEmpty()){
            fetchHolidays();
        }
    }

    private void mapSymbolsAndExpiresForNSE() {
        log.info("Starting Data Collection Job and mapping Symbol and indices");

//        Map<InstrumentType, Map<String, List<String>>> nfoExpires = nseDataCollectionJob.findNfoExpires();
//
//        if(!nfoExpires.isEmpty()){
//            mapOfSymbolToExpiry = nfoExpires;
//        }

        if(mapOfSymbolToExpiry.isEmpty()){
            mapIndexAndExpiresFromFyers();
//            mapSymbolsAndExpiresFromFyers();
//            mapOfSymbolToExpiry = modelService.mapIndicesAndExpiry();
//            nseDataCollectionJob.saveSymbolsAndExpires(mapOfSymbolToExpiry);
        }
//        log.debug("Map of Symbol to Expiry: {}", mapOfSymbolToExpiry);
        log.info("Completed mapping Symbols and expires");
    }

    private void mapIndexAndExpiresFromFyers(){
        log.info("Gather Index and expires ");

        List<InstrumentDTO> indexInstrumentsList = fyersMarketDataClient.getFnoInstruments("NSE",true);
        List<String> listOfIndexUnderlying = indexInstrumentsList.stream().map(InstrumentDTO::getUnderlying).toList();
        Map<String, String> reference = collectReferences(indexInstrumentsList);
        List<SymbolsAndExpiryDTO> indexAndExpires = fyersMarketDataClient.getIntrumentAndExpiries("NSE", listOfIndexUnderlying);
                mapOfSymbolToExpiry.put(InstrumentType.INDEX,getMapped(indexAndExpires,reference));
    }

    private Map<String, String> collectReferences(List<InstrumentDTO> listOfIndexUnderlying) {
        Map<String, String> result = new HashMap<>();
        for(InstrumentDTO instrumentDTO : listOfIndexUnderlying){
            result.put(instrumentDTO.getUnderlying(),instrumentDTO.getSymbol());
        }
        return result;
    }

    private void mapSymbolsAndExpiresFromFyers(){
        log.info("Gather symbols and expires ");

        List<InstrumentDTO> indexInstrumentsList = fyersMarketDataClient.getFnoInstruments("NSE",false);
        List<String> listOfIndexUnderlying = indexInstrumentsList.stream().map(InstrumentDTO::getUnderlying).toList();
        List<SymbolsAndExpiryDTO> indexAndExpires = fyersMarketDataClient.getIntrumentAndExpiries("NSE", listOfIndexUnderlying);
        Map<String, String> reference = collectReferences(indexInstrumentsList);
        mapOfSymbolToExpiry.put(InstrumentType.EQUITY,getMapped(indexAndExpires,reference));


    }

    private Map<String, List<Long>> getMapped(List<SymbolsAndExpiryDTO> indexAndExpires, Map<String, String> refernceMap) {
     Map<String,List<Long>> symbolToExpiry = new HashMap<>();
        for (SymbolsAndExpiryDTO each : indexAndExpires){
            symbolToExpiry.put(refernceMap.get(each.getSymbol()),each.getExpires());
     }
        return symbolToExpiry;
    }

//        @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")
    //Trigger this for daily collection of data for NIFTY and other indices. This will run once a day at 9:00 AM IST
//    public void scheduledJob_original_save_to_clickHouse() {
//        if (mapOfSymbolToExpiry == null) {
////            if(dailyJobRan == 1) log.warn("Job has already completed for today {}",LocalDate.now());
//            return;
//        }
//        final LocalDate today = LocalDate.now(IST);
//        LocalDate maxDate = today.plusMonths(3);
//
//        for (Map.Entry<InstrumentType, Map<String, List<String>>> entry : mapOfSymbolToExpiry.entrySet()) {
//
//
//            InstrumentType typeOfInstrument = entry.getKey();
//            for(Map.Entry<String, List<String>> innerMap: entry.getValue().entrySet()){
//                String index = innerMap.getKey();
//                if (index.equalsIgnoreCase("NIFTY")) {
//
//                    List<String> expiryDates = innerMap.getValue();
//
//                    List<String> filteredExpires = expiryDates.stream()
//                            .filter(expiry -> {
//                                try {
//                                    LocalDate expiryDate =
//                                            LocalDate.parse(expiry, DateTimeFormatters.expiryDateFormatter);
//
//                                    return !expiryDate.isBefore(today)
//                                            && !expiryDate.isAfter(maxDate);
//                                } catch (Exception e) {
//                                    log.warn("Actual exception {} with message ",e.getClass().getName(), e);
//                                    log.warn("Unable to parse expiry {}", expiry);
//                                    return false;
//                                }
//                            })
//                            .toList();
//
//                    if (!filteredExpires.isEmpty()) {
//                        CustomCache.cache.put(index, filteredExpires);
//                    }
//
//                    for (String expiryDate : filteredExpires) {
//                        LocalDate parsedExpiryDate = LocalDate.parse(expiryDate, DateTimeFormatters.expiryDateFormatter);
//                        nseDataCollectionJob.fillDataIntoClickhouseEveryMinute(index, parsedExpiryDate, typeOfInstrument);
////                          nseDataCollectionJob.fillData(index, expiryDate, typeOfInstrument);
//                    }
//                }
//            }
//        }
//    }

//    @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")
    public void executeClickHouseDuringDay(){
        LocalTime now = LocalTime.now(IST);
//          Use this to control market timing injection
        if (now.isBefore(LocalTime.of(9, 10)) ||
                now.isAfter(LocalTime.of(15, 35)) || isHoliday()) {
            return;
        }

        scheduledJob_forAllSymbols_duringDay_toClickHouse(TimeOfDay.DURING_DAY);
    }

//    @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")
    public void executeClickHouseAfterDay(){
        scheduledJob_forAllSymbols_duringDay_toClickHouse(TimeOfDay.PREVIOUS_DAY);
    }
    public void scheduledJob_forAllSymbols_duringDay_toClickHouse(TimeOfDay timeOfDayType) {
        LocalDateTime timeOfDay = LocalDateTime.now(IST);
        if(mapOfSymbolToExpiry == null || mapOfSymbolToExpiry.isEmpty()){
            log.info("No symbols and expiry dates mapped yet");
            return;
        }

        List<CollectionTask> tasks =
                taskBuilder.buildTasks(mapOfSymbolToExpiry,timeOfDayType,DataBaseChoice.ClickHouse,timeOfDay
                );

        if(tasks.isEmpty()) {

            log.info(
                    "No pending collection tasks - clickHouse."
            );

            return;
        }

        log.info(
                "Starting {} collection tasks - clickHouse",
                tasks.size()
        );

        collectionExecutorService.execute(
                tasks,
                TimeOfDay.DURING_DAY,
                DataBaseChoice.ClickHouse
        );
    }

    /*
    * Data collection job - runs during market hours and collect data for all NFO symbols and indices
    * */
//    @Schedules({
//            @Scheduled(cron = "0 16 9 * * MON-FRI", zone = "Asia/Kolkata"),
//            @Scheduled(cron = "0 30 9 * * MON-FRI", zone = "Asia/Kolkata"),
//            @Scheduled(cron = "0 30 10 * * MON-FRI", zone = "Asia/Kolkata"),
//            @Scheduled(cron = "0 30 11 * * MON-FRI", zone = "Asia/Kolkata"),
//            @Scheduled(cron = "0 30 12 * * MON-FRI", zone = "Asia/Kolkata"),
//            @Scheduled(cron = "0 30 13 * * MON-FRI", zone = "Asia/Kolkata"),
//            @Scheduled(cron = "0 30 14 * * MON-FRI", zone = "Asia/Kolkata"),
//            @Scheduled(cron = "0 30 15 * * MON-FRI", zone = "Asia/Kolkata"),
//            @Scheduled(cron = "0 45 15 * * MON-FRI", zone = "Asia/Kolkata")
//    })
    public void executeNseDataCollection(TimeOfDay timeOfDay) {
        LocalDateTime session = LocalDateTime.now(IST);
        if (isHoliday() && timeOfDay != TimeOfDay.PREVIOUS_DAY) {
            log.info("Holiday detected. Skipping collection.");
            return;
        }

        if (mapOfSymbolToExpiry == null || mapOfSymbolToExpiry.isEmpty()) {
            log.info("No symbols and expiry dates mapped yet. Skipping collection.");
            return;
        }

        List<CollectionTask> tasks =
                taskBuilder.buildTasks(mapOfSymbolToExpiry,timeOfDay,DataBaseChoice.MongoDB,session);

        if (tasks.isEmpty()) {
            log.info("No pending collection tasks for periodic collection with interval : {}.",timeOfDay.name());
            return;
        }

        log.info("Starting {} collection tasks for periodic collection with interval : {}", tasks.size(),timeOfDay.name());

        collectionExecutorService.execute(tasks,timeOfDay,DataBaseChoice.MongoDB);
    }

    private boolean isHoliday() {
//        if(holidayDates == null|| holidayDates.isEmpty()){
//            fetchHolidays();
//        }
        return holidayDates.contains(
                LocalDate.now(ZoneId.of("Asia/Kolkata"))
        );
    }

    private void fetchHolidays(){
        log.info("Fetching NSE holidays through API");
        List<Holiday> cmHolidays = modelService.fetchAllNseHolidays();
        this.holidayDates = cmHolidays.stream()
                .map(Holiday::getDate)
                .collect(Collectors.toSet());
        nseDataCollectionJob.saveNseHolidays(cmHolidays);
    }

    @Scheduled(cron = "0 11 9 * * MON-FRI", zone = "Asia/Kolkata")
    public void collectPreOpen() {

        log.info("Collecting Pre-Open data for all symbols and indices");
        executeNseDataCollection(TimeOfDay.PRE_OPEN);
    }

//    @Scheduled(cron = "0 50 8 * * MON-FRI", zone = "Asia/Kolkata")
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")
    public void previousDay() {
        log.info("Collecting previous day data for all symbols and indices");
        executeNseDataCollection(TimeOfDay.PREVIOUS_DAY);
    }

//    @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")
//    public void duringDayMongoDBSave() {
//        log.info("Collecting data for all symbols and indices during market hours");
//        executeNseDataCollection(TimeOfDay.DURING_DAY);
//    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")
    public void duringDayMongoDBSave() {

        LocalTime now = LocalTime.now(IST);
//          Use this to control market timing injection
        if (now.isBefore(LocalTime.of(9, 10)) ||
                    now.isAfter(LocalTime.of(15, 35)) || isHoliday()) {
            return;
        }
        log.info("Collecting data for all symbols and indices during market hours, time {}",now);
        executeNseDataCollection(TimeOfDay.DURING_DAY);
    }

    @Scheduled(cron = "0 16 9 * * MON-FRI", zone = "Asia/Kolkata")
    public void collect0916() {

        log.info("Collecting 9 16 data for all symbols and indices");
        executeNseDataCollection(TimeOfDay.T_916_AM);
    }

    @Scheduled(cron = "0 30 9 * * MON-FRI", zone = "Asia/Kolkata")
    public void collect0930() {

        log.info("Collecting 9 30 data for all symbols and indices");
        executeNseDataCollection(TimeOfDay.T_930_AM);
    }

    @Scheduled(cron = "0 30 10 * * MON-FRI", zone = "Asia/Kolkata")
    public void collect1030() {

        log.info("Collecting 1030 data for all symbols and indices");
        executeNseDataCollection(TimeOfDay.T_1030_AM);
    }

    @Scheduled(cron = "0 30 11 * * MON-FRI", zone = "Asia/Kolkata")
    public void collect1130() {
        executeNseDataCollection(TimeOfDay.T_1130_AM);
    }

    @Scheduled(cron = "0 30 12 * * MON-FRI", zone = "Asia/Kolkata")
    public void collect1230() {
        executeNseDataCollection(TimeOfDay.T_1230_PM);
    }

    @Scheduled(cron = "0 30 13 * * MON-FRI", zone = "Asia/Kolkata")
    public void collect1330() {
        executeNseDataCollection(TimeOfDay.T_130_PM);
    }

    @Scheduled(cron = "0 30 14 * * MON-FRI", zone = "Asia/Kolkata")
    public void collect1430() {
        executeNseDataCollection(TimeOfDay.T_230_PM);
    }

    @Scheduled(cron = "0 30 15 * * MON-FRI", zone = "Asia/Kolkata")
    public void collect1530() {
        executeNseDataCollection(TimeOfDay.T_330_PM);
    }

    @Scheduled(cron = "0 45 15 * * MON-FRI", zone = "Asia/Kolkata")
    public void collect1545() {
        executeNseDataCollection(TimeOfDay.T_345_PM);
    }
}
