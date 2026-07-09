package com.talex.server.repositories.mongo;

import com.talex.server.entities.mongo.UserFeatureDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserFeatureRepository extends MongoRepository<UserFeatureDocument, String> {
Optional<UserFeatureDocument> findByAccountId(String accountId);
}