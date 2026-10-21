package com.nse.analyser.repositories;

import com.nse.analyser.models.ChainSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChainSnapshotRepository
        extends MongoRepository<ChainSnapshot, String> {

    Optional<ChainSnapshot> findTopByUnderlyingAndExpiryDateOrderBySnapshotTimeDesc(
            String underlying,
            String expiryDate
    );

    List<ChainSnapshot> findByUnderlyingAndExpiryDateOrderBySnapshotTimeDesc(
            String underlying,
            String expiryDate
    );

    List<ChainSnapshot> findByUnderlyingAndExpiryDateAndSnapshotTimeBetweenOrderBySnapshotTimeAsc(
            String underlying,
            String expiryDate,
            Instant from,
            Instant to
    );

    Optional<ChainSnapshot> findTopByUnderlyingOrderBySnapshotTimeDesc(
            String underlying
    );

    Optional<ChainSnapshot> findTopByUnderlyingAndExpiryDateAndSnapshotTimeLessThanEqualOrderBySnapshotTimeDesc(
            String underlying,
            String expiryDate,
            Instant snapshotTime
    );
}