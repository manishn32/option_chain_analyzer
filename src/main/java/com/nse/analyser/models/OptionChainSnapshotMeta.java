package com.nse.analyser.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

//@Document(collection = "Option_chain_snapshot_meta")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionChainSnapshotMeta {

    @Id
    private String id;

    private String underlying;

    private LocalDate expiryDate;

    private Double underlyingValue;

    private Instant snapshotTime;

    private Long totalContracts;

    private Integer atmStrike;
}