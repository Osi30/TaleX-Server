package com.talex.server.controllers.coin;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.coin.CompleteAdRequestDto;
import com.talex.server.dtos.requests.coin.StartAdRequestDto;
import com.talex.server.dtos.responses.coin.AdSessionResponseDto;
import com.talex.server.services.coin.IMissionAdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/missions/ads")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "User - Mission Ads", description = "Endpoints for watching ads to complete missions and earn rewards")
public class MissionAdController {

    private final IMissionAdService missionAdService;

    @PostMapping("/start")
    @Operation(
            summary = "Start an ad session",
            description = "Initializes a secure session for watching an ad. Returns a session ID valid for 90 seconds."
    )
    public ResponseEntity<BaseResponse> startAdSession(
            @Parameter(hidden = true) @CurrentAccountId UUID accountId,
            @Valid @RequestBody StartAdRequestDto request
    ) {
        AdSessionResponseDto session = missionAdService.startAdSession(
                accountId,
                request.getMissionCode()
        );

        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Ad session started successfully")
                        .data(session)
                        .build()
        );
    }

    @PostMapping("/complete")
    @Operation(
            summary = "Complete an ad session",
            description = "Validates the ad session and awards mission progress if watched for at least 10 seconds."
    )
    public ResponseEntity<BaseResponse> completeAdSession(
            @Parameter(hidden = true) @CurrentAccountId UUID accountId,
            @Valid @RequestBody CompleteAdRequestDto request
    ) {
        missionAdService.completeAdSession(accountId, request.getSessionId());

        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Ad session completed successfully")
                        .data(null)
                        .build()
        );
    }
}
