package com.nse.analyser.services.schedulers;

import com.nse.analyser.dtos.nseResponse.HistoricalVolatility;
import com.nse.analyser.rabbitMQ.TelegramConfiguration;
import com.nse.analyser.rabbitMQ.dtos.Communications;
import com.nse.analyser.rabbitMQ.producer.TelegramPublisher;
import com.nse.analyser.utils.NumberUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalVolatilityReader {

    @Value("${historical.volatility.file}")
    private String FILE_PATH ;
//            "C:\\Users\\manis\\Downloads\\FOVOLT_22102026csv";

    private final TelegramPublisher telegramPublisher;

    private final TelegramConfiguration telegramConfiguration;

    public static final Map<String, HistoricalVolatility> mapOfVolatility = new HashMap<>();
    @Order(3)
    @EventListener(ApplicationReadyEvent.class)
    public void initMap(){
        try {
            read();
        } catch (Exception e) {
            Communications communications = new Communications();
            communications.setChatId(telegramConfiguration.getErrors());
            communications.setMessage("Error in using Historical volatility file path: %s \n Error %s".formatted(FILE_PATH,e));
            telegramPublisher.publish(communications);
        }
    }
    public void read(
//            Path file
    )
            throws Exception {

        Path file = Path.of(FILE_PATH);

        try (
                Reader reader = Files.newBufferedReader(file);

                CSVParser parser = CSVFormat.DEFAULT
                        .builder()
                        .setIgnoreSurroundingSpaces(true)
                        .setSkipHeaderRecord(true).get()
                        .parse(reader)
        ) {

            boolean headerSkipped = false;

            for (CSVRecord record : parser) {

                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                HistoricalVolatility hv =
                        HistoricalVolatility.builder()
                                .date(record.get(0).trim())
                                .symbol(record.get(1).trim())
                                .underlyingAnnualisedVolatility(NumberUtils.roundTo2Decimals(parse(record.get(7).trim())*100))
                                .futuresAnnualisedVolatility(NumberUtils.roundTo2Decimals(parse(record.get(13))*100))
                                .applicableDailyVolatility(NumberUtils.roundTo2Decimals(parse(record.get(14))*100))
                                .applicableAnnualisedVolatility(NumberUtils.roundTo2Decimals(parse(record.get(15))*100))
                                .build();

                mapOfVolatility.put(hv.getSymbol(), hv);
            }
        }catch (Exception exception){
            log.error("Error in using Historical volatility file path: {}",FILE_PATH,exception);
            throw exception;
        }
//        return result;
        log.info("Successfully mapped volatility data for all symbols {}",mapOfVolatility.size());
    }

    private double parse(String value) {
        return Double.parseDouble(value.trim());
    }
}