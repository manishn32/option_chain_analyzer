package com.nse.analyser.services.impl;

import com.nse.analyser.constants.CollectionNames;
import com.nse.analyser.constants.SnapshotCollectionQueryingConstants;
import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.models.ChainSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.nse.analyser.utils.DateTimeFormatters.IST;
import static com.nse.analyser.utils.DateTimeFormatters.expiryDateFormatter;

@Service
@RequiredArgsConstructor
public class ChainSnapshotServiceImpl {

    private final MongoTemplate mongoTemplate;

    private String getCollectionName(InstrumentType instrumentType) {
        return instrumentType == InstrumentType.EQUITY?
                CollectionNames.NSE_STOCK_SNAPSHOT : CollectionNames.NSE_INDEX_SNAPSHOT;

    }

    public List<ChainSnapshot> findAllSnapshots(
            String underlying,
            LocalDate expiryDate,
            InstrumentType instrumentType) {

        Query query = new Query();

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.underlying.name()).is(underlying)
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.expiryDate.name())
                        .is(expiryDate.format(expiryDateFormatter))
        );

        query.with(Sort.by(SnapshotCollectionQueryingConstants.nseTimestamp.name()));

        return mongoTemplate.find(
                query,
                ChainSnapshot.class,
                getCollectionName(instrumentType)
        );
    }
    public Optional<ChainSnapshot> find_snapshot_for_given_time(
            String underlying,
            LocalDate expiryDate,
            LocalDateTime timestamp,
            InstrumentType instrumentType) {

        Query query = new Query();

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.underlying.name()).is(underlying)
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.expiryDate.name())
                        .is(expiryDate.format(expiryDateFormatter))
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.nseTimestamp.name()).is(timestamp)
        );

        return mongoTemplate.find(query, ChainSnapshot.class,getCollectionName(instrumentType))
                .stream()
                .findFirst();
    }

    public Optional<ChainSnapshot> find_first_snapshot_of_the_day(
            String underlying,
            LocalDate expiryDate,
            InstrumentType instrumentType) {

        Query query = new Query();

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.underlying.name()).is(underlying)
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.expiryDate.name())
                        .is(expiryDate.format(expiryDateFormatter))
        );

        query.with(
                Sort.by(Sort.Direction.ASC,
                        SnapshotCollectionQueryingConstants.nseTimestamp.name())
        );

        query.limit(1);

        return mongoTemplate.find(query, ChainSnapshot.class,getCollectionName(instrumentType))
                .stream()
                .findFirst();
    }

    public Optional<ChainSnapshot> find_yesterdays_closing_snapshot(
            String underlying,
            LocalDate expiryDate,
            InstrumentType instrumentType) {

        Query query = new Query();

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.underlying.name()).is(underlying)
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.expiryDate.name())
                        .is(expiryDate.format(expiryDateFormatter))
        );

        query.with(
                Sort.by(Sort.Direction.DESC,
                        SnapshotCollectionQueryingConstants.nseTimestamp.name())
        );

        query.limit(1);

        return mongoTemplate.find(query, ChainSnapshot.class,getCollectionName(instrumentType))
                .stream()
                .findFirst();
    }

    public List<ChainSnapshot> find_snapshots_between_start_and_end_time(
            String underlying,
            LocalDate expiryDate,
            LocalDateTime start,
            LocalDateTime end,
            InstrumentType instrumentType) {

        Query query = new Query();

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.underlying.name())
                        .is(underlying)
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.expiryDate.name())
                        .is(expiryDate.format(expiryDateFormatter))
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.nseTimestamp.name())
                        .gte(start)
                        .lte(end)
        );

        query.with(
                Sort.by(SnapshotCollectionQueryingConstants.nseTimestamp.name())
        );

        return mongoTemplate.find(
                query,
                ChainSnapshot.class,
                getCollectionName(instrumentType)
        );
    }

    public List<ChainSnapshot> find_ATM_strike_snapshots(
            String underlying,
            LocalDate expiryDate,
            double atm,
            double range,
            InstrumentType instrumentType) {

        Query query = new Query();

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.underlying.name()).is(underlying)
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.expiryDate.name())
                        .is(expiryDate.format(expiryDateFormatter))
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.strikePrice.name())
                        .gte(atm - range)
                        .lte(atm + range)
        );

        return mongoTemplate.find(
                query,
                ChainSnapshot.class,
                getCollectionName(instrumentType)
        );
    }

    public List<ChainSnapshot> find_pre_open_session_snapshots(
            String underlying,
            LocalDate expiryDate,
            InstrumentType instrumentType) {
        LocalDate today = LocalDate.now(IST);
        LocalDateTime start =
                today.atTime(9,0);

        LocalDateTime end =
                today.atTime(9,15);

        Query query = new Query();

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.underlying.name())
                        .is(underlying)
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.expiryDate.name())
                        .is(expiryDate.format(expiryDateFormatter))
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.nseTimestamp.name())
                        .gte(start)
                        .lt(end)
        );

        return mongoTemplate.find(
                query,
                ChainSnapshot.class,
                getCollectionName(instrumentType)
        );
    }

    public List<ChainSnapshot> findSnapshots_for_session(
            String underlying,
            LocalDate expiryDate,
            LocalDateTime startTime,
            LocalDateTime endTime,
            InstrumentType instrumentType) {

        Query query = new Query();

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.underlying.name())
                        .is(underlying)
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.expiryDate.name())
                        .is(expiryDate.format(expiryDateFormatter))
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.nseTimestamp.name())
                        .gte(startTime)
                        .lt(endTime)
        );

        return mongoTemplate.find(
                query,
                ChainSnapshot.class,
                getCollectionName(instrumentType)
        );
    }

    public Optional<ChainSnapshot> findLatestCapturedSnapshot(
            String underlying,
            LocalDate expiryDate,
            InstrumentType instrumentType) {

        Query query = new Query();

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.underlying.name()).is(underlying)
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.expiryDate.name())
                        .is(expiryDate.format(expiryDateFormatter))
        );

        query.with(
                Sort.by(
                        Sort.Direction.DESC,
                        SnapshotCollectionQueryingConstants.snapshotTime.name()
                )
        );

        query.limit(1);

        return mongoTemplate.find(query, ChainSnapshot.class,getCollectionName(instrumentType))
                .stream()
                .findFirst();
    }

    public List<ChainSnapshot> findLatestCapturedSnapshot_soFar(
            String underlying,
            Long expiryDate,
            InstrumentType instrumentType) {

        Query query = new Query();

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.underlying.name()).is(underlying)
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.expiryDate.name())
                        .is(expiryDate)
        );

        query.with(
                Sort.by(
                        Sort.Direction.DESC,
                        SnapshotCollectionQueryingConstants.snapshotTime.name()
                )
        );

        return mongoTemplate.find(query, ChainSnapshot.class,getCollectionName(instrumentType));
    }

    public List<ChainSnapshot> findBySnapshotTimeRange_usingSnapshotInstants_start_and_end_time(
            String underlying,
            LocalDate expiryDate,
            Instant start,
            Instant end,
            InstrumentType instrumentType) {

        Query query = new Query();

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.underlying.name()).is(underlying)
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.expiryDate.name())
                        .is(expiryDate.format(expiryDateFormatter))
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.snapshotTime.name())
                        .gte(start)
                        .lte(end)
        );

        query.with(
                Sort.by(SnapshotCollectionQueryingConstants.snapshotTime.name())
        );

        return mongoTemplate.find(
                query,
                ChainSnapshot.class,
                getCollectionName(instrumentType)
        );
    }

    public List<Instant> findTodaySnapshotRuns(
            String underlying,
            LocalDate expiryDate,
            InstrumentType instrumentType) {


        Instant startOfDay =
                LocalDate.now(IST)
                        .atStartOfDay(IST)
                        .toInstant();

        Instant endOfDay =
                LocalDate.now(IST)
                        .plusDays(1)
                        .atStartOfDay(IST)
                        .toInstant();

        Query query = new Query();

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.underlying.name())
                        .is(underlying)
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.expiryDate.name())
                        .is(expiryDate.format(expiryDateFormatter))
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.snapshotTime.name())
                        .gte(startOfDay)
                        .lt(endOfDay)
        );

        return mongoTemplate.findDistinct(
                        query,
                        SnapshotCollectionQueryingConstants.snapshotTime.name(),
                        getCollectionName(instrumentType),
                        ChainSnapshot.class,
                        Instant.class
                ).stream()
                .sorted()
                .toList();
    }

    public List<ChainSnapshot> findToday_inserted_snapshots(
            String underlying,
            String expiryDate,
            InstrumentType instrumentType) {

        LocalDate today = LocalDate.now(IST);

        Instant startOfDay = today
                .atStartOfDay(IST)
                .toInstant();

        Instant endOfDay = today
                .plusDays(1)
                .atStartOfDay(IST)
                .toInstant();

        Query query = new Query();

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.underlying.name())
                        .is(underlying)
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.expiryDate.name())
                        .is(expiryDate)
        );

        query.addCriteria(
                Criteria.where(SnapshotCollectionQueryingConstants.snapshotTime.name())
                        .gte(startOfDay)
                        .lt(endOfDay)
        );

        query.with(
                Sort.by(
                        Sort.Direction.ASC,
                        SnapshotCollectionQueryingConstants.snapshotTime.name(),
                        SnapshotCollectionQueryingConstants.strikePrice.name()
                )
        );

        return mongoTemplate.find(
                query,
                ChainSnapshot.class,
                getCollectionName(instrumentType)
        );
    }
}
