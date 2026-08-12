package com.talex.server.dtos.recommend.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankRequestPayload {
    private String accountId;
    private List<String> seriesIds;
}
