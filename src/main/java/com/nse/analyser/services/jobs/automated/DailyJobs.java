package com.nse.analyser.services.jobs.automated;

import com.nse.analyser.models.underlyingInfos.TradableFnO_Contracts;
import com.nse.analyser.services.impl.ModelService;
import com.nse.analyser.services.jobs.NseDataCollectionJob;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class DailyJobs {
    private final NseDataCollectionJob nseDataCollectionJob;
    private final ModelService modelService;
    private Map<String, List<String>> mapOfSymbolToExpiry;
    @EventListener(ApplicationReadyEvent.class)
    public void startDataCollection(){
        //Get all Symbols
        Map<String, List<String>> stringListMap = modelService.mapIndicesAndExpiry();
        mapOfSymbolToExpiry = stringListMap;
        for(Map.Entry<String, List<String>> entry : stringListMap.entrySet()) {
            String index = entry.getKey();
            if (index.equalsIgnoreCase("NIFTY")){
                List<String> expiryDates = entry.getValue();
                for (String expiryDate : expiryDates) {
                    nseDataCollectionJob.fillDataForNifty(index, expiryDate);
                }
            }
        }
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")
    public void scheduledJob(){
        if(mapOfSymbolToExpiry == null) return;
        for(Map.Entry<String, List<String>> entry : mapOfSymbolToExpiry.entrySet()) {
            String index = entry.getKey();
            if (index.equalsIgnoreCase("NIFTY")){
                List<String> expiryDates = entry.getValue();
                for (String expiryDate : expiryDates) {
                    nseDataCollectionJob.fillDataForNifty(index, expiryDate);
                }
            }
        }
    }
}
