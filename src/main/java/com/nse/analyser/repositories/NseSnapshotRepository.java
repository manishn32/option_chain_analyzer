package com.nse.analyser.repositories;

import com.nse.analyser.models.chainSnapShot.OptionLeg;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface NseSnapshotRepository {

    void save(OptionLeg snapshot);

    void saveAll(List<OptionLeg> snapshots);

    List<OptionLeg> findLatest(
            String underlying,
            LocalDate expiryDate,
            int limit);

    List<OptionLeg> findByStrike(
            String underlying,
            LocalDate expiryDate,
            Double strikePrice,
            String optionType);

    void deleteOlderThan(LocalDateTime timestamp);
}