package com.talex.server.dtos.responses.media;

import com.talex.server.enums.media.MediaPlaybackPolicy;
import com.talex.server.enums.media.MediaProtectionType;
import com.talex.server.enums.media.MediaProvider;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.media.MediaType;
import com.talex.server.enums.series.ContentApprovalStatus;
import com.talex.server.enums.series.EpisodeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponseDto {
    private String mediaId;
    private String episodeId;
    // Trạng thái của EPISODE chứa media này — cần cho tab "Đã duyệt" (admin) biết Admin có
    // đang ép ẩn cả episode hay không, vì hành động "Ẩn nội dung" giờ thao tác ở cấp
    // Episode (xem EpisodeServiceImpl.forceHide), không phải cấp Media.
    private EpisodeStatus episodeStatus;
    private String creatorId;
    private Boolean isLocked;
    private MediaType mediaType;
    private String mimeType;
    private String fileUrl;
    private String externalPublicId;
    private String storageProvider;
    private MediaProvider provider;
    private String providerAssetId;
    private String providerPublicId;
    private String providerDeliveryType;
    private String originalUrl;
    private String playbackUrl;
    private String hlsUrl;
    private String signedPlaybackUrl;
    private String thumbnailUrl;
    private String previewUrl;
    private String format;
    private MediaProtectionType protectionType;
    private MediaPlaybackPolicy playbackPolicy;
    private String drmProvider;
    private String drmKeyId;
    private String drmLicenseUrl;
    private String drmCertificateUrl;
    private Integer tokenTtlSeconds;
    private String errorMessage;
    private Boolean pendingDelete;
    private Long fileSize;
    private String checksum;
    private Integer width;
    private Integer height;
    private String resolution;
    private Long duration;
    private Integer displayOrder;
    private MediaStatus status;
    private ContentApprovalStatus approvalStatus;
    private LocalDateTime approvalReviewedAt;
    private String approvalReviewedBy;
    private String approvalReviewedByName;
    private String approvalReviewedByRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private String createdBy;
    private String updatedBy;
    private String deletedBy;
    private Boolean isDeleted;
    private String contentId;
    private String copyrightId;
    // Chỉ set ở danh sách kiểm duyệt (listPendingReview/listApproved) — số Media khác trong
    // CÙNG episode này khớp cùng bộ filter đang áp dụng, để FE hiện badge thay vì lặp lại
    // từng Media của 1 episode thành nhiều card riêng (episode đã group ở tầng BE).
    private Integer episodeMediaCount;
    // Các field dưới đây chỉ set ở danh sách kiểm duyệt (listPendingReview/listApproved) —
    // Staff/Admin cần biết nội dung thuộc episode/season/series/creator nào để duyệt/ép ẩn
    // đúng, tránh phải tra episodeId thủ công.
    private String episodeTitle;
    private String seasonTitle;
    private String seriesTitle;
    private String creatorUsername;
}
