package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.recommend.response.SeriesCardResponseDto;
import com.talex.server.dtos.requests.series.SeriesSearchCriteria;
import com.talex.server.enums.series.ContentType;
import com.talex.server.enums.series.SeriesStatus;
import com.talex.server.services.media.MediaPlaybackSecurityService;
import com.talex.server.services.media.MediaService;
import com.talex.server.services.series.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicContentController {
    private final ComboEpisodeService comboEpisodeService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final SeriesService seriesService;
    private final SeasonService seasonService;
    private final EpisodeService episodeService;
    private final MediaService mediaService;
    private final MediaPlaybackSecurityService mediaPlaybackSecurityService;

    @GetMapping("/categories")
    public ResponseEntity<BaseResponse> listCategories(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ResponseEntity.ok(response(200, "OK", categoryService.listPublic(page, pageSize)));
    }

    @GetMapping("/tags")
    public ResponseEntity<BaseResponse> listTags(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ResponseEntity.ok(response(200, "OK", tagService.listPublic(page, pageSize)));
    }

    @GetMapping("/series")
    public ResponseEntity<BaseResponse> listSeries(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ResponseEntity.ok(response(200, "OK", seriesService.listPublic(page, pageSize)));
    }

    @GetMapping("/series/search")
    @Operation(summary = "Tìm kiếm nâng cao series", description = "Lọc series công khai theo từ khóa (hỗ trợ tìm không dấu), loại nội dung, thể loại, tag, độ tuổi, status kèm sắp xếp và phân trang Slice (lướt vô tận).")
    public ResponseEntity<BaseResponse> searchSeries(
            @RequestParam(required = false) String seriesId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ContentType contentType,
            @RequestParam(required = false) List<String> ageRatings,
            @RequestParam(required = false) SeriesStatus status,
            @RequestParam(required = false) List<String> categoryIds,
            @RequestParam(required = false) List<String> tagIds,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {

        SeriesSearchCriteria criteria = SeriesSearchCriteria.builder()
                .seriesId(seriesId)
                .search(search)
                .contentType(contentType)
                .ageRatings(ageRatings)
                .status(status)
                .categoryIds(categoryIds)
                .tagIds(tagIds)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        Slice<SeriesCardResponseDto> response = seriesService.searchPublicSeries(criteria, pageable);

        return ResponseEntity.ok(response(200, "OK", response));
    }


    @GetMapping("/series/{seriesId}")
    public ResponseEntity<BaseResponse> getSeries(@PathVariable String seriesId) {
        return ResponseEntity.ok(response(200, "OK", seriesService.getPublicById(seriesId)));
    }

    @GetMapping("/series/{seriesId}/seasons")
    public ResponseEntity<BaseResponse> listSeasons(@PathVariable String seriesId) {
        return ResponseEntity.ok(response(200, "OK", seasonService.listPublicBySeries(seriesId)));
    }

    @GetMapping("/seasons/{seasonId}")
    public ResponseEntity<BaseResponse> getSeason(@PathVariable String seasonId) {
        return ResponseEntity.ok(response(200, "OK", seasonService.getPublicById(seasonId)));
    }

    @GetMapping("/seasons/{seasonId}/episodes")
    public ResponseEntity<BaseResponse> listEpisodes(@PathVariable String seasonId) {
        return ResponseEntity.ok(response(200, "OK", episodeService.listPublicBySeason(seasonId)));
    }

    @GetMapping("/episodes/{episodeId}")
    public ResponseEntity<BaseResponse> getEpisode(@PathVariable String episodeId) {
        return ResponseEntity.ok(response(200, "OK", episodeService.getPublicById(episodeId)));
    }

    @GetMapping("/episodes/{episodeId}/media")
    public ResponseEntity<BaseResponse> listMedia(
            @PathVariable String episodeId,
            @Parameter(hidden = true) @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "OK",
                mediaService.listPublicByEpisode(episodeId, accountId == null ? null : accountId.toString())));
    }

    @GetMapping("/episodes/{episodeId}/playback")
    public ResponseEntity<BaseResponse> getEpisodePlayback(
            @PathVariable String episodeId,
            @Parameter(hidden = true) @CurrentAccountId UUID accountId,
            HttpServletRequest request) {
        return ResponseEntity.ok(response(200, "OK",
                mediaPlaybackSecurityService.getEpisodePlayback(
                        episodeId,
                        accountId == null ? null : accountId.toString(),
                        request.getRemoteAddr(),
                        request.getHeader("User-Agent"))));
    }

    @GetMapping("/media/{mediaId}")
    public ResponseEntity<BaseResponse> getMedia(
            @PathVariable String mediaId,
            @Parameter(hidden = true) @CurrentAccountId UUID accountId) {
        String viewerId = accountId == null ? null : accountId.toString();
        return ResponseEntity.ok(response(200, "OK", mediaService.getPublicById(mediaId, viewerId)));
    }

    @GetMapping("/media/{mediaId}/watermarked-image")
    @Operation(summary = "Lấy ảnh có đính kèm watermark", description = "Lấy ảnh gốc và tự động đính kèm Creator ID và Viewer ID thông qua AI Backend.")
    public ResponseEntity<byte[]> getWatermarkedImage(
            @PathVariable String mediaId,
            @Parameter(hidden = true) @CurrentAccountId UUID accountId) {
        String viewerId = accountId == null ? null : accountId.toString();
        return mediaService.getWatermarkedImage(mediaId, viewerId);
    }

    @GetMapping("/combos")
    @Operation(summary = "Lấy danh sách Combo", description = "Lấy danh sách các combo.")
    public ResponseEntity<BaseResponse> list() {
        return ResponseEntity.ok(response(200, "OK", comboEpisodeService.getAll()));
    }

    private BaseResponse response(int code, String message, Object data) {
        return BaseResponse.builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}
