package com.talex.server.controllers.recommend;

import com.talex.server.services.recommend.SeriesChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
@Tag(
        name = "Kênh Đề Xuất (Recommendation Channels)",
        description = "APIs xử lý và cung cấp danh sách Series IDs cho từng kênh đệm"
)
public class SeriesChannelController {
    private final SeriesChannelService seriesChannelService;

    @PostMapping("/promoted")
    @Operation(
            summary = "Kênh 1: Lấy danh sách Series IDs Quảng cáo / Tài trợ (Promoted)",
            description = "Truy vấn danh sách Candidate Series IDs từ Redis Pool cho kênh Quảng cáo. " +
                    "Tự động lọc các IDs nằm trong Blacklist và kích hoạt Fallback query PostgreSQL nếu Redis pool bị cạn."
    )
    public ResponseEntity<List<String>> getPromotedSeriesIds(
            @RequestParam(defaultValue = "2") int limit,
            @RequestBody(required = false) List<String> blacklistSeriesIds
    ) {
        List<String> seriesIds = seriesChannelService.getPromotedSeriesIds(blacklistSeriesIds, limit);
        return ResponseEntity.ok(seriesIds);
    }

    @PostMapping("/promoted/refresh")
    @Operation(
            summary = "Làm mới (Refresh) Redis Pool cho Kênh Quảng cáo",
            description = "Dùng cho Cronjob định kỳ hoặc Trigger thủ công. Quét PostgreSQL lấy các Campaign Series có status = RUNNING " +
                    "và totalImpression thấp nhất, sau đó chèn vào đầu Redis Pool cũ mà không làm xáo trộn thứ tự các item cũ."
    )
    public ResponseEntity<List<String>> refreshPromotedPool(
            @RequestParam(defaultValue = "2") int limit
    ) {
        List<String> refreshedPool = seriesChannelService.refreshPromotedPool(limit);
        return ResponseEntity.ok(refreshedPool);
    }
}
