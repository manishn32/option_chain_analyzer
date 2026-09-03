package com.nse.analyser.repositories;

import com.nse.analyser.models.chainSnapShot.OptionLeg;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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

    void insertKeys(List<OptionLeg> snapshots);

    Set<String> findExistingIds(Collection<String> ids);
}