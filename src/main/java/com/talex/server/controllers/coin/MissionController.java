package com.talex.server.controllers.coin;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.responses.coin.MissionProgressResponseDto;
import com.talex.server.services.coin.IMissionService;
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
public class MissionController {

    private final IMissionService missionService;

    @GetMapping
    public ResponseEntity<BaseResponse> getMyDailyMissions(
            @CurrentAccountId UUID accountId
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
    public ResponseEntity<BaseResponse> processOnlineHeartbeat(
            @CurrentAccountId UUID accountId
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
