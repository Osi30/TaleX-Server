package com.talex.server.dtos.requests.ads;

import lombok.*;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InVideoConfigDto {
    private Integer skipAfterSec;
    private Integer cooldownSeconds;
}
