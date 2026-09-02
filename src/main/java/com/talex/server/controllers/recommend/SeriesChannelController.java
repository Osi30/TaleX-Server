package com.talex.server.controllers.recommend;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.recommend.response.SeriesCardResponseDto;
import com.talex.server.services.recommend.SeriesChannelService;
import com.talex.server.services.recommend.SeriesPoolService;
import com.talex.server.services.series.SeriesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
@Tag(
        name = "Kênh Đề Xuất (Recommendation Channels)",
        description = "APIs xử lý và cung cấp danh sách Series IDs cho từng kênh đệm"
)
public class SeriesChannelController {
    private final SeriesChannelService seriesChannelService;
    private final SeriesService seriesService;
    private final SeriesPoolService seriesPoolService;

    @PostMapping("/pool")
    @Operation(
            summary = "Kích hoạt tiến trình tạo pool cho các series",
            description = "Thực hiện tạo pool cho 6 kênh thuộc kênh chung của hệ thống thủ công"
    )
    public ResponseEntity<?> initGlobalPools() {
        seriesPoolService.rebuildAllGlobalPools();
        return ResponseEntity.ok("Khởi tạo thành công");
    }

    // --- Kênh: Promoted ---

    @GetMapping("/promoted/cards")
    @Operation(summary = "1. Kênh Promoted: Lấy toàn bộ danh sách Series Cards")
    public ResponseEntity<List<SeriesCardResponseDto>> getPromotedSeriesCards() {
        List<String> ids = seriesChannelService.getPromotedPoolElements();
        return ResponseEntity.ok(seriesService.getPromotedSeriesCardsByIds(ids));
    }

    @PostMapping("/promoted")
    @Operation(
            summary = "Kênh 1: Lấy danh sách Series IDs Quảng cáo / Tài trợ (Promoted)",
            description = "Truy vấn danh sách Candidate Series IDs từ Redis Pool dựa theo con trỏ Offset của từng Account. " +
                    "Tự động xoay vòng danh sách và tịnh tiến offset trên Redis."
    )
    public ResponseEntity<List<String>> getPromotedSeriesIds(
            @CurrentAccountId UUID accountId,
            @RequestParam(defaultValue = "3") int limit
    ) {
        if (accountId == null) return ResponseEntity.noContent().build();
        List<String> seriesIds = seriesChannelService.getPromotedSeriesIds(
                accountId.toString(), limit
        );
        return ResponseEntity.ok(seriesIds);
    }

    @PostMapping("/promoted/refresh")
    @Operation(
            summary = "Làm mới (Refresh) Redis Pool cho Kênh Quảng cáo",
            description = "Quét PostgreSQL lấy các Campaign Series RUNNING có impression thấp nhất. " +
                    "Ghép giữ nguyên thứ tự các ID cũ còn dư và chèn ID mới vào sau, đồng thời đồng bộ vào Redis Global IDs."
    )
    public ResponseEntity<List<String>> refreshPromotedPool(
            @RequestParam(defaultValue = "3") int limit
    ) {
        List<String> refreshedPool = seriesChannelService.refreshPromotedPool(limit);
        return ResponseEntity.ok(refreshedPool);
    }

    // --- Kênh: Mới ra mắt (New Releases) ---

    @GetMapping("/new-releases/cards")
    @Operation(summary = "2. Kênh Mới ra mắt: Lấy toàn bộ danh sách Series Cards")
    public ResponseEntity<List<SeriesCardResponseDto>> getNewReleasesSeriesCards() {
        List<String> ids = seriesChannelService.getNewReleasesPoolElements();
        return ResponseEntity.ok(seriesService.getSeriesCardsByIds(ids));
    }

    @PostMapping("/new-releases")
    @Operation(
            summary = "Kênh Mới ra mắt (New Releases): Lấy danh sách Series IDs",
            description = "Truy vấn danh sách Candidate Series IDs từ Redis Pool kênh Mới ra mắt dựa theo con trỏ Offset của Account."
    )
    public ResponseEntity<List<String>> getNewReleasesSeriesIds(
            @CurrentAccountId UUID accountId,
            @RequestParam(defaultValue = "3") int limit
    ) {
        if (accountId == null) return ResponseEntity.noContent().build();
        List<String> seriesIds = seriesChannelService.getNewReleasesSeriesIds(accountId.toString(), limit);
        return ResponseEntity.ok(seriesIds);
    }

    @PostMapping("/new-releases/refresh")
    @Operation(
            summary = "Làm mới (Refresh) Redis Pool cho Kênh Mới ra mắt",
            description = "Quét PostgreSQL lấy các Series công khai có totalImpression <= ngưỡng và releasedUpdateTime xa nhất. " +
                    "Truyền blacklistSeriesIds (ví dụ IDs lấy từ kênh Promoted) để né trùng lặp."
    )
    public ResponseEntity<List<String>> refreshNewReleasesPool(
            @RequestBody(required = false) List<String> blacklistSeriesIds,
            @RequestParam(defaultValue = "3") int limit
    ) {
        List<String> refreshedPool = seriesChannelService.refreshNewReleasesPool(blacklistSeriesIds, limit);
        return ResponseEntity.ok(refreshedPool);
    }

    // --- Kênh: Mới cập nhật (Recently Updated) ---

    @GetMapping("/recently-updated/cards")
    @Operation(summary = "3. Kênh Mới cập nhật: Lấy toàn bộ danh sách Series Cards")
    public ResponseEntity<List<SeriesCardResponseDto>> getRecentlyUpdatedSeriesCards() {
        List<String> ids = seriesChannelService.getRecentlyUpdatedPoolElements();
        return ResponseEntity.ok(seriesService.getSeriesCardsByIds(ids));
    }

    @PostMapping("/recently-updated")
    @Operation(
            summary = "Kênh Mới cập nhật (Recently Updated): Lấy danh sách Series IDs",
            description = "Truy vấn danh sách Candidate Series IDs từ Redis Pool kênh Mới cập nhật dựa theo con trỏ Offset của Account."
    )
    public ResponseEntity<List<String>> getRecentlyUpdatedSeriesIds(
            @CurrentAccountId UUID accountId,
            @RequestParam(defaultValue = "3") int limit
    ) {
        if (accountId == null) return ResponseEntity.noContent().build();
        List<String> seriesIds = seriesChannelService.getRecentlyUpdatedSeriesIds(accountId.toString(), limit);
        return ResponseEntity.ok(seriesIds);
    }

    @PostMapping("/recently-updated/refresh")
    @Operation(
            summary = "Làm mới (Refresh) Redis Pool cho Kênh Mới cập nhật",
            description = "Quét PostgreSQL lấy các Series công khai sắp xếp giảm dần theo thời gian cập nhật mới nhất (releasedUpdateTime DESC). " +
                    "Truyền blacklistSeriesIds (ví dụ IDs lấy từ kênh Promoted hoặc New Releases) để né trùng lặp."
    )
    public ResponseEntity<List<String>> refreshRecentlyUpdatedPool(
            @RequestBody(required = false) List<String> blacklistSeriesIds,
            @RequestParam(defaultValue = "3") int limit
    ) {
        List<String> refreshedPool = seriesChannelService.refreshRecentlyUpdatedPool(blacklistSeriesIds, limit);
        return ResponseEntity.ok(refreshedPool);
    }

    // --- Kênh: Cộng đồng bình chọn mới nhất (Latest Community Choice Hourly) ---

    @GetMapping("/latest-community-choice/cards")
    @Operation(summary = "4. Kênh Cộng đồng bình chọn mới nhất: Lấy toàn bộ danh sách Series Cards")
    public ResponseEntity<List<SeriesCardResponseDto>> getLatestCommunityChoiceSeriesCards() {
        List<String> ids = seriesChannelService.getLatestCommunityChoicePoolElements();
        return ResponseEntity.ok(seriesService.getSeriesCardsByIds(ids));
    }

    @PostMapping("/latest-community-choice")
    @Operation(
            summary = "Kênh Cộng đồng bình chọn mới nhất: Lấy danh sách Series IDs",
            description = "Truy vấn danh sách Candidate Series IDs từ Redis Pool kênh Cộng đồng bình chọn mới nhất dựa theo con trỏ Offset của Account."
    )
    public ResponseEntity<List<String>> getLatestCommunityChoiceSeriesIds(
            @CurrentAccountId UUID accountId,
            @RequestParam(defaultValue = "2") int limit
    ) {
        if (accountId == null) return ResponseEntity.noContent().build();
        List<String> seriesIds = seriesChannelService.getLatestCommunityChoiceSeriesIds(accountId.toString(), limit);
        return ResponseEntity.ok(seriesIds);
    }

    @PostMapping("/latest-community-choice/refresh")
    @Operation(
            summary = "Làm mới (Refresh) Redis Pool cho Kênh Cộng đồng bình chọn mới nhất",
            description = "Quét PostgreSQL lấy SeriesLog theo mốc hourBucket gần nhất (< đầu giờ hiện tại), " +
                    "sắp xếp giảm dần theo watchTime -> likes -> views. Truyền blacklistSeriesIds để loại trừ trùng lặp."
    )
    public ResponseEntity<List<String>> refreshLatestCommunityChoicePool(
            @RequestBody(required = false) List<String> blacklistSeriesIds,
            @RequestParam(defaultValue = "3") int limit
    ) {
        List<String> refreshedPool = seriesChannelService.refreshLatestCommunityChoicePool(blacklistSeriesIds, limit);
        return ResponseEntity.ok(refreshedPool);
    }

    // --- Kênh: Cộng đồng bình chọn All-time (Community Choice) ---

    @GetMapping("/community-choice/cards")
    @Operation(summary = "5. Kênh Cộng đồng bình chọn All-time: Lấy toàn bộ danh sách Series Cards")
    public ResponseEntity<List<SeriesCardResponseDto>> getCommunityChoiceSeriesCards() {
        List<String> ids = seriesChannelService.getCommunityChoicePoolElements();
        return ResponseEntity.ok(seriesService.getSeriesCardsByIds(ids));
    }

    @PostMapping("/community-choice")
    @Operation(
            summary = "Kênh Cộng đồng bình chọn All-time: Lấy danh sách Series IDs",
            description = "Truy vấn danh sách Candidate Series IDs từ Redis Pool kênh Cộng đồng bình chọn All-time dựa theo con trỏ Offset của Account."
    )
    public ResponseEntity<List<String>> getCommunityChoiceSeriesIds(
            @CurrentAccountId UUID accountId,
            @RequestParam(defaultValue = "3") int limit
    ) {
        if (accountId == null) return ResponseEntity.noContent().build();
        List<String> seriesIds = seriesChannelService.getCommunityChoiceSeriesIds(accountId.toString(), limit);
        return ResponseEntity.ok(seriesIds);
    }

    @PostMapping("/community-choice/refresh")
    @Operation(
            summary = "Làm mới (Refresh) Redis Pool cho Kênh Cộng đồng bình chọn All-time",
            description = "Quét PostgreSQL lấy danh sách Series công khai sắp xếp giảm dần theo chỉ số tích lũy All-time (watchTime -> likes -> views -> comments -> shares -> bookmarks). " +
                    "Truyền blacklistSeriesIds để loại trừ trùng lặp với các kênh trước đó."
    )
    public ResponseEntity<List<String>> refreshCommunityChoicePool(
            @RequestBody(required = false) List<String> blacklistSeriesIds,
            @RequestParam(defaultValue = "3") int limit
    ) {
        List<String> refreshedPool = seriesChannelService.refreshCommunityChoicePool(blacklistSeriesIds, limit);
        return ResponseEntity.ok(refreshedPool);
    }

    // --- Kênh: Thể loại ngẫu nhiên (Random Category) ---

    @GetMapping("/random-category/cards")
    @Operation(summary = "6. Kênh Thể loại ngẫu nhiên: Lấy toàn bộ danh sách Series Cards")
    public ResponseEntity<List<SeriesCardResponseDto>> getRandomCategorySeriesCards() {
        List<String> ids = seriesChannelService.getRandomCategoryPoolElements();
        return ResponseEntity.ok(seriesService.getSeriesCardsByIds(ids));
    }

    @PostMapping("/random-category")
    @Operation(
            summary = "Kênh Thể loại ngẫu nhiên: Lấy danh sách Series IDs",
            description = "Truy vấn danh sách Candidate Series IDs từ Redis Pool kênh Thể loại ngẫu nhiên dựa theo con trỏ Offset của Account."
    )
    public ResponseEntity<List<String>> getRandomCategorySeriesIds(
            @CurrentAccountId UUID accountId,
            @RequestParam(defaultValue = "3") int limit
    ) {
        if (accountId == null) return ResponseEntity.noContent().build();
        List<String> seriesIds = seriesChannelService.getRandomCategorySeriesIds(accountId.toString(), limit);
        return ResponseEntity.ok(seriesIds);
    }

    @PostMapping("/random-category/refresh")
    @Operation(
            summary = "Làm mới (Refresh) Redis Pool cho Kênh Thể loại ngẫu nhiên",
            description = "Quét PostgreSQL 1 lần duy nhất lấy Top N Series xuất sắc nhất cho từng Category active " +
                    "(sắp xếp theo watchTime -> likes -> views...), sau đó xáo trộn ngẫu nhiên và lưu vào Pool."
    )
    public ResponseEntity<List<String>> refreshRandomCategoryPool(
            @RequestBody(required = false) List<String> blacklistSeriesIds,
            @RequestParam(defaultValue = "3") int limitPerCategory,
            @RequestParam(defaultValue = "20") int totalLimit
    ) {
        List<String> refreshedPool = seriesChannelService.refreshRandomCategoryPool(blacklistSeriesIds, limitPerCategory, totalLimit);
        return ResponseEntity.ok(refreshedPool);
    }

    // --- Kênh: Tác giả đã đăng ký (Subscribed Creators) ---

    @GetMapping("/subscribed-creators/cards")
    @Operation(summary = "8. Kênh Tác giả đã đăng ký: Lấy toàn bộ danh sách Series Cards cá nhân hóa")
    public ResponseEntity<List<SeriesCardResponseDto>> getSubscribedCreatorsSeriesCards(
            @CurrentAccountId UUID accountId
    ) {
        if (accountId == null) return ResponseEntity.noContent().build();
        List<String> ids = seriesChannelService.getAllSubscribedCreatorsSeriesIds(accountId.toString());
        return ResponseEntity.ok(seriesService.getSeriesCardsByIds(ids));
    }

    @PostMapping("/subscribed-creators")
    @Operation(
            summary = "Kênh Tác giả đã đăng ký: Lấy danh sách Series IDs cá nhân hóa",
            description = "Truy vấn danh sách Candidate Series IDs từ Redis Pool cá nhân của Account dựa trên danh sách Creators mà người dùng đã follow."
    )
    public ResponseEntity<List<String>> getSubscribedCreatorsSeriesIds(
            @CurrentAccountId UUID accountId,
            @RequestParam(defaultValue = "3") int limit
    ) {
        if (accountId == null) return ResponseEntity.noContent().build();
        List<String> seriesIds = seriesChannelService.getSubscribedCreatorsSeriesIds(accountId.toString(), Collections.emptySet(), limit);
        return ResponseEntity.ok(seriesIds);
    }

    @PostMapping("/subscribed-creators/refresh")
    @Operation(
            summary = "Làm mới (Refresh) Redis Pool cho Kênh Tác giả đã đăng ký",
            description = "Quét PostgreSQL 1 lần duy nhất lấy Top N Series xuất sắc nhất từ các Creator mà Account đang theo dõi, " +
                    "sau đó xáo trộn và lưu vào Redis Pool cá nhân."
    )
    public ResponseEntity<List<String>> refreshSubscribedCreatorsPool(
            @CurrentAccountId UUID accountId,
            @RequestBody(required = false) Set<String> blacklistSeriesIds,
            @RequestParam(defaultValue = "3") int limitPerCreator,
            @RequestParam(defaultValue = "20") int totalLimit
    ) {
        if (accountId == null) return ResponseEntity.noContent().build();
        List<String> refreshedPool = seriesChannelService.refreshSubscribedCreatorsPool(
                accountId.toString(), blacklistSeriesIds, limitPerCreator, totalLimit
        );
        return ResponseEntity.ok(refreshedPool);
    }

    // --- Kênh: Trending ---

    @GetMapping("/trending/cards")
    @Operation(summary = "7. Kênh Trending: Lấy toàn bộ danh sách Series Cards")
    public ResponseEntity<List<SeriesCardResponseDto>> getTrendingSeriesCards() {
        List<String> ids = seriesChannelService.getTrendingPoolElements();
        return ResponseEntity.ok(seriesService.getSeriesCardsByIds(ids));
    }

    @PostMapping("/trending")
    @Operation(
            summary = "Kênh Trending: Lấy danh sách Series IDs",
            description = "Truy vấn danh sách Candidate Series IDs từ Redis Pool kênh Trending dựa theo con trỏ Offset của Account."
    )
    public ResponseEntity<List<String>> getTrendingSeriesIds(
            @CurrentAccountId UUID accountId,
            @RequestParam(defaultValue = "3") int limit
    ) {
        if (accountId == null) return ResponseEntity.noContent().build();
        List<String> seriesIds = seriesChannelService.getTrendingSeriesIds(accountId.toString(), limit);
        return ResponseEntity.ok(seriesIds);
    }

    @PostMapping("/trending/refresh")
    @Operation(
            summary = "Làm mới (Refresh) Redis Pool cho Kênh Trending",
            description = "Quét PostgreSQL lấy các Series công khai có impressionStatus là SUCCESS và rankingScore > 0, " +
                    "sắp xếp giảm dần theo rankingScore. Truyền blacklistSeriesIds để loại trừ trùng lặp."
    )
    public ResponseEntity<List<String>> refreshTrendingPool(
            @RequestBody(required = false) List<String> blacklistSeriesIds,
            @RequestParam(defaultValue = "3") int limit
    ) {
        List<String> refreshedPool = seriesChannelService.refreshTrendingPool(blacklistSeriesIds, limit);
        return ResponseEntity.ok(refreshedPool);
    }

    // --- Kênh: Sở thích Onboarding (Onboarding Preferences) ---

    @GetMapping("/onboarding-preferences")
    @Operation(
            summary = "Kênh Sở thích Onboarding: Lấy danh sách Series IDs cá nhân hóa",
            description = "Truy vấn danh sách Candidate Series IDs từ Redis Pool cá nhân dựa theo Onboarding Genres & Tags thu thập khi khảo sát người dùng. " +
                    "Sử dụng con trỏ offset tự động xoay vòng trên Redis."
    )
    public ResponseEntity<List<String>> getOnboardingPreferencesSeriesIds(
            @CurrentAccountId UUID accountId,
            @RequestParam(defaultValue = "3") int limit
    ) {
        if (accountId == null) return ResponseEntity.noContent().build();
        List<String> seriesIds = seriesChannelService.getOnboardingPreferencesSeriesIds(
                accountId.toString(), Collections.emptySet(), limit
        );
        return ResponseEntity.ok(seriesIds);
    }

    @GetMapping("/dynamic-preferences")
    @Operation(
            summary = "Kênh Sở thích Động: Lấy danh sách Series IDs cá nhân hóa theo tương tác",
            description = "Lấy dữ liệu thói quen người dùng (Top Categories & Tags thời gian xem cao nhất từ MongoDB), " +
                    "sau đó truy vấn danh sách Series tương ứng từ PostgreSQL có hỗ trợ Blacklist."
    )
    public ResponseEntity<List<String>> getDynamicPreferencesSeriesIds(
            @CurrentAccountId UUID accountId,
            @RequestBody(required = false) Set<String> blacklistSeriesIds,
            @RequestParam(defaultValue = "10") int totalLimit
    ) {
        if (accountId == null) return ResponseEntity.noContent().build();
        List<String> seriesIds = seriesChannelService.getDynamicPreferencesSeriesIds(
                accountId.toString(), blacklistSeriesIds, totalLimit
        );
        return ResponseEntity.ok(seriesIds);
    }

    // --- Global IDs ---

    @GetMapping("/global-ids")
    @Operation(
            summary = "Lấy toàn bộ Global IDs chống trùng lặp của tất cả các kênh",
            description = "Thực hiện đúng 1 query (HVALS) lên Redis để lấy tập hợp tất cả IDs đang xuất hiện ở các kênh Pool."
    )
    public ResponseEntity<Set<String>> getAllGlobalIds() {
        Set<String> globalIds = seriesChannelService.getAllGlobalIds();
        return ResponseEntity.ok(globalIds);
    }
}
