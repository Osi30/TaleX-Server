package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.interaction.InteractionRequest;
import com.talex.server.dtos.requests.interaction.WatchTimeRequest;
import com.talex.server.schedulers.InteractionDataSyncScheduler;
import com.talex.server.services.IInteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
@Tag(name = "Tương tác người dùng", description = "API ghi nhận tương tác nội dung và telemetry xem video của người dùng")
public class InteractionController {
        private final IInteractionService interactionService;
        private final InteractionDataSyncScheduler dataSyncScheduler;

        @PreAuthorize("isAuthenticated()")
        @PostMapping()
        @Operation(summary = "Ghi nhận tương tác nội dung", description = "Ghi nhận tương tác của người dùng với nội dung trên nền tảng.")
        public ResponseEntity<BaseResponse> userInteractContent(
                        @CurrentAccountId UUID accountId,
                        @RequestBody InteractionRequest request) {
                interactionService.processInteraction(accountId, request);
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(200)
                                .message("Success")
                                .data("Tương tác thành công!")
                                .build());
        }

        @PostMapping("/telemetry")
        @Operation(summary = "Ghi nhận telemetry xem video", description = "Ghi nhận thời gian xem và dữ liệu telemetry của người dùng.")
        public ResponseEntity<BaseResponse> recordWatchTime(
                        @CurrentAccountId UUID accountId,
                        @RequestBody WatchTimeRequest request) {
                interactionService.processTelemetry(accountId, request);
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(200)
                                .message("Success")
                                .data("Thu thập thành công!")
                                .build());
        }

        @PostMapping("test/interact")
        @Operation(summary = "Xử lý tương tác thử nghiệm", description = "Endpoint thử nghiệm để xử lý tương tác trực tiếp, dùng cho test nội bộ.")
        public ResponseEntity<String> interact(
                        @CurrentAccountId UUID accountId,
                        @RequestBody InteractionRequest request) {
                interactionService.handleInteraction(accountId, request);
                return ResponseEntity.ok("Xử lý tương tác thành công!");
        }

        @GetMapping
        public ResponseEntity<String> interact( ){
//                InteractionDataSyncScheduler syncScheduler = new InteractionDataSyncScheduler();
                dataSyncScheduler.performSyncFlow();
                return ResponseEntity.ok("Xử lý tương tác thành công!");
        }
}
