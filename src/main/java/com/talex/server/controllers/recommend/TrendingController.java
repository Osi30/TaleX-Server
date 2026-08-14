package com.talex.server.controllers.recommend;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.recommend.response.TrendingSampleConfigRes;
import com.talex.server.dtos.responses.series.SeriesTrendingResponseDto;
import com.talex.server.enums.interaction.ImpressionStatus;
import com.talex.server.services.trending.TrendingSampleConfigService;
import com.talex.server.services.trending.TrendingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trending/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Trending Dashboard", description = "API quản lý, kích hoạt đánh giá và theo dõi xu hướng (Trending/New Releases)")
public class TrendingController {
    private final TrendingService trendingService;
    private final TrendingSampleConfigService configService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/eval-wilson")
    @Operation(
            summary = "Kích hoạt đánh giá Wilson Score (Vòng 1)",
            description = "Thủ công kích hoạt tiến trình chạy đánh giá Wilson Score Vòng 1 cho các series. Chỉ Admin."
    )
    public ResponseEntity<BaseResponse> triggerWilsonEvaluation() {
        log.info("[AdminAction] Thủ công kích hoạt đánh giá Wilson Score Vòng 1...");
        trendingService.evaluateWilsonScoreBatch();
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Thực thi đánh giá Wilson Score Vòng 1 thành công!")
                .data(null)
                .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/eval-ranking")
    @Operation(
            summary = "Kích hoạt cập nhật Hacker News Ranking Score (Vòng 2)",
            description = "Thủ công kích hoạt tính toán và cập nhật điểm xếp hạng Hacker News Ranking Score cho các Series có trạng thái SUCCESS. Chỉ Admin."
    )
    public ResponseEntity<BaseResponse> triggerRankingEvaluation() {
        log.info("[AdminAction] Thủ công kích hoạt tính toán Hacker News Ranking Score...");
        trendingService.recalculateHackerNewsRankingScores();
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Thực thi cập nhật Hacker News Ranking Score thành công!")
                .data(null)
                .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/force-threshold")
    @Operation(
            summary = "Ép buộc tính toán lại điểm mốc xu hương",
            description = "Reset currentBatch về 0 và tính toán lại điểm mốc xu hương từ toàn bộ dữ liệu lịch sử."
    )
    public ResponseEntity<BaseResponse> forceRecalculateThreshold() {
        log.info("[AdminAction] Admin force tính toán lại Threshold...");
        TrendingSampleConfigRes result = configService.forceRecalculateThreshold();
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Ép buộc tính toán lại Threshold thành công!")
                .data(result)
                .build());
    }

    @Operation(
            summary = "Danh sách ứng viên chờ phân phối",
            description = "Lấy danh sách ứng viên đang chờ phân phối Trending"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/candidate-new-releases")
    public ResponseEntity<BaseResponse> getCandidateNewReleasesSeries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<SeriesTrendingResponseDto> candidateIds = trendingService.getCandidateNewReleasesSeriesIds(page, size);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách ứng viên New Releases thành công!")
                .data(candidateIds)
                .build());
    }

    @Operation(
            summary = "Danh sách Series trong Pool New Releases",
            description = "Lấy danh sách chi tiết các Series hiện đang phân phối"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/new-releases-pool")
    public ResponseEntity<BaseResponse> getNewReleasesPoolSeries() {
        List<SeriesTrendingResponseDto> result = trendingService.getNewReleasesPoolSeries();
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách Series trong Pool New Releases thành công!")
                .data(result)
                .build());
    }

    @GetMapping("/evaluated-series")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Danh sách Series đã qua đánh giá Vòng 1",
            description = "Lấy danh sách các Series đã qua đánh giá Vòng 1 (có ImpressionStatus là SUCCESS hoặc FAILED), sắp xếp giảm dần theo thời gian cập nhật Wilson (wilson_updated_at) và hỗ trợ phân trang. Chỉ Admin."
    )
    public ResponseEntity<BaseResponse> getEvaluatedSeries(
            @RequestParam(required = false) List<ImpressionStatus> statuses,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        BasePageResponse<SeriesTrendingResponseDto> result = trendingService.getEvaluatedSeries(statuses, page, size);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách Series đã qua đánh giá Vòng 1 thành công!")
                .data(result)
                .build());
    }
}
