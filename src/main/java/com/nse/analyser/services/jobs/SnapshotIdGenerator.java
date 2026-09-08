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
            LocalDateTime nseTimestamp) {

        String source =
                underlying + "|" +
                        expiryDate + "|" +
                        nseTimestamp;

        return DigestUtils.sha256Hex(source);
    }
}