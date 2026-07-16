package com.talex.server.controllers.mongo;

import com.talex.server.dtos.responses.series.EpisodeRefs;
import com.talex.server.services.mongo.ISeriesFeatureService;
import com.talex.server.services.series.EpisodeService;
import com.talex.server.services.mongo.IUserFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/feature")
@RequiredArgsConstructor
@Tag(name = "Feature Test", description = "Các API demo lưu trữ dữ liệu document cho mongoDB")
public class FeatureTestController {
    private final IUserFeatureService userFeatureService;
    private final ISeriesFeatureService seriesFeatureService;
    private final EpisodeService episodeService;

    @GetMapping("/{episodeId}/refs")
    @Operation(summary = "Api test lấy các tags và categories của một series", description = "Api test lấy các tags và categories của một series")
    public ResponseEntity<EpisodeRefs> getEpisodeRefs(@PathVariable String episodeId) {
        return ResponseEntity.ok(episodeService.getEpisodeRefsByEpisodeId(episodeId));
    }

    @PostMapping("/user/sync-dynamic")
    @Operation(summary = "Lưu trữ dữ liệu động của người dùng", description = "Kích hoạt lưu trữ dữ liệu động của người dùng")
    public ResponseEntity<String> triggerUserFeatureSync() {
        userFeatureService.syncUserDynamicFeatures();
        return ResponseEntity.ok("Đã kích hoạt và hoàn tất đồng bộ hóa Dynamic Features thành công!");
    }

    @PostMapping("/user/sync-preferences")
    @Operation(summary = "Đồng bộ sở thích động (Genres & Tags)", description = "Kích hoạt thủ công tiến trình đồng bộ Dynamic Preferences từ QuestDB sang MongoDB")
    public ResponseEntity<String> triggerUserPreferenceSync() {
        userFeatureService.syncUserDynamicPreferences();
        return ResponseEntity.ok("Đã kích hoạt và hoàn tất đồng bộ hóa Dynamic Preferences thành công!");
    }

    @PostMapping("/user/sync-monetization")
    @Operation(summary = "Lưu trữ dữ liệu tài chính của người dùng", description = "Kích hoạt lưu trữ dữ liệu Monetization từ Postgres sang Mongo")
    public ResponseEntity<String> triggerUserMonetizationSync() {
        userFeatureService.syncUserMonetizationFeatures();
        return ResponseEntity.ok("Đã kích hoạt và hoàn tất đồng bộ hóa Monetization Features thành công!");
    }

    @PostMapping("/series/stats/trigger")
    @Operation(
            summary = "Kích hoạt đồng bộ hóa thủ công",
            description = "Kích hoạt đồng thời 3 luồng đồng bộ: Lũy kế (Cumulative), Trending 24h và Trending 7d từ PostgreSQL sang MongoDB."
    )
    public ResponseEntity<String> triggerSync() {
        seriesFeatureService.syncAllSeriesFeatures();
        return ResponseEntity.ok("Đã kích hoạt và hoàn tất đồng bộ hóa toàn bộ Series Stats thành công!");
    }

    @PostMapping("/series/stats/reset")
    @Operation(
            summary = "Kích hoạt clean up",
            description = "Kích hoạt clean up dữ liệu 24h và 7d của nhiều series"
    )
    public ResponseEntity<String> cleanSync(
            @RequestParam() String[] seriesIds,
            @RequestParam Boolean is24h,
            @RequestParam Boolean is7d
    ) {
        seriesFeatureService.resetInactiveSeriesStatsInMongo(
                Arrays.stream(seriesIds).toList(), is24h, is7d);
        return ResponseEntity.ok("Đã kích hoạt và hoàn tất dọn dẹp toàn bộ Series Stats thành công!");
    }
}
