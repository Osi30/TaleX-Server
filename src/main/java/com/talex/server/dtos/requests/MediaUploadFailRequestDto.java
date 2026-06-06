package com.talex.server.dtos.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadFailRequestDto {
    private String errorMessage;
    private String actorId;
}
