package com.talex.server.dtos.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaComicPagesRequestDto {
    @Valid
    @NotEmpty
    private List<MediaComicPageRequestDto> pages;

    private String actorId;
}
