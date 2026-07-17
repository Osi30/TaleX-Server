package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.services.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Recommendation API",
        description = "Các API phục vụ cho tính năng đề xuất nội dung và phân tích hành vi người dùng"
)
public class RecommendationController {
    private final RecommendationService recommendationService;

    @GetMapping("/recent-series")
    @Operation(
            summary = "Lấy danh sách 5 series gần nhất mà người dùng đã xem",
            description = "API này thực hiện lấy ra danh sách tối đa 5 series_id mà người dùng (truy vấn theo accountId) đã xem gần nhất. " +
                    "Hệ thống sẽ ưu tiên đọc dữ liệu từ bộ nhớ đệm Redis Cache cực nhanh. Trong trường hợp Cache trống hoặc " +
                    "hết hạn (TTL), hệ thống sẽ chủ động fallback truy vấn dữ liệu từ QuestDB (sử dụng cú pháp tối ưu LATEST ON), " +
                    "sau đó trả về kết quả đồng thời tự động tái cấu trúc (rebuild) lại cache trên Redis cho các lần truy vấn tiếp theo."
    )
    public ResponseEntity<List<String>> getRecentWatchedSeries(
            @CurrentAccountId UUID accountId
    ) {
        List<String> recentSeries = recommendationService
                .getRecentWatchedSeries(accountId == null ? "" : accountId.toString());
        return ResponseEntity.ok(recentSeries);
    }
}