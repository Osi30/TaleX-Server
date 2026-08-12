package com.talex.server.controllers.recommend;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.recommend.request.HomeFeedRequestDto;
import com.talex.server.dtos.recommend.response.HomePoolsSeriesResponseDto;
import com.talex.server.dtos.recommend.response.PoolSeriesCardResponseDto;
import com.talex.server.dtos.recommend.response.RankResultItem;
import com.talex.server.dtos.recommend.response.SeriesCardResponseDto;
import com.talex.server.services.recommend.RecommendationService;
import com.talex.server.services.series.SeriesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
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
    private final SeriesService seriesService;

    @GetMapping("/home-feed")
    @Operation(
            summary = "Lấy danh sách Series đề xuất cho trang chủ từ 8 Kênh",
            description = "Truy vấn danh sách Series IDs từ 8 kênh (Promoted, Trending, New Releases, Recently Updated, " +
                    "Latest Community Choice, Community Choice, Random Category, và Account Subscription). " +
                    "Kết quả trả về được phân loại rõ ràng theo từng Pool và có sẵn danh sách phẳng được xếp theo đúng thứ tự."
    )
    public ResponseEntity<BaseResponse> getHomeFeedSeries(
            @CurrentAccountId UUID accountId,
            @Valid @ModelAttribute HomeFeedRequestDto request
    ) {
        HomePoolsSeriesResponseDto result = recommendationService.getHomeFeedSeries(
                accountId == null ? null : accountId.toString(),
                request
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
    public ResponseEntity<BaseResponse> getRecentWatchedSeries(
            @RequestParam String accountId
    ) {
        List<String> recentSeriesIds = recommendationService
                .getRecentWatchedSeries(accountId == null ? "" : accountId);

        List<SeriesCardResponseDto> seriesCards = seriesService.getSeriesCardsByIds(recentSeriesIds);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách series đã xem gần đây thành công!")
                .data(seriesCards)
                .build());
    }

    @GetMapping("/similar")
    @Operation(
            summary = "Lấy danh sách các series tương tự (Similar IDs)",
            description = "API này thực hiện lấy ra danh sách các tương tự của một series_id. " +
                    "Hệ thống kiểm tra cache Redis trước (Key dạng recommendation:series:{id}). " +
                    "Nếu không tồn tại hoặc hết hạn, hệ thống tìm kiếm trong MongoDB bộ sưu tập 'series_recommendations', " +
                    "sau đó tự động đồng bộ (rebuild) lại dữ liệu sang Redis với thời gian hết hạn 7 ngày để tối ưu hiệu năng."
    )
    public ResponseEntity<BaseResponse> getSimilarSeriesIds(
            @RequestParam("seriesId") String seriesId
    ) {
        List<String> similarSeriesIds = recommendationService.getSimilarSeriesIds(seriesId);

        List<SeriesCardResponseDto> similarSeries = seriesService.getSeriesCardsByIds(similarSeriesIds);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách series tương tự thành công!")
                .data(similarSeries)
                .build());
    }

    @PostMapping("/rank")
    @Operation(
            summary = "Xếp hạng tinh danh sách các series ứng viên bằng mô hình AI LightGBM",
            description = "API này nhận vào danh sách các seriesIds ứng viên (ví dụ: 100 sản phẩm thu được từ tích vô hướng ở tầng Retrieval), " +
                    "sau đó chuyển tiếp thông tin cùng accountId sang TaleX AI Service (Python) qua giao thức HTTP REST đồng bộ. " +
                    "Hệ thống Python sẽ trích xuất đặc trưng từ MongoDB, nạp vào mô hình LightGBM để tính điểm tương thích chi tiết, " +
                    "và trả về danh sách các ID đã sắp xếp theo thứ tự ưu tiên giảm dần để hiển thị trực tiếp lên UI."
    )
    public ResponseEntity<BaseResponse> rankSeriesIds(
            @RequestParam String accountId,
            @RequestBody List<String> seriesIds
    ) {
        List<RankResultItem> rankedItems = recommendationService.rankSeries(
                accountId == null ? "" : accountId,
                seriesIds
        );

        List<String> rankedSeriesIds = rankedItems.stream()
                .map(RankResultItem::getSeriesId)
                .toList();

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Xếp hạng danh sách series thành công!")
                .data(rankedSeriesIds)
                .build());
    }

    @GetMapping("/pools/recommendation")
    @Operation(
            summary = "Lấy danh sách Series kèm Score trong Recommendation Pool mới nhất (Phục vụ Demo)",
            description = "Truy vấn toàn bộ Series trong Recommendation Pool kèm theo thông tin ranking score (điểm số AI, 'another_channel', hoặc 'null')."
    )
    public ResponseEntity<BaseResponse> getLatestRecommendationPoolSeries(
            @RequestParam String accountId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "HOME") String pageType
    ) {
        if (accountId == null) {
            return ResponseEntity.ok(BaseResponse.builder()
                    .code(200)
                    .message("Người dùng chưa đăng nhập!")
                    .data(Collections.emptyList())
                    .build());
        }

        List<PoolSeriesCardResponseDto> result = recommendationService.getLatestRecommendationPoolSeries(
                accountId,
                sessionId,
                pageType
        );

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách Recommendation Pool kèm điểm thành công!")
                .data(result)
                .build());
    }

    @GetMapping("/pools/already-watched")
    @Operation(
            summary = "Lấy danh sách Series trong Already Watched Pool",
            description = "Truy vấn danh sách các Series đã từng xuất hiện/hiển thị cho người dùng gần đây từ Redis Set (Already Watched Pool)."
    )
    public ResponseEntity<BaseResponse> getAlreadyWatchedPoolSeries(
           @RequestParam String accountId
    ) {
        if (accountId == null) {
            return ResponseEntity.ok(BaseResponse.builder()
                    .code(200)
                    .message("Người dùng chưa đăng nhập!")
                    .data(Collections.emptyList())
                    .build());
        }

        List<SeriesCardResponseDto> result = recommendationService.getAlreadyWatchedPoolSeries(
                accountId
        );

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách Already Watched Pool thành công!")
                .data(result)
                .build());
    }
}