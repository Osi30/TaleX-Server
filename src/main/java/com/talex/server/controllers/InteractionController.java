package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.interaction.InteractionRequest;
import com.talex.server.dtos.requests.interaction.WatchTimeRequest;
import com.talex.server.services.IInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
public class InteractionController {
    private final IInteractionService interactionService;

    @PostMapping()
    public ResponseEntity<BaseResponse> userInteractContent(
            @CurrentAccountId UUID accountId,
            @RequestBody InteractionRequest request
    ) {
        interactionService.processInteraction(accountId, request);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Success")
                .data("Tương tác thành công!")
                .build());
    }

    @PostMapping("/telemetry")
    public ResponseEntity<BaseResponse> recordWatchTime(
            @CurrentAccountId UUID accountId,
            @RequestBody WatchTimeRequest request
    ) {
        interactionService.processTelemetry(accountId, request);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Success")
                .data("Thu thập thành công!")
                .build());
    }

    @PostMapping("test/interact")
    public ResponseEntity<String> interact(
            @CurrentAccountId UUID accountId,
            @RequestBody InteractionRequest request
    ) {
        interactionService.handleInteraction(accountId, request);
        return ResponseEntity.ok("Xử lý tương tác thành công!");
    }
}
