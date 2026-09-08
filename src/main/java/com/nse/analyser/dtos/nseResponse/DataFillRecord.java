package com.nse.analyser.dtos.nseResponse;

import java.time.LocalDateTime;

public record DataFillRecord (int totalRecords, LocalDateTime nseTimestamp) {
}
