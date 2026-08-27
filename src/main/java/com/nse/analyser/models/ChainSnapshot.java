package com.nse.analyser.models;

import com.nse.analyser.models.chainSnapShot.OptionLeg;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.Instant;

//@Document(collection = "Option_chain_snapshot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainSnapshot {

//    @Id
//    private String id;

    private String underlying;

    private Double underlyingValue;

    private String expiryDate;

    private Double strikePrice;

    private String nseTimestamp;

    private OptionLeg CE;

    private OptionLeg PE;

    private Instant snapshotTime;

    private Double pcr_oi;

    private Double pcr_volume;
}