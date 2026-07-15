package com.talex.server.dtos.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestDbPreferenceResult {
    private String accountId;
    private String episodeId;
    private Long totalClicks;
    private Double totalWatchTime; // Giây
}