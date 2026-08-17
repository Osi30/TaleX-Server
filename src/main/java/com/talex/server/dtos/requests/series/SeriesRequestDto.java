package com.talex.server.dtos.requests.series;

import com.talex.server.enums.series.ContentType;
import com.talex.server.enums.series.SeriesStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

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

    // Danh sách "code" của ContentWarningCategory đã khai (VD "SEXUAL_NUDITY") — validate
    // đúng code tồn tại/đang active ở SeriesServiceImpl trước khi lưu.
    private Set<String> contentWarnings;

    private String language;

    private List<String> categoryIds;

    private List<String> tagIds;

}
