package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.services.media.MediaPlaybackSecurityService;
import com.talex.server.services.media.MediaService;
import com.talex.server.services.series.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
