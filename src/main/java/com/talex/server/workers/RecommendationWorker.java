package com.talex.server.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.kafka.RecommendationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationWorker {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String RECOMMENDATION_PREFIX = "recommendation:series:";

    @KafkaListener(topics = "recommendation-result", groupId = "recommendation-worker-group")
    public void processRecommendationResult(String message) {
        try {
            RecommendationResult result = objectMapper.readValue(message, RecommendationResult.class);
            log.info("Received recommendation result for seriesId={}", result.getSeriesId());

            if (result.getSeriesId() != null && result.getSimilarIds() != null && !result.getSimilarIds().isEmpty()) {
                String redisKey = RECOMMENDATION_PREFIX + result.getSeriesId();
                
                // Store in Redis (List or Value). Here we store as a JSON value or string list.
                String similarIdsJson = objectMapper.writeValueAsString(result.getSimilarIds());
                redisTemplate.opsForValue().set(redisKey, similarIdsJson, Duration.ofDays(7));
                
                log.info("Successfully stored {} similar series for seriesId={} in Redis", result.getSimilarIds().size(), result.getSeriesId());
            }

        } catch (Exception e) {
            log.error("Failed to process recommendation result: {}", e.getMessage(), e);
        }
    }
}
