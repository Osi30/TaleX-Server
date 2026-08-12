package com.talex.server.services.recommend;

import java.util.List;
import java.util.Set;

public interface SeriesChannelService {

    // --- Promoted Channel ---
    List<String> getPromotedPoolElements();
    List<String> getPromotedSeriesIds(String accountId, int limit);
    List<String> refreshPromotedPool(int limit);

    // --- New Releases Channel ---
    List<String> getNewReleasesPoolElements();
    List<String> getNewReleasesSeriesIds(String accountId, int limit);
    List<String> refreshNewReleasesPool(List<String> blacklistIds, int limit);

    // --- Recently Updated Channel ---
    List<String> getRecentlyUpdatedPoolElements();
    List<String> getRecentlyUpdatedSeriesIds(String accountId, int limit);
    List<String> refreshRecentlyUpdatedPool(List<String> blacklistIds, int limit);

    // --- Latest Community Choice Channel ---
    List<String> getLatestCommunityChoicePoolElements();
    List<String> getLatestCommunityChoiceSeriesIds(String accountId, int limit);
    List<String> refreshLatestCommunityChoicePool(List<String> blacklistIds, int limit);

    // --- Community Choice Channel ---
    List<String> getCommunityChoicePoolElements();
    List<String> getCommunityChoiceSeriesIds(String accountId, int limit);
    List<String> refreshCommunityChoicePool(List<String> blacklistIds, int limit);

    // --- Random Category Channel ---
    List<String> getRandomCategoryPoolElements();
    List<String> getRandomCategorySeriesIds(String accountId, int limit);
    List<String> refreshRandomCategoryPool(List<String> blacklistIds, int limitPerCategory, int totalLimit);

    // --- Trending Channel ---
    List<String> getTrendingPoolElements();
    List<String> getTrendingSeriesIds(String accountId, int limit);
    List<String> refreshTrendingPool(List<String> blacklistIds, int limit);

    // --- Account Subscription Channel ---
    List<String> getSubscribedCreatorsSeriesIds(String accountId, Set<String> blacklistIds, int limit);
    List<String> refreshSubscribedCreatorsPool(String accountId, Set<String> blacklistIds, int limitPerCreator, int totalLimit);
    List<String> getAllSubscribedCreatorsSeriesIds(String accountId);

    // --- Onboarding Preferences Channel ---
    List<String> getOnboardingPreferencesSeriesIds(String accountId, Set<String> blacklistIds, int totalLimit);

    // --- Dynamic Preferences Channel ---
    List<String> getDynamicPreferencesSeriesIds(String accountId, Set<String> blacklistIds, int totalLimit);

    // --- Global IDs ---
    void updateGlobalIds(Set<String> allGlobalIds);
    Set<String> getAllGlobalIds();
}
