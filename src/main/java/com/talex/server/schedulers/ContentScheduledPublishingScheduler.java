package com.talex.server.schedulers;

import com.talex.server.entities.Episode;
import com.talex.server.enums.EpisodeStatus;
import com.talex.server.repositories.EpisodeRepository;
import com.talex.server.services.EpisodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentScheduledPublishingScheduler {
    private static final String ACTOR_ID = "system:scheduled-publisher";
    private static final List<EpisodeStatus> SCHEDULED_EPISODE_STATUSES =
            List.of(EpisodeStatus.DRAFT, EpisodeStatus.HIDDEN);

    private final EpisodeRepository episodeRepository;
    private final EpisodeService episodeService;

    @Scheduled(
            fixedDelayString = "${content.scheduling.publish-fixed-delay-ms:60000}",
            initialDelayString = "${content.scheduling.publish-initial-delay-ms:60000}")
    public void publishDueContent() {
        LocalDateTime now = LocalDateTime.now();
        runBatch("episodes", () -> publishDueEpisodes(now));
    }

    private void runBatch(String contentType, Runnable publisher) {
        try {
            publisher.run();
        } catch (RuntimeException exception) {
            log.warn("Scheduled publishing batch failed for {}: {}", contentType, exception.getMessage());
            log.debug("Scheduled publishing batch failure detail for {}", contentType, exception);
        }
    }

    private void publishDueEpisodes(LocalDateTime now) {
        List<Episode> dueEpisodes = episodeRepository
                .findTop100ByScheduledPublishAtLessThanEqualAndStatusInAndIsDeletedFalseOrderByScheduledPublishAtAsc(
                        now,
                        SCHEDULED_EPISODE_STATUSES);
        for (Episode episode : dueEpisodes) {
            try {
                episodeService.publishScheduled(episode.getEpisodeId(), ACTOR_ID);
            } catch (RuntimeException exception) {
                log.warn("Failed to publish scheduled episode {}", episode.getEpisodeId(), exception);
            }
        }
    }
}
