package com.nse.analyser.repositories.mongoRepositories.repo;

import com.nse.analyser.models.ChainSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NseSnapshotRepo extends MongoRepository<ChainSnapshot, String> {
}
