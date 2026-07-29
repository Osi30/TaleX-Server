package com.talex.server.dtos.responses.ads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdLabelResponseDto {
    private UUID labelId;
    private UUID profileId;
    private String name;
    private String color;
}
