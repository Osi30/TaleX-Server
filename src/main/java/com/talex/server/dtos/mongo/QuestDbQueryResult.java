package com.talex.server.dtos.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestDbQueryResult {
    private String accountId;
    private Long totalClicks;
    private Long totalLikes;
    private Long totalBookmarks;
    private Long totalShares;
    private Long totalComments;
    private Double periodWatchTime;
}