package com.talex.server.repositories.mongo;

import com.talex.server.entities.mongo.SeriesMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeriesMetadataRepository extends MongoRepository<SeriesMetadata, String> {
}
