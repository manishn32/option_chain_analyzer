package com.nse.analyser.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nse.analyser.enums.OptionRank;
import com.nse.analyser.enums.OptionType;
import com.nse.analyser.enums.PremiumBias;
import com.nse.analyser.marketData.dto.fyers.IndexDataDTO;
import com.nse.analyser.models.chainSnapShot.StrikeSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(
                name = "uniq_expiry_underlying_timestamp",
                def = "{'expiryDate':1,'underlying':1,'snapshotTime':1}",
                unique = true
        ),
        @CompoundIndex(
                name = "underlying_expiry_snapshot_idx",
                def = "{'underlying':1,'expiryDate':1,'snapshotTime':-1}"
        ),
        @CompoundIndex(
                name = "underlying_expiry_nse_idx",
                def = "{'underlying':1,'expiryDate':1,'nseTimestamp':-1}"
        )
})
public class ChainSnapshot {

    @Id
    private String identification;

    private String symbol;

    private String underlying;

    private Double underlyingValue;

    private Double premium;

    private Double underlyingChange;

    private Double underlyingChangeP;

    private Double futuresPrice;

    private Double futuresChange;

    private Double futuresChangeP;

    private String expiryDate;

    private LocalDateTime nseTimestamp;

    private Instant snapshotTime;

    private Double pcr_oi;

    private Double pcr_volume;

    private PremiumBias premiumBias;

    private IndexDataDTO indiavixData;

    private List<StrikeSnapshot> strikes = new ArrayList<>();

    @JsonIgnore
    public StrikeSnapshot getATMStrike() {

        if (this.getUnderlyingValue() == null) {
            return null;
        }

        return this.getStrikes()
                .stream()
                .min(
                        Comparator.comparingDouble(
                                strike ->
                                        Math.abs(
                                                strike.getStrikePrice()
                                                        - this.getUnderlyingValue()
                                        )
                        )
                )
                .orElse(null);
    }

    @JsonIgnore
    public StrikeSnapshot getMaxCallOiStrike() {

        return this.getStrikes()
                .stream()
                .filter(
                        strike ->
                                strike.getCe() != null
                )
                .max(
                        Comparator.comparingLong(
                                strike ->
                                        strike.getCe().getOpenInterest()
                        )
                )
                .orElse(null);
    }

    @JsonIgnore
    public StrikeSnapshot getMaxPutOiStrike(
            ) {

        return this.getStrikes()
                .stream()
                .filter(
                        strike ->
                                strike.getPe() != null
                )
                .max(
                        Comparator.comparingLong(
                                strike ->
                                        strike.getPe().getOpenInterest()
                        )
                )
                .orElse(null);
    }

    @JsonIgnore
    public Double getSupport() {

        StrikeSnapshot support =
                getMaxPutOiStrike();

        return support == null
                ? null
                : support.getStrikePrice();
    }

    @JsonIgnore
    public Double getResistance() {

        StrikeSnapshot resistance =
                getMaxCallOiStrike();

        return resistance == null
                ? null
                : resistance.getStrikePrice();
    }

    @JsonIgnore
    public PremiumBias getPremiumBias() {

        StrikeSnapshot atm = getATMStrike();

        if (atm == null) {
            return PremiumBias.NEUTRAL;
        }

        double atmStrike = atm.getStrikePrice();

        double totalCallPremium =
                getStrikes()
                        .stream()
                        .filter(s -> s.getStrikePrice() > atmStrike)
                        .filter(s -> s.getCe() != null)
                        .mapToDouble(s -> {
                            Double ltp = s.getCe().getLTP();
                            return ltp == null ? 0.0 : ltp;
                        })
                        .sum();

        double totalPutPremium =
                getStrikes()
                        .stream()
                        .filter(s -> s.getStrikePrice() < atmStrike)
                        .filter(s -> s.getPe() != null)
                        .mapToDouble(s -> {
                            Double ltp = s.getPe().getLTP();
                            return ltp == null ? 0.0 : ltp;
                        })
                        .sum();

        if (totalCallPremium > totalPutPremium) {
            return PremiumBias.CALL_HEAVY;
        }

        if (totalPutPremium > totalCallPremium) {
            return PremiumBias.PUT_HEAVY;
        }

        return PremiumBias.NEUTRAL;
    }
}