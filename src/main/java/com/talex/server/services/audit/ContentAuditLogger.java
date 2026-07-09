package com.talex.server.services.audit;

import io.questdb.client.Sender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentAuditLogger {

    private final Sender questDBSender;

    /**
     * Logs content related actions to QuestDB
     * @param entityName the name of the entity (e.g. Series, Season, Episode)
     * @param entityId the ID of the entity
     * @param action the action performed (e.g. CREATE, UPDATE, DELETE, PUBLISH)
     * @param accountId the user who performed the action
     * @param creatorId the creator owning the content
     */
    @Async
    public void logAction(String entityName, String entityId, String action, String accountId, String creatorId) {
        try {
            synchronized (questDBSender) {
                questDBSender.table("content_audit_logs")
                        .symbol("entity_name", entityName)
                        .symbol("action", action)
                        .symbol("account_id", accountId)
                        .symbol("creator_id", creatorId)
                        .stringColumn("entity_id", entityId)
                        .at(Instant.now());
            }
        } catch (Exception e) {
            log.warn("[QuestDB] Failed to save audit log for {} {} action {}: {}", entityName, entityId, action, e.getMessage());
        }
    }
}
