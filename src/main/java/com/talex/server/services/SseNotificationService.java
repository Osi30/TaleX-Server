package com.talex.server.services;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Manages SSE connections and pushes pipeline event notifications to creators.
 */
public interface SseNotificationService {
    SseEmitter connect(String accountId);
    void pushEvent(String accountId, String eventName, Object payload);
    void disconnect(String accountId);
}
