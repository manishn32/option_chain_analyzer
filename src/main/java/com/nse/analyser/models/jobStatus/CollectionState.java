package com.nse.analyser.models.jobStatus;

import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.enums.TimeOfDay;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tools.jackson.databind.type.CollectionType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document("collection_state")
@Data
@Builder
public class CollectionState {

    @Id
    private String id;

    private String symbol;

    private String expiryDate;

    private InstrumentType instrumentType;

    private CollectionType collectionType;

    private LocalDate businessDate;

    private CollectionStatus status;

    private Integer attemptCount;

    private Long recordsCollected;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime lastAttemptAt;

    private String errorMessage;

    private TimeOfDay timeOfDay;
}
