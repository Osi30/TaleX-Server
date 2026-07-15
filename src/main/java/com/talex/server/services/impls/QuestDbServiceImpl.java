package com.talex.server.services.impls;

import com.talex.server.dtos.mongo.QuestDbPreferenceResult;
import com.talex.server.dtos.mongo.QuestDbQueryResult;
import com.talex.server.services.QuestDbService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class QuestDbServiceImpl implements QuestDbService {
    private final JdbcTemplate questDbTemplate;

    public QuestDbServiceImpl(
            @Qualifier("questDbJdbcTemplate") JdbcTemplate questDbTemplate
    ) {
        this.questDbTemplate = questDbTemplate;
    }

    private static final String MASTER_QUERY =
            "WITH combined_events AS ( " +
                    "    SELECT account_id, 'CLICK' as event_type, delta as weight FROM view_logs WHERE timestamp > ? AND timestamp <= ? AND account_id IS NOT NULL AND account_id NOT IN ('anonymous', '') " +
                    "    UNION ALL " +
                    "    SELECT account_id, 'LIKE' as event_type, CASE WHEN action_type = 'LIKE' THEN 1 ELSE -1 END as weight FROM like_logs WHERE timestamp > ? AND timestamp <= ? AND account_id IS NOT NULL AND account_id NOT IN ('anonymous', '') " +
                    "    UNION ALL " +
                    "    SELECT account_id, 'BOOKMARK' as event_type, CASE WHEN action_type = 'BOOKMARK' THEN 1 ELSE -1 END as weight FROM bookmark_logs WHERE timestamp > ? AND timestamp <= ? AND account_id IS NOT NULL AND account_id NOT IN ('anonymous', '') " +
                    "    UNION ALL " +
                    "    SELECT account_id, 'SHARE' as event_type, 1 as weight FROM share_logs WHERE timestamp > ? AND timestamp <= ? AND account_id IS NOT NULL AND account_id NOT IN ('anonymous', '') " +
                    "    UNION ALL " +
                    "    SELECT account_id, 'COMMENT' as event_type, CASE WHEN action_type = 'CREATE' THEN 1 ELSE -1 END as weight FROM comment_logs WHERE timestamp > ? AND timestamp <= ? AND account_id IS NOT NULL AND account_id NOT IN ('anonymous', '') " +
                    "    UNION ALL " +
                    "    SELECT account_id, 'WATCH_TIME' as event_type, watch_time as weight FROM watch_session_logs WHERE timestamp > ? AND timestamp <= ? AND account_id IS NOT NULL AND account_id NOT IN ('anonymous', '') " +
                    ") " +
                    "SELECT account_id, " +
                    "       sum(CASE WHEN event_type = 'CLICK' THEN weight ELSE 0 END) as total_clicks, " +
                    "       sum(CASE WHEN event_type = 'LIKE' THEN weight ELSE 0 END) as total_likes, " +
                    "       sum(CASE WHEN event_type = 'BOOKMARK' THEN weight ELSE 0 END) as total_bookmarks, " +
                    "       sum(CASE WHEN event_type = 'SHARE' THEN weight ELSE 0 END) as total_shares, " +
                    "       sum(CASE WHEN event_type = 'COMMENT' THEN weight ELSE 0 END) as total_comments, " +
                    "       sum(CASE WHEN event_type = 'WATCH_TIME' THEN weight ELSE 0 END) as period_watch_time " +
                    "FROM combined_events " +
                    "GROUP BY account_id";

    private static final String PREFERENCE_QUERY =
            "WITH combined AS ( " +
                    "    SELECT account_id, episode_id, delta as clicks, 0L as watch_time " +
                    "    FROM view_logs " +
                    "    WHERE timestamp > ? AND timestamp <= ? " +
                    "      AND account_id IS NOT NULL AND account_id NOT IN ('anonymous', '') " +
                    "    UNION ALL " +
                    "    SELECT account_id, episode_id, 0L as clicks, watch_time " +
                    "    FROM watch_session_logs " +
                    "    WHERE timestamp > ? AND timestamp <= ? " +
                    "      AND account_id IS NOT NULL AND account_id NOT IN ('anonymous', '') " +
                    ") " +
                    "SELECT account_id, episode_id, sum(clicks) as total_clicks, sum(watch_time) as total_watch_time " +
                    "FROM combined " +
                    "GROUP BY account_id, episode_id";

    @Async("questDbExecutor")
    @Override
    public CompletableFuture<List<QuestDbQueryResult>> queryInteractionsAsync(Instant startTime, Instant endTime) {
        String startStr = startTime.toString();
        String endStr = endTime.toString();

        Object[] params = new Object[] {
                startStr, endStr,
                startStr, endStr,
                startStr, endStr,
                startStr, endStr,
                startStr, endStr,
                startStr, endStr
        };

        List<QuestDbQueryResult> results = questDbTemplate.query(
                MASTER_QUERY,
                (rs, rowNum) -> new QuestDbQueryResult(
                        rs.getString("account_id"),
                        rs.getLong("total_clicks"),
                        rs.getLong("total_likes"),
                        rs.getString("total_bookmarks") != null ? rs.getLong("total_bookmarks") : 0L,
                        rs.getString("total_shares") != null ? rs.getLong("total_shares") : 0L,
                        rs.getString("total_comments") != null ? rs.getLong("total_comments") : 0L,
                        rs.getObject("period_watch_time") != null ? rs.getDouble("period_watch_time") : 0D
                ),
                params
        );

        return CompletableFuture.completedFuture(results);
    }

    @Async("questDbExecutor")
    @Override
    public CompletableFuture<List<QuestDbPreferenceResult>> queryPreferencesAsync(Instant startTime, Instant endTime) {
        String startStr = startTime.toString();
        String endStr = endTime.toString();

        Object[] params = new Object[] {
                startStr, endStr,
                startStr, endStr
        };

        List<QuestDbPreferenceResult> results = questDbTemplate.query(
                PREFERENCE_QUERY,
                (rs, rowNum) -> new QuestDbPreferenceResult(
                        rs.getString("account_id"),
                        rs.getString("episode_id"),
                        rs.getLong("total_clicks"),
                        rs.getDouble("total_watch_time")
                ),
                params
        );

        return CompletableFuture.completedFuture(results);
    }
}
