package com.talex.server.dtos.responses.media;

import com.talex.server.enums.media.ViolationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaCopyrightResponseDto {
    private String mediaCopyrightId;
    private String mediaId;
    private String sourceMediaId;
    private Float startTimeTarget;
    private Float endTimeTarget;
    private Float startTimeSource;
    private Float endTimeSource;
    private Float similarityScore;
    private ViolationType violationType;
    private Boolean isValid;
    private String note;
    private LocalDateTime checkedAt;

    // Thông tin nội dung gốc bị trùng — CHỈ điền khi caller là STAFF/ADMIN (xem
    // MediaServiceImpl.mapCopyrightToDto). Creator tự xem violation của mình sẽ luôn
    // nhận null ở các field này, tránh lộ danh tính creator khác.
    private String sourceEpisodeTitle;
    private String sourceSeriesTitle;
    private String sourceCreatorUsername;
    private String sourceThumbnailUrl;
    private Boolean sourceMediaDeleted;
}
