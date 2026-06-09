package com.talex.server.dtos.requests;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.temporal.ChronoUnit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequestDto {
    private String tier;
    private String description;

    @Positive(message = "Giá sản phẩm phải lớn hơn 0")
    private Long price;

    @Positive(message = "Thời lượng phải lớn hơn 0")
    private Integer duration;

    private ChronoUnit durationUnit;
    private Boolean isAdBlocked;
    private Boolean isMovieUnlocked;
    private Boolean isStoryUnlocked;
}
