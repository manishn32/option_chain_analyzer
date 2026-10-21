package com.nse.analyser.repositories.impl;

import com.nse.analyser.models.ChainSnapshot;
import com.nse.analyser.repositories.ChainSnapshotCustomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;

import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChainSnapshotCustomRepositoryImpl
        implements ChainSnapshotCustomRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<ChainSnapshot> getStrikeHistory(
            String underlying,
            String expiryDate,
            Double strike) {

        MatchOperation match =
                Aggregation.match(
                        Criteria.where("underlying")
                                .is(underlying)
                                .and("expiryDate")
                                .is(expiryDate)
                );

        UnwindOperation unwind =
                Aggregation.unwind("strikes");

        MatchOperation strikeMatch =
                Aggregation.match(
                        Criteria.where("strikes.strikePrice")
                                .is(strike)
                );

        Aggregation aggregation =
                Aggregation.newAggregation(
                        match,
                        unwind,
                        strikeMatch
                );

        return mongoTemplate
                .aggregate(
                        aggregation,
                        "chainSnapshot",
                        ChainSnapshot.class
                )
                .getMappedResults();
    }
}