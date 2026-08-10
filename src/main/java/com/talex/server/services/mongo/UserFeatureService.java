package com.talex.server.services.mongo;

import com.talex.server.dtos.mongo.UserDynamicFeature;
import com.talex.server.dtos.mongo.UserFeatureRequest;
import com.talex.server.dtos.mongo.UserStaticFeature;
import com.talex.server.entities.mongo.UserFeatureDocument;

import java.util.List;
import java.util.Optional;

public interface UserFeatureService {
    /// Cập nhập dữ liệu tĩnh từ người dùng
    UserFeatureDocument saveOrUpdateFeatures(String userId, UserFeatureRequest request);

    Optional<UserFeatureDocument> getFeaturesByUserId(String userId);

    UserStaticFeature getUserStaticFeatureByAccountId(String accountId);

    UserDynamicFeature getUserDynamicFeatureByAccountId(String accountId);

    /// Lưu trữ InteractionStats và DeepEngagementStats
    void syncUserDynamicFeatures();

    /// Lưu trữ DynamicPreferences
    void syncUserDynamicPreferences();

    void cleanUp24hFeatures(List<String> accountIds);

    void cleanUp7dFeatures(List<String> accountIds);
}
