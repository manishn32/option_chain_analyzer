package com.nse.analyser.models.jobStatus;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document("job_state")
@Data
@Builder
public class JobState {

    @Id
    private String jobName;

    private LocalDate lastSuccessfulRunDate;

    private Integer totalSymbolsProcessed;

    private Integer totalExpiriesProcessed;

    private Integer failures;

    private LocalDateTime updatedAt;
}