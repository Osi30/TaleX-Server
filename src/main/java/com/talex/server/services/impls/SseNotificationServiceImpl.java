package com.talex.server.services.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.services.SseNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE emitter registry keyed by accountId.
 * Latest connection wins — previous emitter is completed and replaced.
 * Cleans up stale emitters via heartbeat every 30s.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SseNotificationServiceImpl implements SseNotificationService {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Override
    public SseEmitter connect(String accountId) {
        // Close existing connection for this user (latest wins)
        SseEmitter existing = emitters.remove(accountId);
        if (existing != null) {
            existing.complete();
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // Triple cleanup: completion, timeout, and error all remove from registry
        emitter.onCompletion(() -> emitters.remove(accountId));
        emitter.onTimeout(() -> emitters.remove(accountId));
        emitter.onError(e -> emitters.remove(accountId));

        emitters.put(accountId, emitter);
        log.info("SSE connected: accountId={}, total={}", accountId, emitters.size());

        // Send initial connection confirmation event
        pushEvent(accountId, "connected", Map.of("message", "Pipeline notifications active"));

        return emitter;
    }

    @Override
    public void pushEvent(String accountId, String eventName, Object payload) {
        SseEmitter emitter = emitters.get(accountId);
        if (emitter == null) {
            log.debug("No SSE connection for accountId={}, skipping event={}", accountId, eventName);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(json));
        } catch (Exception e) {
            log.warn("SSE send failed: accountId={}, event={}, removing emitter", accountId, eventName);
            emitters.remove(accountId);
        }
    }

    @Override
    public void disconnect(String accountId) {
        SseEmitter emitter = emitters.remove(accountId);
        if (emitter != null) {
            emitter.complete();
        }
    }

    /**
     * Heartbeat every 30s to keep connections alive through proxies.
     * Also prunes any emitters that have silently gone stale.
     */
    @Scheduled(fixedDelay = 30_000)
    public void heartbeat() {
        emitters.forEach((accountId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("{}"));
            } catch (Exception e) {
                log.debug("Heartbeat failed for accountId={}, removing stale emitter", accountId);
                emitters.remove(accountId);
            }
        });
    }
}
