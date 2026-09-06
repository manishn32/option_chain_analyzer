package com.nse.analyser.models;

import com.nse.analyser.dtos.nseResponse.Activity;
import com.nse.analyser.models.chainSnapShot.OptionLeg;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document
//        (collection = "Option_chain_snapshot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(
        name = "uniq_expiry_underlying_timestamp",
def = "{'expiryDate':1,'underlying':1,'nseTimeStamp':1}",
unique = true

        )

        })
public class ChainSnapshot {

//    @Id
    private String id;

    private String underlying;

    private Double underlyingValue;

    private String expiryDate;

    private Double strikePrice;

    private String nseTimestamp;

    private OptionLeg CE;

    private OptionLeg PE;

    private Instant snapshotTime;

    private Activity ceActivity;

    private Activity peActivity;


    private Double pcr_oi;

    private Double pcr_volume;
}