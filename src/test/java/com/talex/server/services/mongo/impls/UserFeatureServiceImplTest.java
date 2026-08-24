package com.talex.server.services.mongo.impls;

import com.talex.server.dtos.mongo.*;
import com.talex.server.dtos.responses.series.EpisodeRefs;
import com.talex.server.entities.config.SyncMetadata;
import com.talex.server.entities.mongo.UserFeatureDocument;
import com.talex.server.entities.mongo.userfeatures.DeepEngagementStats;
import com.talex.server.entities.mongo.userfeatures.DynamicPreferences;
import com.talex.server.entities.mongo.userfeatures.InteractionStats;
import com.talex.server.enums.SyncType;
import com.talex.server.repositories.config.SyncMetadataRepository;
import com.talex.server.repositories.mongo.UserFeatureRepository;
import com.talex.server.repositories.series.CategoryRepository;
import com.talex.server.repositories.series.TagRepository;
import com.talex.server.services.QuestDbService;
import com.talex.server.services.series.EpisodeService;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserFeatureServiceImpl Tests")
class UserFeatureServiceImplTest {

    @Mock
    private UserFeatureRepository featureRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private QuestDbService questDbService;
    @Mock
    private EpisodeService episodeService;
    @Mock
    private SyncMetadataRepository syncMetadataRepository;
    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private UserFeatureServiceImpl service;

    private UserFeatureDocument sampleDoc;

    @BeforeEach
    void setUp() {
        sampleDoc = new UserFeatureDocument();
        sampleDoc.setAccountId("user-1");
        sampleDoc.setLanguage("vi");
        sampleDoc.setGender("MALE");
        sampleDoc.setAge("25");
        sampleDoc.setCreatedAt(LocalDateTime.now());
        sampleDoc.setOnboardingGenres(new ArrayList<>(List.of("Action")));
        sampleDoc.setOnboardingTags(new ArrayList<>(List.of("Superhero")));
    }

    // =========================================================================
    // 1. saveOrUpdateFeatures
    // =========================================================================

    @Test
    @DisplayName("saveOrUpdateFeatures - Create new vs update existing doc")
    void saveOrUpdateFeatures() {
        UserFeatureRequest request = new UserFeatureRequest();
        request.setLanguage("en");
        request.setGender("FEMALE");
        request.setAge("30");
        request.setOnboardingGenres(List.of("cat-1"));
        request.setOnboardingTags(List.of("tag-1"));

        // Case 1: Doc exists with non-empty genres/tags
        when(featureRepository.findByAccountId("user-1")).thenReturn(Optional.of(sampleDoc));
        when(categoryRepository.findCategoryNamesByCategoryIds(List.of("cat-1"))).thenReturn(List.of("Comedy"));
        when(tagRepository.findTagNamesByTagIds(List.of("tag-1"))).thenReturn(List.of("Funny"));
        when(featureRepository.save(any(UserFeatureDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        UserFeatureDocument res1 = service.saveOrUpdateFeatures("user-1", request);

        assertThat(res1.getLanguage()).isEqualTo("en");
        assertThat(res1.getGender()).isEqualTo("FEMALE");
        assertThat(res1.getAge()).isEqualTo("30");
        assertThat(res1.getOnboardingGenres()).containsExactly("Comedy");
        assertThat(res1.getOnboardingTags()).containsExactly("Funny");

        // Case 2: New doc (empty existing onboarding genres/tags)
        when(featureRepository.findByAccountId("user-2")).thenReturn(Optional.empty());
        UserFeatureDocument res2 = service.saveOrUpdateFeatures("user-2", request);

        assertThat(res2.getAccountId()).isEqualTo("user-2");
        assertThat(res2.getOnboardingGenres()).isEmpty();
        assertThat(res2.getOnboardingTags()).isEmpty();
    }

    // =========================================================================
    // 2. getFeaturesByUserId & getUserStaticFeatureByAccountId
    // =========================================================================

    @Test
    @DisplayName("getUserStaticFeatureByAccountId - Found vs Absent")
    void getUserStaticFeatureByAccountId() {
        when(featureRepository.findByAccountId("user-1")).thenReturn(Optional.of(sampleDoc));

        UserStaticFeature feature = service.getUserStaticFeatureByAccountId("user-1");

        assertThat(feature).isNotNull();
        assertThat(feature.getAccountId()).isEqualTo("user-1");
        assertThat(feature.getLanguage()).isEqualTo("vi");
        assertThat(feature.getOnboardingGenres()).containsExactly("Action");

        when(featureRepository.findByAccountId("unknown")).thenReturn(Optional.empty());
        assertThat(service.getUserStaticFeatureByAccountId("unknown")).isNull();
    }

    // =========================================================================
    // 3. getUserDynamicFeatureByAccountId
    // =========================================================================

    @Test
    @DisplayName("getUserDynamicFeatureByAccountId - Empty/Absent vs Populated preferences")
    void getUserDynamicFeatureByAccountId() {
        // Absent
        when(featureRepository.findByAccountId("user-1")).thenReturn(Optional.empty());
        UserDynamicFeature emptyFeature = service.getUserDynamicFeatureByAccountId("user-1");
        assertThat(emptyFeature.getCategories()).isEmpty();

        // Populated preferences
        DynamicPreferences prefs = new DynamicPreferences();
        prefs.getGenresWatchTimeRaw().put("cat1", 100.0);
        prefs.getGenresWatchTimeRaw().put("cat2", 200.0); // Highest
        prefs.getTagsWatchTimeRaw().put("tag1", 50.0);

        sampleDoc.setPreferences(prefs);
        when(featureRepository.findByAccountId("user-1")).thenReturn(Optional.of(sampleDoc));

        UserDynamicFeature dynamicFeature = service.getUserDynamicFeatureByAccountId("user-1");

        assertThat(dynamicFeature.getCategories()).containsExactly("cat2", "cat1");
        assertThat(dynamicFeature.getTags()).containsExactly("tag1");
    }

    // =========================================================================
    // 4. syncUserDynamicFeatures
    // =========================================================================

    @Test
    @DisplayName("syncUserDynamicFeatures - Empty QuestDB results vs Populated results")
    void syncUserDynamicFeatures() {
        SyncMetadata syncMeta = SyncMetadata.builder()
                .syncType(SyncType.USER_INTERACTION_DEEP_ENGAGEMENT)
                .lastSyncTime(Instant.EPOCH)
                .build();
        when(syncMetadataRepository.findById(SyncType.USER_INTERACTION_DEEP_ENGAGEMENT)).thenReturn(Optional.of(syncMeta));

        // Case 1: Empty results from QuestDB
        when(questDbService.queryInteractionsAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        service.syncUserDynamicFeatures();
        verify(syncMetadataRepository).save(syncMeta);

        // Case 2: Populated results
        QuestDbQueryResult r1 = new QuestDbQueryResult();
        r1.setAccountId("user-1");
        r1.setTotalClicks(10L);
        r1.setTotalLikes(2L);
        r1.setTotalBookmarks(1L);
        r1.setTotalShares(1L);
        r1.setTotalComments(1L);
        r1.setPeriodWatchTime(120.0);

        when(questDbService.queryInteractionsAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(List.of(r1)));
        when(featureRepository.findAllById(anySet())).thenReturn(List.of(sampleDoc));

        service.syncUserDynamicFeatures();

        verify(featureRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("syncUserDynamicFeatures - Throws RuntimeException on failure")
    void syncUserDynamicFeatures_Exception() {
        when(syncMetadataRepository.findById(any())).thenThrow(new RuntimeException("QuestDB down"));

        assertThatThrownBy(() -> service.syncUserDynamicFeatures())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Quy trình đồng bộ dữ liệu Dynamic Features thất bại");
    }

    // =========================================================================
    // 5. syncUserDynamicPreferences
    // =========================================================================

    @Test
    @DisplayName("syncUserDynamicPreferences - Empty results vs Populated results")
    void syncUserDynamicPreferences() {
        SyncMetadata syncMeta = SyncMetadata.builder()
                .syncType(SyncType.USER_DYNAMIC_PREFERENCES)
                .lastSyncTime(Instant.EPOCH)
                .build();
        when(syncMetadataRepository.findById(SyncType.USER_DYNAMIC_PREFERENCES)).thenReturn(Optional.of(syncMeta));

        // Case 1: Empty active accounts
        when(questDbService.queryPreferencesAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        service.syncUserDynamicPreferences();

        // Case 2: Populated QuestDB results
        QuestDbPreferenceResult prefRes = new QuestDbPreferenceResult();
        prefRes.setAccountId("user-1");
        prefRes.setEpisodeId("ep-1");
        prefRes.setTotalClicks(5L);
        prefRes.setTotalWatchTime(60.0);

        EpisodeRefs refs = new EpisodeRefs("ep-1", List.of("tag1"), List.of("cat1"));

        when(questDbService.queryPreferencesAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(List.of(prefRes)));
        when(featureRepository.findByAccountId("user-1")).thenReturn(Optional.of(sampleDoc));
        when(episodeService.getEpisodeRefsByEpisodeId("ep-1")).thenReturn(refs);

        service.syncUserDynamicPreferences();

        verify(featureRepository).saveAll(anyCollection());
        verify(syncMetadataRepository).save(syncMeta);
    }

    @Test
    @DisplayName("syncUserDynamicPreferences - Throws RuntimeException on failure")
    void syncUserDynamicPreferences_Exception() {
        SyncMetadata syncMeta = SyncMetadata.builder()
                .syncType(SyncType.USER_DYNAMIC_PREFERENCES)
                .lastSyncTime(Instant.EPOCH)
                .build();
        when(syncMetadataRepository.findById(SyncType.USER_DYNAMIC_PREFERENCES)).thenReturn(Optional.of(syncMeta));
        when(questDbService.queryPreferencesAsync(any(), any())).thenThrow(new RuntimeException("QuestDB down"));

        assertThatThrownBy(() -> service.syncUserDynamicPreferences())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Quy trình đồng bộ dữ liệu Dynamic Preferences thất bại");
    }

    // =========================================================================
    // 6. cleanUp24hFeatures & cleanUp7dFeatures
    // =========================================================================

    @Test
    @DisplayName("cleanUp24hFeatures & cleanUp7dFeatures - Null/empty list vs valid bulk updates")
    void cleanUpFeatures() {
        service.cleanUp24hFeatures(null);
        service.cleanUp24hFeatures(Collections.emptyList());
        verify(mongoTemplate, never()).updateMulti(any(), any(), any(Class.class));

        service.cleanUp24hFeatures(List.of("user-1"));
        verify(mongoTemplate).updateMulti(any(Query.class), any(Update.class), eq(UserFeatureDocument.class));

        service.cleanUp7dFeatures(null);
        service.cleanUp7dFeatures(Collections.emptyList());

        service.cleanUp7dFeatures(List.of("user-1"));
        verify(mongoTemplate, times(2)).updateMulti(any(Query.class), any(Update.class), eq(UserFeatureDocument.class));
    }
}
