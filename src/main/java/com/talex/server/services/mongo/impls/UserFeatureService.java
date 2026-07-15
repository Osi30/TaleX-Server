package com.talex.server.services.mongo.impls;

import com.talex.server.dtos.mongo.QuestDbPreferenceResult;
import com.talex.server.dtos.mongo.QuestDbQueryResult;
import com.talex.server.dtos.mongo.UserFeatureRequest;
import com.talex.server.dtos.responses.EpisodeRefs;
import com.talex.server.entities.SyncMetadata;
import com.talex.server.entities.mongo.UserFeatureDocument;
import com.talex.server.entities.mongo.userfeatures.DeepEngagementStats;
import com.talex.server.entities.mongo.userfeatures.DynamicPreferences;
import com.talex.server.entities.mongo.userfeatures.InteractionStats;
import com.talex.server.entities.mongo.userfeatures.MonetizationStats;
import com.talex.server.enums.SyncType;
import com.talex.server.records.MonetizationData;
import com.talex.server.repositories.SyncMetadataRepository;
import com.talex.server.repositories.mongo.UserFeatureRepository;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.EpisodeService;
import com.talex.server.services.IQuestDbService;
import com.talex.server.services.mongo.IUserFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserFeatureService implements IUserFeatureService {
    private final UserFeatureRepository featureRepository;
    private final OrderRepository orderRepository;
    private final IQuestDbService questDbService;
    private final EpisodeService episodeService;
    private final SyncMetadataRepository syncMetadataRepository;

    @Override
    public UserFeatureDocument saveOrUpdateFeatures(String userId, UserFeatureRequest incoming) {
        // Nếu chưa có document dưới DB thì khởi tạo thực thể mới
        UserFeatureDocument existing = featureRepository.findByAccountId(userId)
                .orElseGet(() -> {
                    UserFeatureDocument newDoc = new UserFeatureDocument();
                    newDoc.setAccountId(userId);
                    return newDoc;
                });

        // Đảm bảo các List nội bộ không bị null phòng hờ lỗi mapping dữ liệu cũ
        if (existing.getDeviceTypes() == null) existing.setDeviceTypes(new ArrayList<>());
        if (existing.getOs() == null) existing.setOs(new ArrayList<>());

        // 1. Tích lũy Device Type (Nếu chưa tồn tại trong List thì mới add vào)
        if (incoming.getDeviceType() != null && !incoming.getDeviceType().isBlank()) {
            if (!existing.getDeviceTypes().contains(incoming.getDeviceType())) {
                existing.getDeviceTypes().add(incoming.getDeviceType());
            }
        }

        // 2. Tích lũy OS (Nếu chưa tồn tại trong List thì mới add vào)
        if (incoming.getOs() != null && !incoming.getOs().isBlank()) {
            if (!existing.getOs().contains(incoming.getOs())) {
                existing.getOs().add(incoming.getOs());
            }
        }

        // 3. Cập nhật Địa lý (Chỉ cập nhật khi trường request có dữ liệu)
        if (incoming.getLanguage() != null) existing.setLanguage(incoming.getLanguage());
        if (incoming.getTimezone() != null) existing.setTimezone(incoming.getTimezone());

        // 4. Cập nhật Demographic / Profile (Chỉ cập nhật khi trường request có dữ liệu)
        if (incoming.getAccountAge() != null) existing.setAccountAge(incoming.getAccountAge());
        if (incoming.getCreatorTier() != null) existing.setCreatorTier(incoming.getCreatorTier());
        if (incoming.getGender() != null) existing.setGender(incoming.getGender());
        if (incoming.getAge() != null) existing.setAge(incoming.getAge());

        // 5. Các trường Write-once (Chỉ ghi nhận ở lần đầu tiên hoặc khi DB trống)
        if (existing.getRegisterBy() == null && incoming.getRegisterBy() != null) {
            existing.setRegisterBy(incoming.getRegisterBy());
        }
        if (existing.getOnboardingMovieGenres().isEmpty() && incoming.getOnboardingMovieGenres() != null) {
            existing.setOnboardingMovieGenres(incoming.getOnboardingMovieGenres());
        }
        if (existing.getOnboardingComicGenres().isEmpty() && incoming.getOnboardingComicGenres() != null) {
            existing.setOnboardingComicGenres(incoming.getOnboardingComicGenres());
        }

        // Thực hiện lưu trữ/cập nhật xuống MongoDB
        return featureRepository.save(existing);
    }

    @Override
    public Optional<UserFeatureDocument> getFeaturesByUserId(String userId) {
        return featureRepository.findByAccountId(userId);
    }

    /// Lưu trữ InteractionStats và DeepEngagementStats
    @Override
    public synchronized void syncUserDynamicFeatures() {
        Instant now = Instant.now();
        try {
            // 1. Lấy mốc thời gian đồng bộ cuối cùng
            SyncMetadata syncMetadata = syncMetadataRepository.findById(SyncType.USER_INTERACTION_DEEP_ENGAGEMENT)
                    .orElseGet(() -> SyncMetadata.builder()
                            .syncType(SyncType.USER_INTERACTION_DEEP_ENGAGEMENT)
                            .lastSyncTime(now.minus(1, ChronoUnit.DAYS))
                            .build());

            Instant lastSync = syncMetadata.getLastSyncTime();

            // Khởi chạy song song 3 luồng lấy dữ liệu từ QuestDB
            CompletableFuture<List<QuestDbQueryResult>> deltaFuture = questDbService.queryInteractionsAsync(lastSync, now);
            CompletableFuture<List<QuestDbQueryResult>> t24hFuture = questDbService.queryInteractionsAsync(now.minus(24, ChronoUnit.HOURS), now);
            CompletableFuture<List<QuestDbQueryResult>> t7dFuture = questDbService.queryInteractionsAsync(now.minus(7, ChronoUnit.DAYS), now);

            CompletableFuture.allOf(deltaFuture, t24hFuture, t7dFuture).join();

            List<QuestDbQueryResult> deltaList = deltaFuture.get();
            List<QuestDbQueryResult> t24hList = t24hFuture.get();
            List<QuestDbQueryResult> t7dList = t7dFuture.get();

            // 2. Gom tất cả Account ID xuất hiện trong các tập dữ liệu
            Set<String> allAccountIds = new HashSet<>();
            deltaList.forEach(r -> allAccountIds.add(r.getAccountId()));
            t24hList.forEach(r -> allAccountIds.add(r.getAccountId()));
            t7dList.forEach(r -> allAccountIds.add(r.getAccountId()));

            if (allAccountIds.isEmpty()) {
                syncMetadata.setLastSyncTime(now);
                syncMetadataRepository.save(syncMetadata);
                return;
            }

            // 3. Chuyển đổi List sang Map để truy xuất O(1)
            Map<String, QuestDbQueryResult> deltaMap = deltaList.stream().collect(Collectors.toMap(QuestDbQueryResult::getAccountId, r -> r));
            Map<String, QuestDbQueryResult> t24hMap = t24hList.stream().collect(Collectors.toMap(QuestDbQueryResult::getAccountId, r -> r));
            Map<String, QuestDbQueryResult> t7dMap = t7dList.stream().collect(Collectors.toMap(QuestDbQueryResult::getAccountId, r -> r));

            // 4. Kéo các Document hiện tại từ MongoDB lên RAM
            List<UserFeatureDocument> existingDocs = featureRepository.findAllById(allAccountIds);
            Map<String, UserFeatureDocument> existingDocsMap = existingDocs.stream()
                    .collect(Collectors.toMap(UserFeatureDocument::getAccountId, doc -> doc));

            List<UserFeatureDocument> docsToSave = new ArrayList<>();

            // 5. Tiến hành lặp và mapping xử lý chia nhỏ phương thức
            for (String accountId : allAccountIds) {
                UserFeatureDocument doc = existingDocsMap.computeIfAbsent(accountId, id -> {
                    UserFeatureDocument newDoc = new UserFeatureDocument();
                    newDoc.setAccountId(id);
                    newDoc.setInteractions(new InteractionStats());
                    newDoc.setDeepEngagement(new DeepEngagementStats());
                    return newDoc;
                });

                // Phòng hộ bảo vệ Null Pointer Exception nếu mapping từ DB cũ thiếu sub-document
                if (doc.getInteractions() == null) doc.setInteractions(new InteractionStats());
                if (doc.getDeepEngagement() == null) doc.setDeepEngagement(new DeepEngagementStats());

                QuestDbQueryResult deltaRes = deltaMap.get(accountId);
                QuestDbQueryResult t24hRes = t24hMap.get(accountId);
                QuestDbQueryResult t7dRes = t7dMap.get(accountId);

                // GỌI CÁC PHƯƠNG THỨC XỬ LÝ RIÊNG BIỆT (ĐÃ CHIA NHỎ)
                updateInteractionStats(doc, deltaRes, t24hRes, t7dRes);
                updateDeepEngagementStats(doc, deltaRes, t24hRes, t7dRes);

                docsToSave.add(doc);
            }

            // 6. Lưu hàng loạt xuống MongoDB & Cập nhật Metadata
            featureRepository.saveAll(docsToSave);
            syncMetadata.setLastSyncTime(now);
            syncMetadataRepository.save(syncMetadata);

        } catch (Exception e) {
            throw new RuntimeException("Quy trình đồng bộ dữ liệu Dynamic Features thất bại", e);
        }
    }

    /// Lưu trữ DynamicPreferences
    @Override
    public void syncUserDynamicPreferences() {
        Instant now = Instant.now();

        // 1. Lấy mốc thời gian đồng bộ cuối cùng của sở thích động
        SyncMetadata syncMetadata = syncMetadataRepository.findById(SyncType.USER_DYNAMIC_PREFERENCES)
                .orElseGet(() -> SyncMetadata.builder()
                        .syncType(SyncType.USER_DYNAMIC_PREFERENCES)
                        .lastSyncTime(now.minus(1, ChronoUnit.DAYS))
                        .build());
        Instant lastSyncTime = syncMetadata.getLastSyncTime();

        try {
            // Định nghĩa các mốc thời gian cho các khung cửa sổ (Windows)
            Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
            Instant twentyFourHoursAgo = now.minus(1, ChronoUnit.DAYS);

            // 2. Kích hoạt truy vấn song song đồng thời 3 luồng xuống QuestDB để tối ưu IO
            CompletableFuture<List<QuestDbPreferenceResult>> incrementalFuture = questDbService.queryPreferencesAsync(lastSyncTime, now);
            CompletableFuture<List<QuestDbPreferenceResult>> last7dFuture = questDbService.queryPreferencesAsync(sevenDaysAgo, now);
            CompletableFuture<List<QuestDbPreferenceResult>> last24hFuture = questDbService.queryPreferencesAsync(twentyFourHoursAgo, now);

            // Đợi cả 3 tiến trình async hoàn tất
            CompletableFuture.allOf(incrementalFuture, last7dFuture, last24hFuture).join();

            List<QuestDbPreferenceResult> incrementalResults = incrementalFuture.get();
            List<QuestDbPreferenceResult> last7dResults = last7dFuture.get();
            List<QuestDbPreferenceResult> last24hResults = last24hFuture.get();

            // 3. Thu thập tất cả AccountId có hoạt động trong bất kỳ khung thời gian nào
            Set<String> activeAccountIds = new HashSet<>();
            incrementalResults.forEach(r -> activeAccountIds.add(r.getAccountId()));
            last7dResults.forEach(r -> activeAccountIds.add(r.getAccountId()));
            last24hResults.forEach(r -> activeAccountIds.add(r.getAccountId()));

            if (activeAccountIds.isEmpty()) {
                return;
            }

            // Tải các tài liệu hiện tại từ MongoDB lên bộ nhớ tạm
            Map<String, UserFeatureDocument> userDocMap = new HashMap<>();
            for (String accountId : activeAccountIds) {
                UserFeatureDocument doc = featureRepository.findByAccountId(accountId)
                        .orElseGet(() -> UserFeatureDocument.builder()
                                .accountId(accountId)
                                .preferences(new DynamicPreferences())
                                .build());
                if (doc.getPreferences() == null) {
                    doc.setPreferences(new DynamicPreferences());
                }
                userDocMap.put(accountId, doc);
            }

            // Nhóm kết quả từ QuestDB theo từng User để tiện xử lý nội bộ
            Map<String, List<QuestDbPreferenceResult>> incrementalByAccount = incrementalResults.stream()
                    .collect(Collectors.groupingBy(QuestDbPreferenceResult::getAccountId));
            Map<String, List<QuestDbPreferenceResult>> last7dByAccount = last7dResults.stream()
                    .collect(Collectors.groupingBy(QuestDbPreferenceResult::getAccountId));
            Map<String, List<QuestDbPreferenceResult>> last24hByAccount = last24hResults.stream()
                    .collect(Collectors.groupingBy(QuestDbPreferenceResult::getAccountId));

            // TỐI ƯU HÓA: Tạo một bộ nhớ đệm Local cho EpisodeRefs.
            // Vì nhiều User có thể xem cùng một Episode, việc cache giúp tránh đập SQL (MySQL/PostgreSQL) liên tục
            Map<String, EpisodeRefs> episodeRefsCache = new HashMap<>();

            // 4. Duyệt qua từng User đang hoạt động để xử lý dữ liệu đặc trưng sở thích
            for (String accountId : activeAccountIds) {
                UserFeatureDocument doc = userDocMap.get(accountId);
                DynamicPreferences prefs = doc.getPreferences();

                // --- LUỒNG 1: CỘNG DỒN INCREMENTAL VÀO BỘ ĐẾM RAW ĐỂ TÍNH TI TỈ LỆ TOÀN THỜI GIAN ---
                List<QuestDbPreferenceResult> incList = incrementalByAccount.get(accountId);
                if (incList != null) {
                    for (QuestDbPreferenceResult res : incList) {
                        EpisodeRefs refs = episodeRefsCache.computeIfAbsent(res.getEpisodeId(),
                                episodeService::getEpisodeRefsByEpisodeId);
                        updateRawCounters(prefs, res, refs);
                    }
                    // Tính toán lại phần trăm phân bổ sở thích All-time
                    recalculatePreferencePercentages(prefs);
                }

                // --- LUỒNG 2: TÍNH TOÁN TỶ LỆ PHẦN TRĂM SỞ THÍCH CHO 7 NGÀY QUA (KHÔNG LƯU RAW) ---
                List<QuestDbPreferenceResult> s7dList = last7dByAccount.getOrDefault(accountId, Collections.emptyList());
                calculateWindowRatios(prefs, s7dList, 7, episodeRefsCache);

                // --- LUỒNG 3: TÍNH TOÁN TỶ LỆ PHẦN TRĂM SỞ THÍCH CHO 24 GIỜ QUA (KHÔNG LƯU RAW) ---
                List<QuestDbPreferenceResult> s24hList = last24hByAccount.getOrDefault(accountId, Collections.emptyList());
                calculateWindowRatios(prefs, s24hList, 24, episodeRefsCache);
            }

            // 5. Lưu đồng loạt các Document đã xử lý xuống MongoDB
            featureRepository.saveAll(userDocMap.values());

            // Ghi nhận mốc thời gian hoàn thành
            syncMetadata.setLastSyncTime(now);
            syncMetadataRepository.save(syncMetadata);

        } catch (Exception e) {
            throw new RuntimeException("Quy trình đồng bộ dữ liệu Dynamic Preferences thất bại", e);
        }
    }

    /// Lưu trữ Monetization
    @Override
    public void syncUserMonetizationFeatures() {
        try {
            // 1. Lấy mốc thời gian UTC từ bảng sync_metadata (Postgres)
            SyncMetadata syncMetadata = syncMetadataRepository.findById(SyncType.USER_MONETIZATION)
                    .orElseGet(() -> SyncMetadata.builder()
                            .syncType(SyncType.USER_MONETIZATION)
                            .lastSyncTime(Instant.EPOCH) // Lần đầu chạy sẽ quét từ mốc thời gian gốc ban đầu
                            .build());

            // Mốc thời gian hiện tại của hệ thống (Dạng Instant UTC để lưu lại vào metadata an toàn)
            Instant nowInstant = Instant.now();

            // 2. Chuyển đổi cẩn thận sang LocalDateTime theo Múi giờ hệ thống / Múi giờ lưu trữ của DB
            ZoneId systemZone = ZoneId.systemDefault();
            LocalDateTime lastSyncLocalDateTime = LocalDateTime.ofInstant(syncMetadata.getLastSyncTime(), systemZone);
            LocalDateTime nowLocalDateTime = LocalDateTime.ofInstant(nowInstant, systemZone);

            // 3. Thực hiện quét DELTA từ Postgres
            List<MonetizationData> deltas = orderRepository.aggregateMonetizationStatsDelta(lastSyncLocalDateTime, nowLocalDateTime);

            if (deltas.isEmpty()) {
                syncMetadata.setLastSyncTime(nowInstant);
                syncMetadataRepository.save(syncMetadata);
                return;
            }

            List<UserFeatureDocument> docsToSave = new ArrayList<>();

            // 4. Lặp qua các bản ghi phát sinh để CỘNG DỒN dữ liệu
            for (MonetizationData delta : deltas) {
                if (delta.accountId() == null) continue;
                String userIdStr = delta.accountId().toString();

                // Tìm document sẵn có trong MongoDB, nếu không có thì khởi tạo mới
                UserFeatureDocument doc = featureRepository.findByAccountId(userIdStr)
                        .orElseGet(() -> UserFeatureDocument.builder().accountId(userIdStr).build());

                if (doc.getMonetization() == null) {
                    doc.setMonetization(new MonetizationStats());
                }

                MonetizationStats monoStats = doc.getMonetization();

                // --- TIẾN HÀNH CỘNG DỒN DỮ LIỆU ---
                BigDecimal currentSpent = monoStats.getTotalSpentAmount() != null ? monoStats.getTotalSpentAmount() : BigDecimal.ZERO;
                BigDecimal deltaSpent = delta.totalSpentAmount() != null ? delta.totalSpentAmount() : BigDecimal.ZERO;
                monoStats.setTotalSpentAmount(currentSpent.add(deltaSpent)); // Cộng dồn tổng tiền

                long currentSubCount = monoStats.getPremiumSubscriptionCount() != null ? monoStats.getPremiumSubscriptionCount() : 0L;
                monoStats.setPremiumSubscriptionCount(currentSubCount + (delta.premiumSubscriptionCount() != null ? delta.premiumSubscriptionCount() : 0L));

                long currentSingleCount = monoStats.getSinglePurchaseCount() != null ? monoStats.getSinglePurchaseCount() : 0L;
                monoStats.setSinglePurchaseCount(currentSingleCount + (delta.singlePurchaseCount() != null ? delta.singlePurchaseCount() : 0L));

                long currentPushCount = monoStats.getInteractionPushCount() != null ? monoStats.getInteractionPushCount() : 0L;
                monoStats.setInteractionPushCount(currentPushCount + (delta.interactionPushCount() != null ? delta.interactionPushCount() : 0L));

                // Cập nhật mốc thời gian mua hàng cuối cùng (Vì quét delta tịnh tiến, mốc này luôn là mới nhất)
                if (delta.lastPurchaseTime() != null) {
                    monoStats.setLastPurchaseTime(delta.lastPurchaseTime());
                }

                docsToSave.add(doc);
            }

            // 5. Lưu đồng loạt các bản ghi cập nhật/cộng dồn vào MongoDB
            if (!docsToSave.isEmpty()) {
                featureRepository.saveAll(docsToSave);
            }

            // 6. Ghi nhận lại mốc thời gian đã quét thành công an toàn dưới dạng Instant UTC
            syncMetadata.setLastSyncTime(nowInstant);
            syncMetadataRepository.save(syncMetadata);

        } catch (Exception e) {
            throw new RuntimeException("Quy trình đồng bộ dữ liệu doanh thu thất bại", e);
        }
    }

    /**
     * Phương thức xử lý riêng biệt cho nhóm Tần suất tương tác (InteractionStats)
     */
    private void updateInteractionStats(UserFeatureDocument doc, QuestDbQueryResult delta, QuestDbQueryResult t24h, QuestDbQueryResult t7d) {
        InteractionStats stats = doc.getInteractions();

        // Khối 1: Toàn thời gian (Cộng dồn Delta)
        if (delta != null) {
            stats.setTotalClicks(stats.getTotalClicks() + delta.getTotalClicks());
            stats.setTotalLikes(stats.getTotalLikes() + delta.getTotalLikes());
            stats.setTotalBookmarks(stats.getTotalBookmarks() + delta.getTotalBookmarks());
            stats.setTotalShares(stats.getTotalShares() + delta.getTotalShares());
            stats.setTotalComments(stats.getTotalComments() + delta.getTotalComments());
        }

        // Tính toán tỷ lệ Toàn thời gian
        stats.setLikeToClickRatio(calculateRatio(stats.getTotalLikes(), stats.getTotalClicks()));
        stats.setBookmarkToClickRatio(calculateRatio(stats.getTotalBookmarks(), stats.getTotalClicks()));
        stats.setShareToClickRatio(calculateRatio(stats.getTotalShares(), stats.getTotalClicks()));
        stats.setCommentToClickRatio(calculateRatio(stats.getTotalComments(), stats.getTotalClicks()));

        // Khối 2: Trong 24 giờ qua (Ghi đè tuyệt đối)
        if (t24h != null) {
            stats.setClicksLast24h(t24h.getTotalClicks());
            stats.setLikesLast24h(t24h.getTotalLikes());
            stats.setBookmarksLast24h(t24h.getTotalBookmarks());
            stats.setSharesLast24h(t24h.getTotalShares());
            stats.setCommentsLast24h(t24h.getTotalComments());
        } else {
            stats.setClicksLast24h(0L);
            stats.setLikesLast24h(0L);
            stats.setBookmarksLast24h(0L);
            stats.setSharesLast24h(0L);
            stats.setCommentsLast24h(0L);
        }
        stats.setLikeToClickRatioLast24h(calculateRatio(stats.getLikesLast24h(), stats.getClicksLast24h()));
        stats.setBookmarkToClickRatioLast24h(calculateRatio(stats.getBookmarksLast24h(), stats.getClicksLast24h()));
        stats.setShareToClickRatioLast24h(calculateRatio(stats.getSharesLast24h(), stats.getClicksLast24h()));
        stats.setCommentToClickRatioLast24h(calculateRatio(stats.getCommentsLast24h(), stats.getClicksLast24h()));

        // Khối 3: Trong 7 ngày qua (Ghi đè tuyệt đối)
        if (t7d != null) {
            stats.setClicksLast7d(t7d.getTotalClicks());
            stats.setLikesLast7d(t7d.getTotalLikes());
            stats.setBookmarksLast7d(t7d.getTotalBookmarks());
            stats.setSharesLast7d(t7d.getTotalShares());
            stats.setCommentsLast7d(t7d.getTotalComments());
        } else {
            stats.setClicksLast7d(0L);
            stats.setLikesLast7d(0L);
            stats.setBookmarksLast7d(0L);
            stats.setSharesLast7d(0L);
            stats.setCommentsLast7d(0L);
        }
        stats.setLikeToClickRatioLast7d(calculateRatio(stats.getLikesLast7d(), stats.getClicksLast7d()));
        stats.setBookmarkToClickRatioLast7d(calculateRatio(stats.getBookmarksLast7d(), stats.getClicksLast7d()));
        stats.setShareToClickRatioLast7d(calculateRatio(stats.getSharesLast7d(), stats.getClicksLast7d()));
        stats.setCommentToClickRatioLast7d(calculateRatio(stats.getCommentsLast7d(), stats.getClicksLast7d()));
    }

    /**
     * Phương thức xử lý riêng biệt cho nhóm Mức độ gắn kết sâu (DeepEngagementStats)
     */
    private void updateDeepEngagementStats(UserFeatureDocument doc, QuestDbQueryResult delta, QuestDbQueryResult t24h, QuestDbQueryResult t7d) {
        DeepEngagementStats engagement = doc.getDeepEngagement();

        // Khối 1: Toàn thời gian (Cộng dồn Delta)
        if (delta != null) {
            engagement.setTotalWatchTime(engagement.getTotalWatchTime() + delta.getPeriodWatchTime());
        }

        // Khối 2: 24h qua (Ghi đè)
        if (t24h != null) {
            engagement.setWatchTimeLast24h(t24h.getPeriodWatchTime());
        } else {
            engagement.setWatchTimeLast24h(0D);
        }

        // Khối 3: 7 ngày qua (Ghi đè)
        if (t7d != null) {
            engagement.setWatchTimeLast7d(t7d.getPeriodWatchTime());
        } else {
            engagement.setWatchTimeLast7d(0D);
        }
    }

    private double calculateRatio(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        double ratio = (double) numerator / denominator;
        // Làm tròn lấy 4 chữ số sau dấu phẩy
        return Math.round(ratio * 10000.0) / 10000.0;
    }

    private void recalculatePreferencePercentages(DynamicPreferences prefs) {
        // Tính tổng số click và tổng thời gian xem của tất cả thể loại nhằm chia tỷ lệ %
        long totalGenreClicks = prefs.getGenresClicksRaw().values().stream().mapToLong(Long::longValue).sum();
        double totalGenreWatchTime = prefs.getGenresWatchTimeRaw().values().stream().mapToDouble(Double::doubleValue).sum();

        // Tương tự với Tags
        long totalTagClicks = prefs.getTagsClicksRaw().values().stream().mapToLong(Long::longValue).sum();
        double totalTagWatchTime = prefs.getTagsWatchTimeRaw().values().stream().mapToDouble(Double::doubleValue).sum();

        // Cập nhật map % cho Thể loại
        prefs.getGenresClicksRaw().forEach((genre, clicks) ->
                prefs.getPreferredGenresByClicks().put(genre, totalGenreClicks > 0 ? (double) clicks / totalGenreClicks : 0.0));
        prefs.getGenresWatchTimeRaw().forEach((genre, time) ->
                prefs.getPreferredGenresByWatchTime().put(genre, totalGenreWatchTime > 0 ? time / totalGenreWatchTime : 0.0));

        // Cập nhật map % cho Thẻ
        prefs.getTagsClicksRaw().forEach((tag, clicks) ->
                prefs.getPreferredTagsByClicks().put(tag, totalTagClicks > 0 ? (double) clicks / totalTagClicks : 0.0));
        prefs.getTagsWatchTimeRaw().forEach((tag, time) ->
                prefs.getPreferredTagsByWatchTime().put(tag, totalTagWatchTime > 0 ? time / totalTagWatchTime : 0.0));
    }

    /**
     * Hàm hỗ trợ cộng dồn dữ liệu log thô (Incremental) vào các bộ đếm tích lũy toàn thời gian.
     */
    private void updateRawCounters(DynamicPreferences prefs, QuestDbPreferenceResult res, EpisodeRefs refs) {
        if (refs == null) return;
        long clicks = res.getTotalClicks();
        double watchTime = res.getTotalWatchTime();

        // Thể loại (Genres/Categories)
        if (refs.getCategories() != null) {
            for (String genreId : refs.getCategories()) {
                prefs.getGenresClicksRaw().put(genreId, prefs.getGenresClicksRaw().getOrDefault(genreId, 0L) + clicks);
                prefs.getGenresWatchTimeRaw().put(genreId, prefs.getGenresWatchTimeRaw().getOrDefault(genreId, 0D) + watchTime);
            }
        }
        // Thẻ (Tags)
        if (refs.getTags() != null) {
            for (String tagId : refs.getTags()) {
                prefs.getTagsClicksRaw().put(tagId, prefs.getTagsClicksRaw().getOrDefault(tagId, 0L) + clicks);
                prefs.getTagsWatchTimeRaw().put(tagId, prefs.getTagsWatchTimeRaw().getOrDefault(tagId, 0D) + watchTime);
            }
        }
    }

    /**
     * Hàm hỗ trợ gom cụm log và chia tỷ lệ phần trăm trực tiếp theo từng khung thời gian xác định (24h hoặc 7d).
     * Không tác động hay cộng dồn vào các cấu trúc Raw cố định trên MongoDB.
     */
    private void calculateWindowRatios(DynamicPreferences prefs, List<QuestDbPreferenceResult> results, int windowDays, Map<String, EpisodeRefs> cache) {
        // Nếu trong khung giờ này người dùng không có hoạt động nào, tiến hành clear trắng map tỷ lệ
        if (results.isEmpty()) {
            if (windowDays == 24) {
                prefs.getPreferredGenresByClicksLast24h().clear();
                prefs.getPreferredGenresByWatchTimeLast24h().clear();
                prefs.getPreferredTagsByClicksLast24h().clear();
                prefs.getPreferredTagsByWatchTimeLast24h().clear();
            } else {
                prefs.getPreferredGenresByClicksLast7d().clear();
                prefs.getPreferredGenresByWatchTimeLast7d().clear();
                prefs.getPreferredTagsByClicksLast7d().clear();
                prefs.getPreferredTagsByWatchTimeLast7d().clear();
            }
            return;
        }

        // Các Map tạm thời phục vụ cho gom cụm và tính tổng số lượng trong phiên chạy hiện tại
        Map<String, Long> tempGenreClicks = new HashMap<>();
        Map<String, Double> tempGenreWatchTime = new HashMap<>();
        Map<String, Long> tempTagClicks = new HashMap<>();
        Map<String, Double> tempTagWatchTime = new HashMap<>();

        for (QuestDbPreferenceResult res : results) {
            EpisodeRefs refs = cache.computeIfAbsent(res.getEpisodeId(), id -> episodeService.getEpisodeRefsByEpisodeId(id));
            if (refs == null) continue;

            long clicks = res.getTotalClicks();
            double watchTime = res.getTotalWatchTime();

            if (refs.getCategories() != null) {
                for (String genreId : refs.getCategories()) {
                    tempGenreClicks.put(genreId, tempGenreClicks.getOrDefault(genreId, 0L) + clicks);
                    tempGenreWatchTime.put(genreId, tempGenreWatchTime.getOrDefault(genreId, 0.0) + watchTime);
                }
            }
            if (refs.getTags() != null) {
                for (String tagId : refs.getTags()) {
                    tempTagClicks.put(tagId, tempTagClicks.getOrDefault(tagId, 0L) + clicks);
                    tempTagWatchTime.put(tagId, tempTagWatchTime.getOrDefault(tagId, 0.0) + watchTime);
                }
            }
        }

        // Tính tổng tất cả phần tử trong window để làm mẫu số chia tỷ lệ
        long totalGenreClicks = tempGenreClicks.values().stream().mapToLong(Long::longValue).sum();
        double totalGenreWatchTime = tempGenreWatchTime.values().stream().mapToDouble(Double::doubleValue).sum();
        long totalTagClicks = tempTagClicks.values().stream().mapToLong(Long::longValue).sum();
        double totalTagWatchTime = tempTagWatchTime.values().stream().mapToDouble(Double::doubleValue).sum();

        // Tính tỷ lệ phần trăm (%) thực tế của từng mục
        Map<String, Double> genreClicksRatio = new HashMap<>();
        Map<String, Double> genreWatchTimeRatio = new HashMap<>();
        Map<String, Double> tagClicksRatio = new HashMap<>();
        Map<String, Double> tagWatchTimeRatio = new HashMap<>();

        tempGenreClicks.forEach((g, c) -> genreClicksRatio.put(g, totalGenreClicks > 0 ? (double) c / totalGenreClicks : 0.0));
        tempGenreWatchTime.forEach((g, t) -> genreWatchTimeRatio.put(g, totalGenreWatchTime > 0 ? t / totalGenreWatchTime : 0.0));
        tempTagClicks.forEach((t, c) -> tagClicksRatio.put(t, totalTagClicks > 0 ? (double) c / totalTagClicks : 0.0));
        tempTagWatchTime.forEach((t, tTime) -> tagWatchTimeRatio.put(t, totalTagWatchTime > 0 ? tTime / totalTagWatchTime : 0.0));

        // Đẩy Map kết quả tỷ lệ vào đúng thuộc tính đích của DynamicPreferences nhờ vào Lombok Setters
        if (windowDays == 24) {
            prefs.setPreferredGenresByClicksLast24h(genreClicksRatio);
            prefs.setPreferredGenresByWatchTimeLast24h(genreWatchTimeRatio);
            prefs.setPreferredTagsByClicksLast24h(tagClicksRatio);
            prefs.setPreferredTagsByWatchTimeLast24h(tagWatchTimeRatio);
        } else {
            prefs.setPreferredGenresByClicksLast7d(genreClicksRatio);
            prefs.setPreferredGenresByWatchTimeLast7d(genreWatchTimeRatio);
            prefs.setPreferredTagsByClicksLast7d(tagClicksRatio);
            prefs.setPreferredTagsByWatchTimeLast7d(tagWatchTimeRatio);
        }
    }
}
