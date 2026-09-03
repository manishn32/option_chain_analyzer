package com.nse.analyser.services.jobs;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class SnapshotIdGenerator {

    public String generate(
            String underlying,
            LocalDate expiryDate,
//            double strikePrice,
//            String optionType,
            LocalDateTime nseTimestamp) {

        String source =
                underlying + "|" +
                        expiryDate + "|" +
//                        strikePrice + "|" +
//                        optionType + "|" +
                        nseTimestamp;

        return DigestUtils.sha256Hex(source);
    }
}