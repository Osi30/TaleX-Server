package com.talex.server.services.mongo;

import com.talex.server.dtos.mongo.UserFeatureRequest;
import com.talex.server.entities.mongo.UserFeatureDocument;

import java.util.List;
import java.util.Optional;

public interface IUserFeatureService {
    UserFeatureDocument saveOrUpdateFeatures(String userId, UserFeatureRequest request);

    Optional<UserFeatureDocument> getFeaturesByUserId(String userId);

    void syncUserDynamicFeatures();

    void syncUserDynamicPreferences();

    void syncUserMonetizationFeatures();

    void cleanUp24hFeatures(List<String> accountIds);

    void cleanUp7dFeatures(List<String> accountIds);
}
