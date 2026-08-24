package com.talex.server.services.mongo.impls;

import com.talex.server.entities.config.SyncMetadata;
import com.talex.server.entities.mongo.SeriesMetadata;
import com.talex.server.entities.series.Category;
import com.talex.server.entities.series.Series;
import com.talex.server.entities.series.Tag;
import com.talex.server.enums.SyncType;
import com.talex.server.enums.series.ContentType;
import com.talex.server.exceptions.details.MongoDocumentException;
import com.talex.server.records.SeriesLogData;
import com.talex.server.repositories.config.SyncMetadataRepository;
import com.talex.server.repositories.mongo.SeriesMetadataRepository;
import com.talex.server.repositories.series.SeriesLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeriesFeatureServiceImpl Tests")
class SeriesFeatureServiceImplTest {

    @Mock
    private SeriesMetadataRepository seriesMetadataRepository;
    @Mock
    private SeriesLogRepository seriesLogRepository;
    @Mock
    private SyncMetadataRepository syncMetadataRepository;
    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private SeriesFeatureServiceImpl service;

    private Series sampleSeries;
    private SeriesMetadata sampleMetadata;

    @BeforeEach
    void setUp() {
        sampleSeries = new Series();
        sampleSeries.setSeriesId("series-1");
        sampleSeries.setTitle("Title 1");
        sampleSeries.setDescription("Desc 1");
        sampleSeries.setContentType(ContentType.VIDEO);

        sampleMetadata = SeriesMetadata.builder()
                .id("series-1")
                .title("Title 1")
                .build();
    }

    // =========================================================================
    // 1. getSeriesFeatureById
    // =========================================================================

    @Test
    @DisplayName("getSeriesFeatureById - Null or blank ID returns null vs valid ID lookup")
    void getSeriesFeatureById() {
        assertThat(service.getSeriesFeatureById(null)).isNull();
        assertThat(service.getSeriesFeatureById("  ")).isNull();

        when(seriesMetadataRepository.findById("series-1")).thenReturn(Optional.of(sampleMetadata));
        assertThat(service.getSeriesFeatureById(" series-1 ")).isEqualTo(sampleMetadata);
    }

    // =========================================================================
    // 2. saveSeriesMetadata
    // =========================================================================

    @Test
    @DisplayName("saveSeriesMetadata - Categories/Tags maps null or populated, handles exceptions silently")
    void saveSeriesMetadata() {
        Category cat = new Category();
        cat.setCategoryName("Action");
        Tag tag = new Tag();
        tag.setTagName("Superhero");

        // Normal save
        service.saveSeriesMetadata(sampleSeries, Map.of("c1", cat), Map.of("t1", tag));
        verify(seriesMetadataRepository).save(argThat(meta ->
                "series-1".equals(meta.getId()) &&
                meta.getCategory().contains("Action") &&
                meta.getTags().contains("Superhero") &&
                "VIDEO".equals(meta.getContentType())
        ));

        // Null maps & null contentType
        sampleSeries.setContentType(null);
        service.saveSeriesMetadata(sampleSeries, null, null);
        verify(seriesMetadataRepository, times(2)).save(any());

        // Exception caught silently
        doThrow(new RuntimeException("Mongo down")).when(seriesMetadataRepository).save(any());
        service.saveSeriesMetadata(sampleSeries, null, null); // should not throw exception
    }

    // =========================================================================
    // 3. syncAllSeriesFeatures
    // =========================================================================

    @Test
    @DisplayName("syncAllSeriesFeatures - SyncMetadata present vs absent")
    void syncAllSeriesFeatures() {
        SyncMetadata syncMetadata = SyncMetadata.builder()
                .syncType(SyncType.SERIES_DYNAMIC_SYNC)
                .lastSyncTime(Instant.now())
                .build();

        when(syncMetadataRepository.findById(SyncType.SERIES_DYNAMIC_SYNC)).thenReturn(Optional.of(syncMetadata));
        when(seriesLogRepository.aggregateByHourBucketBetweenExclusive(any(), any())).thenReturn(Collections.emptyList());
        when(seriesLogRepository.aggregateByHourBucketBetweenInclusiveStart(any(), any())).thenReturn(Collections.emptyList());

        service.syncAllSeriesFeatures();

        verify(syncMetadataRepository).save(syncMetadata);

        // When SyncMetadata absent
        when(syncMetadataRepository.findById(SyncType.SERIES_DYNAMIC_SYNC)).thenReturn(Optional.empty());
        service.syncAllSeriesFeatures();
        verify(syncMetadataRepository, times(2)).save(any());
    }

    // =========================================================================
    // 4. syncCumulativeStats
    // =========================================================================

    @Test
    @DisplayName("syncCumulativeStats - Empty results vs populated results (clicks > 0 and clicks == 0)")
    void syncCumulativeStats() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now();

        // Empty results
        when(seriesLogRepository.aggregateByHourBucketBetweenExclusive(start, end)).thenReturn(Collections.emptyList());
        CompletableFuture<Void> future1 = service.syncCumulativeStats(start, end);
        assertThat(future1.get()).isNull();

        // Populated results
        SeriesLogData log1 = new SeriesLogData("s1", 100L, 20L, 5L, 2L, 1L, 120.5); // clicks > 0
        SeriesLogData log2 = new SeriesLogData("s2", 0L, 0L, 0L, 0L, 0L, 0.0);    // clicks == 0

        when(seriesLogRepository.aggregateByHourBucketBetweenExclusive(start, end)).thenReturn(List.of(log1, log2));
        when(seriesMetadataRepository.findById("s1")).thenReturn(Optional.of(sampleMetadata));
        when(seriesMetadataRepository.findById("s2")).thenReturn(Optional.empty());

        CompletableFuture<Void> future2 = service.syncCumulativeStats(start, end);
        assertThat(future2.get()).isNull();

        verify(seriesMetadataRepository, times(2)).save(any(SeriesMetadata.class));
    }

    @Test
    @DisplayName("syncCumulativeStats - Throws MongoDocumentException on failure")
    void syncCumulativeStats_Exception() {
        when(seriesLogRepository.aggregateByHourBucketBetweenExclusive(any(), any())).thenThrow(new RuntimeException("DB Error"));

        assertThatThrownBy(() -> service.syncCumulativeStats(LocalDateTime.now(), LocalDateTime.now()))
                .isInstanceOf(MongoDocumentException.class);
    }

    // =========================================================================
    // 5. syncLast24hStats
    // =========================================================================

    @Test
    @DisplayName("syncLast24hStats - Empty results vs populated results")
    void syncLast24hStats() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        // Empty
        when(seriesLogRepository.aggregateByHourBucketBetweenInclusiveStart(any(), eq(now))).thenReturn(Collections.emptyList());
        CompletableFuture<Void> f1 = service.syncLast24hStats(now);
        assertThat(f1.get()).isNull();

        // Populated
        SeriesLogData logData = new SeriesLogData("s1", 50L, 10L, 2L, 1L, 0L, 60.0);
        when(seriesLogRepository.aggregateByHourBucketBetweenInclusiveStart(any(), eq(now))).thenReturn(List.of(logData));
        when(seriesMetadataRepository.findById("s1")).thenReturn(Optional.of(sampleMetadata));

        CompletableFuture<Void> f2 = service.syncLast24hStats(now);
        assertThat(f2.get()).isNull();
        verify(seriesMetadataRepository).save(sampleMetadata);

        // Exception
        when(seriesLogRepository.aggregateByHourBucketBetweenInclusiveStart(any(), any())).thenThrow(new RuntimeException("DB Error"));
        assertThatThrownBy(() -> service.syncLast24hStats(now)).isInstanceOf(MongoDocumentException.class);
    }

    // =========================================================================
    // 6. syncLast7dStats
    // =========================================================================

    @Test
    @DisplayName("syncLast7dStats - Empty results vs populated results")
    void syncLast7dStats() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        // Empty
        when(seriesLogRepository.aggregateByHourBucketBetweenInclusiveStart(any(), eq(now))).thenReturn(Collections.emptyList());
        CompletableFuture<Void> f1 = service.syncLast7dStats(now);
        assertThat(f1.get()).isNull();

        // Populated
        SeriesLogData logData = new SeriesLogData("s1", 200L, 40L, 10L, 5L, 3L, 300.0);
        when(seriesLogRepository.aggregateByHourBucketBetweenInclusiveStart(any(), eq(now))).thenReturn(List.of(logData));
        when(seriesMetadataRepository.findById("s1")).thenReturn(Optional.of(sampleMetadata));

        CompletableFuture<Void> f2 = service.syncLast7dStats(now);
        assertThat(f2.get()).isNull();
        verify(seriesMetadataRepository).save(sampleMetadata);

        // Exception
        when(seriesLogRepository.aggregateByHourBucketBetweenInclusiveStart(any(), any())).thenThrow(new RuntimeException("DB Error"));
        assertThatThrownBy(() -> service.syncLast7dStats(now)).isInstanceOf(MongoDocumentException.class);
    }

    // =========================================================================
    // 7. resetInactiveSeriesStatsInMongo
    // =========================================================================

    @Test
    @DisplayName("resetInactiveSeriesStatsInMongo - Null/empty list vs valid bulk update")
    void resetInactiveSeriesStatsInMongo() {
        service.resetInactiveSeriesStatsInMongo(null, true, true);
        service.resetInactiveSeriesStatsInMongo(Collections.emptyList(), true, true);
        verify(mongoTemplate, never()).updateMulti(any(), any(), any(Class.class));

        service.resetInactiveSeriesStatsInMongo(List.of("s1", "s2"), true, true);
        verify(mongoTemplate).updateMulti(any(Query.class), any(Update.class), eq(SeriesMetadata.class));
    }
}
