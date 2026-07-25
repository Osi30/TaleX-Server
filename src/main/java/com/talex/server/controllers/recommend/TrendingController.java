package com.talex.server.controllers.recommend;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.recommend.TrendingSampleConfigRes;
import com.talex.server.services.trending.TrendingSampleConfigService;
import com.talex.server.services.trending.TrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trending/dashboard")
@RequiredArgsConstructor
@Slf4j
public class TrendingController {
    private final TrendingService trendingService;
    private final TrendingSampleConfigService configService;

    /**
     * 1. Test Cron Job 1: Kích hoạt chạy đánh giá Wilson Score Vòng 1
     */
    @PostMapping("/eval-wilson")
    public ResponseEntity<BaseResponse> triggerWilsonEvaluation() {
        log.info("[AdminAction] Thủ công kích hoạt đánh giá Wilson Score Vòng 1...");
        trendingService.evaluateWilsonScoreBatch();
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Thực thi đánh giá Wilson Score Vòng 1 thành công!")
                .data(null)
                .build());
    }

    /**
     * 2. Test Cron Job 2: Kích hoạt cập nhật Hacker News Ranking Score cho các Series SUCCESS
     */
    @PostMapping("/eval-ranking")
    public ResponseEntity<BaseResponse> triggerRankingEvaluation() {
        log.info("[AdminAction] Thủ công kích hoạt tính toán Hacker News Ranking Score...");
        trendingService.recalculateHackerNewsRankingScores();
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Thực thi cập nhật Hacker News Ranking Score thành công!")
                .data(null)
                .build());
    }

    /**
     * 3. Force tính toán Threshold: Reset currentBatch = 0 và tính toán lại ngay Threshold từ toàn bộ lịch sử
     */
    @PostMapping("/force-threshold")
    public ResponseEntity<BaseResponse> forceRecalculateThreshold() {
        log.info("[AdminAction] Admin force tính toán lại Threshold...");
        TrendingSampleConfigRes result = configService.forceRecalculateThreshold();
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Ép buộc tính toán lại Threshold thành công!")
                .data(result)
                .build());
    }
}
