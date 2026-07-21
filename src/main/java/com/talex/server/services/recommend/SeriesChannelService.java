package com.talex.server.services.recommend;

import java.util.List;

public interface SeriesChannelService {
    List<String> getPromotedSeriesIds(List<String> blacklistIds, int limit);
    List<String> refreshPromotedPool(int limit);
}
