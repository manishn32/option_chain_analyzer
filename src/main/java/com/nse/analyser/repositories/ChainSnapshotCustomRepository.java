package com.nse.analyser.repositories;

import com.nse.analyser.models.ChainSnapshot;

import java.util.List;

public interface ChainSnapshotCustomRepository {

    List<ChainSnapshot> getStrikeHistory(
            String underlying,
            String expiryDate,
            Double strike
    );
}