package com.talex.server.services.media;

import com.talex.server.dtos.requests.media.MediaComicPagesRequestDto;
import com.talex.server.dtos.requests.media.MediaMetadataRequestDto;
import com.talex.server.dtos.requests.media.MediaRejectRequestDto;
import com.talex.server.dtos.requests.media.MediaReorderRequestDto;
import com.talex.server.dtos.requests.media.MediaUpdateRequestDto;
import com.talex.server.dtos.responses.media.CreatorViolationsSummaryDto;
import com.talex.server.dtos.responses.media.MediaResponseDto;
import com.talex.server.dtos.responses.media.MediaViolationsResponseDto;
import com.talex.server.entities.media.Media;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MediaService {
    MediaResponseDto createFromUrl(String episodeId, MediaMetadataRequestDto request, String accountId);

    List<MediaResponseDto> createComicPagesFromUrls(String episodeId, MediaComicPagesRequestDto request, String accountId);

    MediaResponseDto getById(String id, String accountId);

    MediaResponseDto getPublicById(String id, String viewerId);

    org.springframework.http.ResponseEntity<byte[]> getWatermarkedImage(String mediaId, String viewerId);

    List<MediaResponseDto> listByEpisode(String episodeId, String accountId);

    List<MediaResponseDto> listPublicByEpisode(String episodeId, String viewerId);

    MediaResponseDto update(String id, MediaUpdateRequestDto request, String accountId);

    MediaResponseDto replaceUrl(String id, MediaMetadataRequestDto request, String accountId);

    List<MediaResponseDto> reorder(String episodeId, MediaReorderRequestDto request, String accountId);

    MediaResponseDto hide(String id, String actorId);

    MediaResponseDto unhide(String id, String actorId);

    MediaResponseDto forceHide(String id, String actorId);

    MediaResponseDto forceUnhide(String id, String actorId);

    MediaResponseDto approve(String id, String actorId);

    MediaResponseDto reject(String id, String actorId);

    CreatorViolationsSummaryDto getCreatorViolationsSummary(String creatorId);

    MediaResponseDto rejectWithReason(String id, String actorId, MediaRejectRequestDto request);

    /** Thử lại pipeline (Content ID + kiểm duyệt, resubmit transcode nếu VIDEO) cho media đang FAILED. */
    MediaResponseDto retryPipeline(String id, String actorId);

    void delete(String id, String actorId);

    Media findActiveEntity(String id);

    Media findManageableEntity(String id, String accountId);

    MediaResponseDto toResponse(Media media);

    MediaViolationsResponseDto getMediaViolations(String mediaId, String accountId);

    /**
     * @param keyword so khớp không phân biệt hoa/thường theo tiêu đề Episode/Season/Series,
     *                mediaId, contentId, và tên đăng nhập Creator, null/rỗng = không lọc.
     */
    Page<MediaResponseDto> listPendingReview(int page, int size, String mediaType, String keyword);

    /**
     * "Đã duyệt" — nội dung approvalStatus=APPROVED, sắp theo lượt duyệt gần nhất, cho
     * Staff/Admin xem lại và ép ẩn nếu cần.
     *
     * @param reviewFilter "manual" = chỉ nội dung Staff/Admin tự tay duyệt (từng bị flag
     *                     vi phạm), "clean" = chỉ nội dung pipeline tự duyệt (không vi phạm),
     *                     giá trị khác hoặc null = không lọc, trả về tất cả.
     * @param mediaType    "IMAGE"/"VIDEO" = chỉ lọc đúng loại đó, null/giá trị khác = không lọc.
     * @param keyword      so khớp không phân biệt hoa/thường theo tiêu đề Episode/Season/Series,
     *                     mediaId, contentId, và tên đăng nhập Creator, null/rỗng = không lọc.
     */
    Page<MediaResponseDto> listApproved(int page, int size, String reviewFilter, String mediaType, String keyword);

    /**
     * "Từ chối" — nội dung approvalStatus=REJECTED (Staff từ chối tay hoặc pipeline tự động
     * reject do lỗi hệ thống), sắp theo lượt duyệt gần nhất.
     *
     * @param mediaType "IMAGE"/"VIDEO" = chỉ lọc đúng loại đó, null/giá trị khác = không lọc.
     * @param keyword   so khớp không phân biệt hoa/thường theo tiêu đề Episode/Season/Series,
     *                  mediaId, contentId, và tên đăng nhập Creator, null/rỗng = không lọc.
     */
    Page<MediaResponseDto> listRejected(int page, int size, String mediaType, String keyword);
}
