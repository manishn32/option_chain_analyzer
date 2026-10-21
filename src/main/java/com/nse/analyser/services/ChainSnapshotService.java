package com.nse.analyser.services;

import com.nse.analyser.models.ChainSnapshot;
import com.nse.analyser.models.chainSnapShot.StrikeSnapshot;
import com.nse.analyser.repositories.ChainSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChainSnapshotService {

    private final ChainSnapshotRepository repository;

    public ChainSnapshot getLatestSnapshot(
            String underlying,
            String expiryDate) {

        return repository
                .findTopByUnderlyingAndExpiryDateOrderBySnapshotTimeDesc(
                        underlying,
                        expiryDate
                )
                .orElse(null);
    }

    public StrikeSnapshot getStrike(
            ChainSnapshot snapshot,
            double strikePrice) {

        return snapshot.getStrikes()
                .stream()
                .filter(
                        strike ->
                                strike.getStrikePrice()
                                        .equals(strikePrice)
                )
                .findFirst()
                .orElse(null);
    }

    public StrikeSnapshot getATMStrike(
            ChainSnapshot snapshot) {

        if (snapshot.getUnderlyingValue() == null) {
            return null;
        }

        return snapshot.getStrikes()
                .stream()
                .min(
                        Comparator.comparingDouble(
                                strike ->
                                        Math.abs(
                                                strike.getStrikePrice()
                                                        - snapshot.getUnderlyingValue()
                                        )
                        )
                )
                .orElse(null);
    }

    public StrikeSnapshot getMaxCallOiStrike(
            ChainSnapshot snapshot) {

        return snapshot.getStrikes()
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

    public StrikeSnapshot getMaxPutOiStrike(
            ChainSnapshot snapshot) {

        return snapshot.getStrikes()
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

    public Double getSupport(
            ChainSnapshot snapshot) {

        StrikeSnapshot support =
                getMaxPutOiStrike(snapshot);

        return support == null
                ? null
                : support.getStrikePrice();
    }

    public Double getResistance(
            ChainSnapshot snapshot) {

        StrikeSnapshot resistance =
                getMaxCallOiStrike(snapshot);

        return resistance == null
                ? null
                : resistance.getStrikePrice();
    }

    public List<ChainSnapshot> getHistoricalSnapshots(
            String underlying,
            String expiryDate,
            Instant from,
            Instant to) {

        return repository
                .findByUnderlyingAndExpiryDateAndSnapshotTimeBetweenOrderBySnapshotTimeAsc(
                        underlying,
                        expiryDate,
                        from,
                        to
                );
    }
}
