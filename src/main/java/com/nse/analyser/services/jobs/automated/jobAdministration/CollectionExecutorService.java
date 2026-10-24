package com.nse.analyser.services.jobs.automated.jobAdministration;

import com.nse.analyser.dtos.nseResponse.DataFillRecord;
import com.nse.analyser.enums.DataBaseChoice;
import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.enums.TimeOfDay;
import com.nse.analyser.models.ChainSnapshot;
import com.nse.analyser.rabbitMQ.TelegramConfiguration;
import com.nse.analyser.rabbitMQ.dtos.Communications;
import com.nse.analyser.rabbitMQ.producer.TelegramPublisher;
import com.nse.analyser.services.impl.ChainSnapshotServiceImpl;
import com.nse.analyser.services.jobs.NseDataCollectionJob;
import com.nse.analyser.utils.DateTimeFormatters;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static com.nse.analyser.utils.DateTimeFormatters.IST;

@Service
//@RequiredArgsConstructor
@Slf4j
public class CollectionExecutorService {


    private final ThreadPoolExecutor mongoExecutor;
    private final ThreadPoolExecutor clickHouseExecutor;
    private final CollectionStateService stateService;
    private final NseDataCollectionJob job;
    private final TelegramPublisher telegramPublisher;
    private final TelegramConfiguration telegramConfiguration;
    private final ChainSnapshotServiceImpl chainSnapshotService;

    public CollectionExecutorService(@Qualifier("mongoCollectionExecutor")ThreadPoolExecutor mongoExecutor, @Qualifier("clickHouseCollectionExecutor") ThreadPoolExecutor clickHouseExecutor, CollectionStateService stateService, NseDataCollectionJob job, TelegramPublisher telegramPublisher, TelegramConfiguration telegramConfiguration, ChainSnapshotServiceImpl chainSnapshotService) {
        this.mongoExecutor = mongoExecutor;
        this.clickHouseExecutor = clickHouseExecutor;
        this.stateService = stateService;
        this.job = job;
        this.telegramPublisher = telegramPublisher;
        this.telegramConfiguration = telegramConfiguration;
        this.chainSnapshotService = chainSnapshotService;
    }

//    public void execute_advanced(
//            List<CollectionTask> tasks,
//            TimeOfDay timeOfDay,
//            DataBaseChoice databaseChoice
//    ) {
//
//        ExecutorService executor =
//                databaseChoice == DataBaseChoice.MongoDB
//                        ? mongoExecutor
//                        : clickHouseExecutor;
//
//        tasks.forEach(task ->
//                CompletableFuture.runAsync(
//                        () -> process(
//                                task,
//                                timeOfDay,
//                                databaseChoice
//                        ),
//                        executor
//                ).exceptionally(ex->{
//                    log.error("Unexpected async error", ex);
//                    return null;
//                })
//        );
//    }
    public void execute(
            List<CollectionTask> tasks,
            TimeOfDay timeOfDay,
            DataBaseChoice databaseChoice
    ) {

        ExecutorService executor =
                databaseChoice == DataBaseChoice.MongoDB
                        ? mongoExecutor
                        : clickHouseExecutor;

        log.info(
                "Submitting {} tasks to {} executor",
                tasks.size(),
                databaseChoice
        );

//        tasks.forEach(task ->
//                CompletableFuture.runAsync(
//                        () -> process(task, timeOfDay, databaseChoice),
//                        executor
//                ).exceptionally(ex -> {
//                    log.error("Unexpected async error", ex);
//                    return null;
//                })
//        );

        tasks.forEach(task ->
                executor.submit(
                        () -> process(
                                task,
                                timeOfDay,
                                databaseChoice
                        )
                )
        );

    }
//
//    public void execute_pakka_working(
//            List<CollectionTask> tasks,
//            TimeOfDay timeOfDay,
//            DataBaseChoice dataBaseChoice
//    ) {
//
//        ExecutorService executor =
//                dataBaseChoice == DataBaseChoice.MongoDB
//                        ? mongoExecutor
//                        : clickHouseExecutor;
//
//        List<CompletableFuture<Void>> futures =
//                tasks.stream()
//                        .map(task ->
//                                CompletableFuture.runAsync(
//                                        () -> process(task,timeOfDay,dataBaseChoice),
//                                        executor
//                                )
//                        )
//                        .toList();
//
//        CompletableFuture.allOf(
//                futures.toArray(
//                        new CompletableFuture[0]
//                )
//        ).join();
//    }

    private void process(
            CollectionTask task,
            TimeOfDay timeOfDay,
            DataBaseChoice dataBaseChoice
    ) {

        try {
            MDC.put("symbol", task.getSymbol());
            MDC.put("db", dataBaseChoice.name());
            stateService.markStarted(
                    task.getSymbol(),
                    task.getExpiryDate(),
                    task.getInstrumentType(),
                    timeOfDay,
                    dataBaseChoice,
                    task.getIntradayKey()
            );


            DataFillRecord records = dataBaseChoice == DataBaseChoice.MongoDB ?
                    job.fillData(task.getSymbol(), task.getExpiryDate(), task.getInstrumentType(),task.getIntradayKey(),timeOfDay) :
                    null;
//                    job.fillDataIntoClickhouseEveryMinute(task.getSymbol(), task.getExpiryDate(), task.getInstrumentType(),timeOfDay);

            stateService.markSuccess(
                    task.getSymbol(),
                    task.getExpiryDate(),
                    records,
                    task.getTimeOfDay(),
                    dataBaseChoice,
                    task.getIntradayKey(),
                    task.getInstrumentType()
            );
            sendNotificationsOfOptionChain(task);
        } catch(Exception exception){

//            log.error(
//                    "Failed Symbol: {} expiring {} with error {} run time flag {} to {}",
//                    task.getSymbol(),
//                    task.getExpiryDate(),
//                    exception,
//                    timeOfDay,
//                    dataBaseChoice
//            );
            log.error(
                    "Failed Symbol {} Expiry {} TimeOfDay {} Database {}",
                    task.getSymbol(),
                    task.getExpiryDate(),
                    timeOfDay,
                    dataBaseChoice,
                    exception
            );

            stateService.markFailure(
                    task.getSymbol(),
                    task.getExpiryDate(),
                    exception,
                    task.getTimeOfDay(),
                    dataBaseChoice,
                    task.getIntradayKey(),
                    task.getInstrumentType()
            );
        }finally {

            MDC.clear();
        }
    }

    public void sendNotificationsOfOptionChain(CollectionTask collectionTask) {

        LocalTime now = LocalTime.now(IST);
//          Use this to control market timing injection
//        if (now.isBefore(LocalTime.of(9, 15)) ||
//                now.isAfter(LocalTime.of(15, 35)) || isHoliday()) {
//            return;
//        }
        log.info("Sending notifications for indices during market hours, time {}",now);
        List<String> indexOptionsData = getIndexOptionsData(collectionTask.getSymbol(),collectionTask.getExpiryDate());
        for(String messages :indexOptionsData ){
            Communications communications = new Communications();
            communications.setChatId(telegramConfiguration.getInfo());
            communications.setMessage(messages);
            telegramPublisher.publish(communications);
        }
    }

    private List<String> getIndexOptionsData(String symbol, Long expiry) {
        List<String> underlyingToQuery = List.of("NSE:NIFTY50-INDEX", "NSE:MIDCPNIFTY-INDEX", "NSE:NIFTYBANK-INDEX");
        if(!underlyingToQuery.contains(symbol.trim().toUpperCase())){
            return Collections.emptyList();
        }

        List<String> messages = new ArrayList<>();

        ChainSnapshot chainSnapshot = chainSnapshotService.findAllSnapshots(symbol, InstrumentType.INDEX, Instant.ofEpochSecond(expiry).atZone(IST).format(DateTimeFormatters.expiryForSnapShot));

//        for(ChainSnapshot chainSnapshot : allSnapshots){
            String s = """
                    Index : %s
                    price : %s
                    premium : %s
                    pcr : %s
                    pcr_oi : %s
                    pcr_volume : %s
                    pcr_oi_change : %s
                    premium_bias : %s
                    Intraday_resistance : %s
                    Intraday_support : %s
                    Strongest_resistance : %s
                    Strongest_support : %s
                    Latest_time_stamp : %s
                    """.formatted(
                    chainSnapshot.getUnderlying(),
                    chainSnapshot.getUnderlyingValue(),
                    chainSnapshot.getPremium(),
                    chainSnapshot.getPcr(),
                    chainSnapshot.getPcr_oi(),
                    chainSnapshot.getPcr_volume(),
                    chainSnapshot.getPcr_oi_change(),
                    chainSnapshot.getPremiumBias(),
                    chainSnapshot.getIntradayResistance(),
                    chainSnapshot.getIntradaySupport(),
                    chainSnapshot.getResistance(),
                    chainSnapshot.getSupport(),
                    chainSnapshot.getSnapshotTime().atZone(IST).toLocalDateTime().toString()
            );
            messages.add(s);
//        }
        return messages;
    }
}