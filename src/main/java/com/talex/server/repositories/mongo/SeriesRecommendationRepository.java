package com.talex.server.repositories.mongo;

import com.talex.server.entities.mongo.SeriesRecommendation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeriesRecommendationRepository extends MongoRepository<SeriesRecommendation, String> {
}
