package com.talex.server.services;

import java.util.List;

public interface RecommendationService {
    List<String> getRecentWatchedSeries(String accountId);
}
