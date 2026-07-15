package com.talex.server.records;

public record SeriesLogData (
        String seriesId,
        Long totalClicks,
        Long totalLikes,
        Long totalBookmarks,
        Long totalShares,
        Long totalComments,
        Double watchTime
){
}
