package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.services.SseNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * SSE endpoint for real-time pipeline status notifications.
 * Clients connect once; server pushes events as pipeline steps complete.
 */
@RestController
@RequestMapping("/api/v1/sse")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SseController {

    private final SseNotificationService sseNotificationService;

    @GetMapping(value = "/pipeline/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectPipeline(@CurrentAccountId UUID accountId) {
        return sseNotificationService.connect(accountId.toString());
    }
}
