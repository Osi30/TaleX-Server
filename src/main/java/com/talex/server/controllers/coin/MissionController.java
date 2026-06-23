package com.talex.server.controllers.coin;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.responses.coin.MissionProgressResponseDto;
import com.talex.server.services.coin.IMissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "User - Missions", description = "API quản lý tiến độ và nhiệm vụ cho người dùng")
public class MissionController {

    private final IMissionService missionService;

    @GetMapping
    @Operation(
            summary = "Lấy danh sách nhiệm vụ hôm nay",
            description = "Trả về danh sách các nhiệm vụ đang hoạt động kèm theo tiến độ hiện tại của user trong ngày."
    )
    public ResponseEntity<BaseResponse> getMyDailyMissions(
            @Parameter(hidden = true) @CurrentAccountId UUID accountId
    ) {
        List<MissionProgressResponseDto> missions =
                missionService.getMyDailyMissions(accountId);

        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Lấy danh sách nhiệm vụ hằng ngày thành công")
                        .data(missions)
                        .build()
        );
    }

    @PostMapping("/heartbeat")
    @Operation(
            summary = "Ghi nhận thời gian Online",
            description = "Ping định kỳ (ví dụ 1 phút/lần) để tăng tiến độ nhiệm vụ trực tuyến. Dữ liệu được đệm qua Redis."
    )
    public ResponseEntity<BaseResponse> processOnlineHeartbeat(
            @Parameter(hidden = true) @CurrentAccountId UUID accountId
    ) {
        missionService.processOnlineHeartbeat(accountId);

        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Ghi nhận thời gian online thành công")
                        .data(null)
                        .build()
        );
    }
}
