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
import java.time.LocalDateTime;

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

        ),
        @CompoundIndex(
                name = "uniq_expiry_underlying_timestamp",
                def = "{'expiryDate':1,'underlying':1,'snapshotTimestamp':1}",
                unique = true

        ),
                @CompoundIndex(
                        name = "underlying_expiry_snapshot_idx",
                        def = "{'underlying':1,'expiryDate':1,'snapshotTimestamp':-1}"
                ),
                @CompoundIndex(
                        name = "underlying_expiry_nse_idx",
                        def = "{'underlying':1,'expiryDate':1,'nseTimestamp':-1}"
                )

        })
public class ChainSnapshot {

//    @Id
    private String id;

    private String underlying;

    private Double underlyingValue;

    private String expiryDate;

    private Double strikePrice;

    private LocalDateTime nseTimestamp;

    private OptionLeg CE;

    private OptionLeg PE;

    private Instant snapshotTime;

    private Activity ceActivity;

    private Activity peActivity;


    private Double pcr_oi;

    private Double pcr_volume;
}