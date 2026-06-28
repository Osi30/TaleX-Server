package com.talex.server.dtos.requests;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledPublishRequestDto {
    @NotNull
    @Future
    private LocalDateTime scheduledPublishAt;
}
