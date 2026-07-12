package com.talex.server.controllers.interaction;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.interaction.request.ViewRequest;
import com.talex.server.services.interaction.IEpisodeViewService;
import com.talex.server.utils.RequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
            @Valid @RequestBody ViewRequest viewRequest,
            @CurrentAccountId UUID accountId,
            HttpServletRequest request
    ) {
        // Trích xuất IP Address an toàn qua các tầng Proxy
        String ipAddress = RequestUtils.getIpAddress(request);
        viewRequest.setIpAddress(ipAddress);
        viewRequest.setAccountId(accountId);

        episodeViewService.viewEpisode(viewRequest);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Ghi nhận lượt xem thành công.")
                .build());
    }
}