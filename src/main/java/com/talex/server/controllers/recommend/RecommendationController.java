package com.talex.server.controllers.recommend;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.recommend.HomePoolsSeriesResponseDto;
import com.talex.server.dtos.recommend.RankResultItem;
import com.talex.server.dtos.recommend.SeriesCardResponseDto;
import com.talex.server.services.recommend.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/home-feed")
    @Operation(
            summary = "Lấy danh sách Series đề xuất cho trang chủ từ 8 Kênh",
            description = "Truy vấn danh sách Series IDs từ 8 kênh (Promoted, Trending, New Releases, Recently Updated, " +
                    "Latest Community Choice, Community Choice, Random Category, và Account Subscription). " +
                    "Kết quả trả về được phân loại rõ ràng theo từng Pool và có sẵn danh sách phẳng được xếp theo đúng thứ tự."
    )
    public ResponseEntity<BaseResponse> getHomeFeedSeries(
            @CurrentAccountId UUID accountId,
            @RequestParam(defaultValue = "5") int limitPerPool
    ) {
        HomePoolsSeriesResponseDto result = recommendationService.getHomeFeedSeries(
                accountId == null ? null : accountId.toString(),
                limitPerPool
        );
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy thành công!")
                .data(result)
                .build());
    }

    @GetMapping("/feed")
    @Operation(
            summary = "Lấy danh sách Series đề xuất cá nhân hóa (Cuộn vô cùng)",
            description = "API đề xuất Series dựa trên thói quen xem gần đây của người dùng kết hợp với 8 Kênh hệ thống và AI LightGBM. " +
                    "Tự động tạo Redis Pool theo Session trong lần đầu và phân trang cuộn vô cùng (offset/limit). " +
                    "Tất cả ID trả về được tự động đẩy sang Kafka cho ImpressionWorker ghi nhận lượt hiển thị."
    )
    public ResponseEntity<BaseResponse> getPersonalizedFeed(
            @CurrentAccountId UUID accountId,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(defaultValue = "HOME") String pageType,
            @RequestParam(defaultValue = "12") int limit
    ) {
        if (accountId == null) return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Chưa đăng nhập nên chưa đề xuất!")
                .data(null)
                .build());

        List<SeriesCardResponseDto> recommendations = recommendationService.getPersonalizedRecommendations(
                accountId.toString(),
                sessionId,
                pageType,
                limit
        );

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách đề xuất thành công!")
                .data(recommendations)
                .build());
    }

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

    @GetMapping("/similar")
    @Operation(
            summary = "Lấy danh sách các series tương tự (Similar IDs)",
            description = "API này thực hiện lấy ra danh sách các tương tự của một series_id. " +
                    "Hệ thống kiểm tra cache Redis trước (Key dạng recommendation:series:{id}). " +
                    "Nếu không tồn tại hoặc hết hạn, hệ thống tìm kiếm trong MongoDB bộ sưu tập 'series_recommendations', " +
                    "sau đó tự động đồng bộ (rebuild) lại dữ liệu sang Redis với thời gian hết hạn 7 ngày để tối ưu hiệu năng."
    )
    public ResponseEntity<List<String>> getSimilarSeriesIds(
            @RequestParam("seriesId") String seriesId
    ) {
        List<String> similarSeries = recommendationService.getSimilarSeriesIds(seriesId);
        return ResponseEntity.ok(similarSeries);
    }

    @PostMapping("/rank")
    @Operation(
            summary = "Xếp hạng tinh danh sách các series ứng viên bằng mô hình AI LightGBM",
            description = "API này nhận vào danh sách các seriesIds ứng viên (ví dụ: 100 sản phẩm thu được từ tích vô hướng ở tầng Retrieval), " +
                    "sau đó chuyển tiếp thông tin cùng accountId sang TaleX AI Service (Python) qua giao thức HTTP REST đồng bộ. " +
                    "Hệ thống Python sẽ trích xuất đặc trưng từ MongoDB, nạp vào mô hình LightGBM để tính điểm tương thích chi tiết, " +
                    "và trả về danh sách các ID đã sắp xếp theo thứ tự ưu tiên giảm dần để hiển thị trực tiếp lên UI."
    )
    public ResponseEntity<List<RankResultItem>> rankSeriesIds(
            @CurrentAccountId UUID accountId,
            @RequestBody List<String> seriesIds
    ) {
        List<RankResultItem> rankedSeries = recommendationService.rankSeries(
                accountId == null ? "" : accountId.toString(),
                seriesIds
        );
        return ResponseEntity.ok(rankedSeries);
    }

    @GetMapping("/session")
    @Operation(
            summary = "Lấy danh sách series gợi ý",
            description = "API nhận vào phiên xem để trả ra danh sách gợi ý series cho người xem."
    )
    public ResponseEntity<List<RankResultItem>> getSeries(
            @RequestParam(value = "accountId") String accountId,
            @RequestParam(value = "viewSessionId") String viewSessionId,
            @RequestParam(value = "seriesIds") List<String> seriesIds
    ) {
        String userIdStr = accountId == null ? "guest_user" : accountId;
        List<RankResultItem> finalRecommendations = recommendationService.getRecommendations(userIdStr, seriesIds, viewSessionId);
        return ResponseEntity.ok(finalRecommendations);
    }
}