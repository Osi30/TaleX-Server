package com.talex.server.controllers.interaction;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.services.interaction.IEpisodeViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Episode Views", description = "API ghi nhận và thống kê số lượt xem tập phim từ IP người dùng")
public class EpisodeViewController {
    private final IEpisodeViewService episodeViewService;

    @Operation(
            summary = "Ghi nhận lượt xem Tập phim",
            description = "Hệ thống tự động trích xuất IP Address của client để đẩy vào hàng đợi Kafka xử lý bất đồng bộ."
    )
    @PostMapping("/episodes/{episodeId}/views")
    public ResponseEntity<BaseResponse> viewEpisode(
            @PathVariable String episodeId,
            HttpServletRequest request
    ) {
        // Trích xuất IP Address an toàn qua các tầng Proxy
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null
                || ipAddress.isEmpty()
                || "unknown".equalsIgnoreCase(ipAddress)
        ) {
            ipAddress = request.getRemoteAddr();
        }

        // Trường hợp chuỗi IP chứa nhiều IP phân tách bằng dấu phẩy (qua nhiều proxy), lấy cái đầu tiên
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        episodeViewService.viewEpisode(ipAddress, episodeId);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Ghi nhận lượt xem thành công.")
                .build());
    }
}