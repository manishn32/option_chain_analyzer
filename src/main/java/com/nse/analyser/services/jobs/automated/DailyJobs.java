package com.nse.analyser.services.jobs.automated;

import com.nse.analyser.CustomCache;
import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.enums.TimeOfDay;
import com.nse.analyser.models.holidayInfo.Holiday;
import com.nse.analyser.services.impl.ModelService;
import com.nse.analyser.services.jobs.NseDataCollectionJob;
import com.nse.analyser.services.jobs.automated.jobAdministration.CollectionExecutorService;
import com.nse.analyser.services.jobs.automated.jobAdministration.CollectionTask;
import com.nse.analyser.services.jobs.automated.jobAdministration.CollectionTaskBuilder;
import com.nse.analyser.utils.DateTimeFormatters;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class DailyJobs {
    private final NseDataCollectionJob nseDataCollectionJob;
    private final ModelService modelService;
    private Map<InstrumentType, Map<String, List<String>>> mapOfSymbolToExpiry;

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
        List<LocalDate> nseHolidays = nseDataCollectionJob.findNseHolidays();
        if(!nseHolidays.isEmpty()){
            log.info("Holidays populated from DB");
            holidayDates = new HashSet<>(nseHolidays);
        }

        if(holidayDates == null || holidayDates.isEmpty()){
            fetchHolidays();
        }
    }

    private void mapSymbolsAndExpiresForNSE() {
        log.info("Starting Data Collection Job and mapping Symbol and indices");

        Map<InstrumentType, Map<String, List<String>>> nfoExpires = nseDataCollectionJob.findNfoExpires();

        if(!nfoExpires.isEmpty()){
            mapOfSymbolToExpiry = nfoExpires;
        }

        if(mapOfSymbolToExpiry == null || mapOfSymbolToExpiry.isEmpty()){
            mapOfSymbolToExpiry = modelService.mapIndicesAndExpiry();
            nseDataCollectionJob.saveSymbolsAndExpires(mapOfSymbolToExpiry);
        }
        log.debug("Map of Symbol to Expiry: {}", mapOfSymbolToExpiry);
        log.info("Completed mapping Symbols and expires");
    }

    //    @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")
    //Trigger this for daily collection of 1m data into Mongo
    public void scheduledJob() {
        if (mapOfSymbolToExpiry == null) {
//            if(dailyJobRan == 1) log.warn("Job has already completed for today {}",LocalDate.now());
            return;
        }
        final LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusMonths(3);

        for (Map.Entry<InstrumentType, Map<String, List<String>>> entry : mapOfSymbolToExpiry.entrySet()) {


            InstrumentType typeOfInstrument = entry.getKey();
            for(Map.Entry<String, List<String>> innerMap: entry.getValue().entrySet()){
                String index = innerMap.getKey();
                if (index.equalsIgnoreCase("NIFTY")) {

                    List<String> expiryDates = innerMap.getValue();

                    List<String> filteredExpires = expiryDates.stream()
                            .filter(expiry -> {
                                try {
                                    LocalDate expiryDate =
                                            LocalDate.parse(expiry, DateTimeFormatters.expiryDateFormatter);

                                    return !expiryDate.isBefore(today)
                                            && !expiryDate.isAfter(maxDate);
                                } catch (Exception e) {
                                    log.warn("Actual exception {} with message ",e.getClass().getName(), e);
                                    log.warn("Unable to parse expiry {}", expiry);
                                    return false;
                                }
                            })
                            .toList();

                    if (!filteredExpires.isEmpty()) {
                        CustomCache.cache.put(index, filteredExpires);
                    }

                    for (String expiryDate : filteredExpires) {
//                        nseDataCollectionJob.fillDataForNifty(index, expiryDate, DataCategory.nse_snapshot,typeOfInstrument);
                          nseDataCollectionJob.fillData(index, expiryDate, typeOfInstrument);
                    }
                }
            }
        }
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")
    public void scheduledJob_forAllSymbols_original() {

        if(mapOfSymbolToExpiry == null){
            return;
        }

        List<CollectionTask> tasks =
                taskBuilder.buildTasks(
                        mapOfSymbolToExpiry,
                        TimeOfDay.PREVIOUS_DAY
                );

        if(tasks.isEmpty()) {

            log.info(
                    "No pending collection tasks."
            );

            return;
        }

        log.info(
                "Starting {} collection tasks",
                tasks.size()
        );

        collectionExecutorService.execute(
                tasks,
                TimeOfDay.PREVIOUS_DAY
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
    public void executeNseDataCollection_hourlyData(TimeOfDay timeOfDay) {

        if (isHoliday()) {
            log.info("Holiday detected. Skipping collection.");
            return;
        }

        if (mapOfSymbolToExpiry == null) {
            return;
        }

        List<CollectionTask> tasks =
                taskBuilder.buildTasks(mapOfSymbolToExpiry,timeOfDay);

        if (tasks.isEmpty()) {
            log.info("No pending collection tasks for periodic collection.");
            return;
        }

        log.info("Starting {} collection tasks for periodic collection", tasks.size());

        collectionExecutorService.execute(tasks,timeOfDay);
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
        executeNseDataCollection_hourlyData(TimeOfDay.PRE_OPEN);
    }

    @Scheduled(cron = "0 50 8 * * MON-FRI", zone = "Asia/Kolkata")
    public void previousDay() {
        executeNseDataCollection_hourlyData(TimeOfDay.PREVIOUS_DAY);
    }
    @Scheduled(cron = "0 16 9 * * MON-FRI", zone = "Asia/Kolkata")
    public void collect0916() {
        executeNseDataCollection_hourlyData(TimeOfDay.T_916_AM);
    }

    @Scheduled(cron = "0 30 9 * * MON-FRI", zone = "Asia/Kolkata")
    public void collect0930() {
        executeNseDataCollection_hourlyData(TimeOfDay.T_930_AM);
    }

    @Scheduled(cron = "0 30 10 * * MON-FRI", zone = "Asia/Kolkata")
    public void collect1030() {
        executeNseDataCollection_hourlyData(TimeOfDay.T_1030_AM);
    }

    @Scheduled(cron = "0 30 11 * * MON-FRI", zone = "Asia/Kolkata")
    public void collect1130() {
        executeNseDataCollection_hourlyData(TimeOfDay.T_1130_AM);
    }

    @Scheduled(cron = "0 30 12 * * MON-FRI", zone = "Asia/Kolkata")
    public void collect1230() {
        executeNseDataCollection_hourlyData(TimeOfDay.T_1230_PM);
    }

    @Scheduled(cron = "0 30 13 * * MON-FRI", zone = "Asia/Kolkata")
    public void collect1330() {
        executeNseDataCollection_hourlyData(TimeOfDay.T_130_PM);
    }

    @Scheduled(cron = "0 30 14 * * MON-FRI", zone = "Asia/Kolkata")
    public void collect1430() {
        executeNseDataCollection_hourlyData(TimeOfDay.T_230_PM);
    }

    @Scheduled(cron = "0 30 15 * * MON-FRI", zone = "Asia/Kolkata")
    public void collect1530() {
        executeNseDataCollection_hourlyData(TimeOfDay.T_330_PM);
    }

    @Scheduled(cron = "0 45 15 * * MON-FRI", zone = "Asia/Kolkata")
    public void collect1545() {
        executeNseDataCollection_hourlyData(TimeOfDay.T_345_PM);
    }
}
