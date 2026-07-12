package com.talex.server.controllers.interaction;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.interaction.request.WatchTimeRequest;
import com.talex.server.schedulers.InteractionDataSyncScheduler;
import com.talex.server.services.interaction.IInteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
@Tag(name = "Interactions", description = "API ghi nhận tương tác nội dung và telemetry xem video của người dùng")
public class InteractionController {
        private final IInteractionService interactionService;
        private final InteractionDataSyncScheduler dataSyncScheduler;

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

        @GetMapping
        public ResponseEntity<String> interact( ){
//                InteractionDataSyncScheduler syncScheduler = new InteractionDataSyncScheduler();
                dataSyncScheduler.performSyncFlow();
                return ResponseEntity.ok("Xử lý tương tác thành công!");
        }
}
