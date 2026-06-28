package com.talex.server.dtos.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.talex.server.enums.ContentType;
import com.talex.server.enums.SeriesStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeriesRequestDto {
//    @JsonIgnore
//    private String creatorId;

    @NotBlank
    private String title;

    private String description;

    private String coverUrl;

    private String bannerUrl;

    @NotNull
    private ContentType contentType;

    private SeriesStatus status;

    private String ageRating;

    private String language;

    private List<String> categoryIds;

    private List<String> tagIds;

}
