package com.talex.server.services.recommend;

import java.util.List;
import java.util.Set;

public interface SeriesChannelService {

    // --- Promoted Channel ---
    List<String> getPromotedSeriesIds(String accountId, int limit);
    List<String> refreshPromotedPool(int limit);

    // --- New Releases Channel ---
    List<String> getNewReleasesSeriesIds(String accountId, int limit);
    List<String> refreshNewReleasesPool(List<String> blacklistIds, int limit);

    // --- Recently Updated Channel ---
    List<String> getRecentlyUpdatedSeriesIds(String accountId, int limit);
    List<String> refreshRecentlyUpdatedPool(List<String> blacklistIds, int limit);

    // --- Latest Community Choice Channel ---
    List<String> getLatestCommunityChoiceSeriesIds(String accountId, int limit);
    List<String> refreshLatestCommunityChoicePool(List<String> blacklistIds, int limit);

    // --- Community Choice Channel ---
    List<String> getCommunityChoiceSeriesIds(String accountId, int limit);
    List<String> refreshCommunityChoicePool(List<String> blacklistIds, int limit);

    // --- Random Category Channel ---
    List<String> getRandomCategorySeriesIds(String accountId, int limit);
    List<String> refreshRandomCategoryPool(List<String> blacklistIds, int limitPerCategory, int totalLimit);

    // --- Account Subscription Channel ---
    List<String> getSubscribedCreatorsSeriesIds(String accountId, int limit);
    List<String> refreshSubscribedCreatorsPool(String accountId, List<String> blacklistIds, int limitPerCreator, int totalLimit);

    // --- Global IDs ---
    void updateGlobalIds(Set<String> allGlobalIds);
    Set<String> getAllGlobalIds();
}
