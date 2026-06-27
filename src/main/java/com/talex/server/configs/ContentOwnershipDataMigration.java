package com.talex.server.configs;

import com.talex.server.repositories.EpisodeRepository;
import com.talex.server.repositories.SeasonRepository;
import com.talex.server.repositories.SeriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
public class ContentOwnershipDataMigration implements CommandLineRunner {
    private final SeriesRepository seriesRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        int migratedSeries = seriesRepository.migrateAccountIdsToCreatorIds();
        int synchronizedSeasons = seasonRepository.synchronizeCreatorIdsFromSeries();
        int synchronizedEpisodes = episodeRepository.synchronizeCreatorIdsFromSeries();

        if (migratedSeries > 0 || synchronizedSeasons > 0 || synchronizedEpisodes > 0) {
            log.info(
                    "Synchronized content ownership: {} series, {} seasons and {} episodes",
                    migratedSeries,
                    synchronizedSeasons,
                    synchronizedEpisodes);
        }
    }
}