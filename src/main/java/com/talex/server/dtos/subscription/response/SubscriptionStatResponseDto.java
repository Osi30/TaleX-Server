package com.talex.server.dtos.subscription.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionStatResponseDto {

    private String id;

    @JsonProperty("month_year")
    private String monthYear;

    @JsonProperty("creator_id")
    private String creatorId;

    @JsonProperty("creator_email")
    private String creatorEmail;

    @JsonProperty("episode_id")
    private String episodeId;

    @JsonProperty("episode_number")
    private Integer episodeNumber;

    @JsonProperty("series_id")
    private String seriesId;

    @JsonProperty("series_title")
    private String seriesTitle;

    private Long views;
}