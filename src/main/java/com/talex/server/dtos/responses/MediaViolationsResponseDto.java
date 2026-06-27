package com.talex.server.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaViolationsResponseDto {
    private String mediaId;
    private String contentId;
    private List<MediaCopyrightResponseDto> copyrightViolations;
    private List<ContentCensorshipResponseDto> censorshipResults;
}
