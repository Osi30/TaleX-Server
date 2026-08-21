package com.talex.server.services.subscription.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.revenue.request.RuleXCalculationRequestDto;
import com.talex.server.dtos.revenue.request.UserStreamRequestDto;
import com.talex.server.dtos.revenue.response.RuleXCalculationResponseDto;
import com.talex.server.dtos.revenue.response.UserAllocationDto;
import com.talex.server.dtos.subscription.dtos.SubscriptionStatRawData;
import com.talex.server.dtos.subscription.response.SubscriptionStatDetailResponseDto;
import com.talex.server.dtos.subscription.response.SubscriptionStatResponseDto;
import com.talex.server.entities.config.CreatorConfig;
import com.talex.server.entities.config.SyncMetadata;
import com.talex.server.entities.subscription.*;
import com.talex.server.enums.SyncType;
import com.talex.server.mappers.subscription.RuleXRequestMapper;
import com.talex.server.records.WatchSessionResponseDto;
import com.talex.server.repositories.config.SyncMetadataRepository;
import com.talex.server.repositories.interaction.WatchSessionRepository;
import com.talex.server.repositories.subscription.SubscriptionResultRepository;
import com.talex.server.repositories.subscription.SubscriptionStatRepository;
import com.talex.server.services.config.CreatorConfigService;
import com.talex.server.services.subscription.SubscriptionStatService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionStatServiceImpl implements SubscriptionStatService {
    private final CreatorConfigService configService;
    private final SubscriptionResultRepository subscriptionResultRepository;
    private final SyncMetadataRepository syncMetadataRepository;
    private final WatchSessionRepository watchSessionRepository;
    private final SubscriptionStatRepository subscriptionStatRepository;
    private final EntityManager entityManager;
    private final RuleXRequestMapper ruleXRequestMapper;

    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int MAX_BINARY_SEARCH_ITERATIONS = 100;
    private static final String KEY_SEPARATOR = "::";

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<SubscriptionStatResponseDto> getStatsByAccountSubscriptionId(
            String accountSubscriptionId, int page, int pageSize) {

        int validPage = Math.max(page, 1);
        int validPageSize = pageSize < 1 ? 20 : pageSize;
        Pageable pageable = PageRequest.of(validPage - 1, validPageSize, Sort.by(Sort.Direction.DESC, "views"));

        Page<Object[]> pageResult = subscriptionStatRepository.findStatsDetailsByAccountSubId(
                accountSubscriptionId, pageable);

        List<SubscriptionStatResponseDto> content = pageResult.stream()
                .map(row -> SubscriptionStatResponseDto.builder()
                        .id(Objects.toString(row[0], null))
                        .monthYear(Objects.toString(row[1], null))
                        .creatorId(Objects.toString(row[2], null))
                        .creatorEmail(Objects.toString(row[3], null))
                        .episodeId(Objects.toString(row[4], null))
                        .episodeNumber(row[5] != null ? ((Number) row[5]).intValue() : null)
                        .seriesId(Objects.toString(row[6], null))
                        .seriesTitle(Objects.toString(row[7], null))
                        .views(row[8] != null ? ((Number) row[8]).longValue() : 0L)
                        .build())
                .toList();

        return BasePageResponse.<SubscriptionStatResponseDto>builder()
                .content(content)
                .pageNumber(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .isFirst(pageResult.isFirst())
                .isLast(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional
    public int processSubscriptionStats() {
        // 1. Lấy mốc thời gian quét gần nhất từ SyncMetadata
        SyncMetadata syncMetadata = syncMetadataRepository.findById(SyncType.SUBSCRIPTION_STAT)
                .orElse(SyncMetadata.builder()
                        .syncType(SyncType.SUBSCRIPTION_STAT)
                        .lastSyncTime(null)
                        .build());

        LocalDateTime lastSyncLocalDateTime = null;
        if (syncMetadata.getLastSyncTime() != null) {
            lastSyncLocalDateTime = LocalDateTime.ofInstant(syncMetadata.getLastSyncTime(), ZoneId.systemDefault());
        }

        Instant currentSyncTime = Instant.now();

        // 2. Lấy danh sách watch session thỏa mãn thời lượng và thời gian chưa quét
        List<WatchSessionResponseDto> validSessions = watchSessionRepository
                .findSessionsByMinWatchDurationAndStartTime(5.0, lastSyncLocalDateTime);

        if (validSessions.isEmpty()) {
            log.info("No new valid watch sessions found since last sync: {}", lastSyncLocalDateTime);
            // Cập nhật lại thời gian sync hiện tại ngay cả khi không có record
            syncMetadata.setLastSyncTime(currentSyncTime);
            syncMetadataRepository.save(syncMetadata);
            return 0;
        }

        int processedCount = 0;

        // 3. Duyệt và upsert từng session
        for (WatchSessionResponseDto session : validSessions) {
            if (session.accountId() == null) {
                continue;
            }

            try {
                upsertSubscriptionStat(
                        session.accountId(),
                        session.creatorId(),
                        session.episodeId(),
                        session.startTime()
                );
                processedCount++;
            } catch (Exception e) {
                log.error("Error processing subscription stat for watchSessionId: {}", session.watchSessionId(), e);
            }
        }

        // 4. Cập nhật thời gian quét mới vào SyncMetadata
        syncMetadata.setLastSyncTime(currentSyncTime);
        syncMetadataRepository.save(syncMetadata);

        log.info("Completed processing subscription stats. Processed {} valid sessions.", processedCount);
        return processedCount;
    }

    @Override
    @Transactional
    public void upsertSubscriptionStat(UUID accountId, String creatorId, String episodeId, LocalDateTime startTime) {
        if (accountId == null || creatorId == null || episodeId == null || startTime == null) {
            return;
        }

        // 1. Kiểm tra startTime có nằm trong khoảng gia hạn hợp lệ của AccountSub
        String activeSubId = subscriptionStatRepository.findActiveAccountSubId(accountId, startTime)
                .orElse(null);

        if (activeSubId == null) {
            return;
        }

        // 2. Lấy monthYear (YYYY-MM)
        String monthYear = startTime.format(MONTH_YEAR_FORMATTER);

        // 3. Thử atomic update (+1 view) kèm theo episodeId
        int rowsUpdated = subscriptionStatRepository.incrementViews(activeSubId, creatorId, episodeId, monthYear);

        // 4. Nếu chưa có record thì tạo mới
        if (rowsUpdated == 0) {
            AccountSubscription subRef = entityManager.getReference(AccountSubscription.class, activeSubId);

            SubscriptionStat newStat = SubscriptionStat.builder()
                    .accountSubscription(subRef)
                    .creatorId(creatorId)
                    .episodeId(episodeId)
                    .monthYear(monthYear)
                    .views(1L)
                    .build();

            subscriptionStatRepository.save(newStat);
        }
    }

    @Override
    public RuleXCalculationResponseDto calculateRuleX(RuleXCalculationRequestDto request) {
        double subscriptionFee = request.getSubscriptionFee() != null ? request.getSubscriptionFee() : 1.0;
        List<UserStreamRequestDto> users = request.getUsers();

        if (users == null || users.isEmpty()) {
            throw new IllegalArgumentException("Danh sách người dùng không được rỗng");
        }

        // Bước 1: Chuẩn hóa dữ liệu đầu vào & tính tổng lượt nghe từng User
        List<Long> vList = normalizeAndCalculateUserStreams(users);

        // Bước 2: Giải thuật Binary Search tìm Gamma (γ)
        double alpha = request.getAlpha();
        double targetBudgetRatio = alpha * users.size();
        double gamma = solveGamma(vList, targetBudgetRatio);

        // Bước 3: Phân bổ ngân sách & Làm tròn số dư lớn nhất ở cấp độ Episode
        return processUserAllocations(users, gamma, subscriptionFee, alpha);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleXCalculationRequestDto> getRuleXRequestFromStats(String monthYear) {
        // 1. Tính toán khoảng thời gian trong tháng
        YearMonth yearMonth = YearMonth.parse(monthYear, MONTH_YEAR_FORMATTER);
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999_999_999);

        // 2. Lấy dữ liệu thô từ DB
        List<SubscriptionStatRawData> rawStats = subscriptionStatRepository
                .findGroupedStatsWithOrderDetailsByMonthYear(startOfMonth, endOfMonth);

        // 3. Lấy cấu hình hệ thống
        CreatorConfig config = configService.getConfigEntity();

        // 4. Ủy quyền toàn bộ việc gom nhóm và tạo DTO cho Component chuyên trách
        return ruleXRequestMapper.aggregate(rawStats, config.getBasePremiumShare());
    }

    @Override
    @Transactional
    public List<SubscriptionResult> calculateAndSaveRevenue(String monthYear, boolean isDemo) {
        // 1. Tự động lấy danh sách RuleXCalculationRequestDto đã gom nhóm theo (Giá thực tế, Thời hạn)
        List<RuleXCalculationRequestDto> requestDtos = getRuleXRequestFromStats(monthYear);

        if (requestDtos == null || requestDtos.isEmpty()) {
            log.info("No subscription stats found for calculation in monthYear: {}", monthYear);
            return Collections.emptyList();
        }

        List<SubscriptionResult> results = new ArrayList<>();

        // 2. Duyệt qua từng nhóm DTO để tính toán Rule X
        for (RuleXCalculationRequestDto requestDto : requestDtos) {
            if (requestDto.getUsers() == null || requestDto.getUsers().isEmpty()) {
                continue;
            }

            // Thực hiện giải thuật tính toán Rule X cho nhóm hiện tại
            RuleXCalculationResponseDto response = calculateRuleX(requestDto);

            // Trích xuất mapping episodeId -> creatorId từ requestDto
            Map<String, String> episodeToCreatorMap = new HashMap<>();
            for (UserStreamRequestDto user : requestDto.getUsers()) {
                if (user.getArtistEpisodeStreams() != null) {
                    user.getArtistEpisodeStreams().forEach((creatorId, epMap) -> {
                        if (epMap != null) {
                            epMap.keySet().forEach(episodeId -> episodeToCreatorMap.put(episodeId, creatorId));
                        }
                    });
                }
            }

            // Tạo Entity SubscriptionResult cho nhóm này
            SubscriptionResult resultEntity = SubscriptionResult.builder()
                    .alpha(requestDto.getAlpha())
                    .totalBudget(response.getTotalBudget())
                    .gamma(response.getGamma())
                    .subscriptionFee(requestDto.getSubscriptionFee())
                    .targetBudget(response.getTargetBudget())
                    .calculatedBudget(response.getCalculatedBudget())
                    .monthYear(monthYear)
                    .revenueLogs(new ArrayList<>())
                    .build();

            // Chuyển đổi episodePayouts thành danh sách SubscriptionRevenueLog
            if (response.getEpisodePayouts() != null) {
                response.getEpisodePayouts().forEach((episodeId, revenue) -> {
                    String creatorId = episodeToCreatorMap.getOrDefault(episodeId, "unknown");

                    SubscriptionRevenueLog logEntity = SubscriptionRevenueLog.builder()
                            .subscriptionResult(resultEntity)
                            .episodeId(episodeId)
                            .creatorId(creatorId)
                            .revenue(revenue)
                            .monthYear(monthYear)
                            .build();

                    resultEntity.getRevenueLogs().add(logEntity);
                });
            }

            results.add(resultEntity);
        }

        // 3. Lưu toàn bộ các kết quả vào DB nếu không phải chế độ Demo
        if (!isDemo && !results.isEmpty()) {
            subscriptionResultRepository.saveAll(results);
            log.info("Successfully saved {} subscription results for monthYear: {}", results.size(), monthYear);
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<SubscriptionStatDetailResponseDto> getDetailedStatsByAccountSubscriptionId(
            String accountSubscriptionId, int page, int pageSize) {

        int validPage = Math.max(page, 1);
        int validPageSize = pageSize < 1 ? 20 : pageSize;
        Pageable pageable = PageRequest.of(validPage - 1, validPageSize, Sort.by(Sort.Direction.DESC, "views"));

        Page<Object[]> pageResult = subscriptionStatRepository.findDetailedStatsByAccountSubscriptionId(
                accountSubscriptionId, pageable);

        List<SubscriptionStatDetailResponseDto> content = pageResult.stream()
                .map(row -> SubscriptionStatDetailResponseDto.builder()
                        .id((String) row[0])
                        .monthYear((String) row[1])
                        .creatorId((String) row[2])
                        .creatorUsername((String) row[3])
                        .creatorAvatarUrl((String) row[4])
                        .episodeId((String) row[5])
                        .episodeTitle((String) row[6])
                        .episodeNumber(row[7] != null ? ((Number) row[7]).intValue() : null)
                        .seriesId((String) row[8])
                        .seriesTitle((String) row[9])
                        .coverUrl((String) row[10])
                        .bannerUrl((String) row[11])
                        .views(row[12] != null ? ((Number) row[12]).longValue() : 0L)
                        .accountSubscriptionId((String) row[13])
                        .build())
                .toList();

        return BasePageResponse.<SubscriptionStatDetailResponseDto>builder()
                .content(content)
                .pageNumber(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .isFirst(pageResult.isFirst())
                .isLast(pageResult.isLast())
                .build();
    }

    // HELPER METHODS

    /**
     * Chuẩn hóa cấu trúc stream và tính tổng số stream (v_u) cho từng User
     */
    private List<Long> normalizeAndCalculateUserStreams(List<UserStreamRequestDto> users) {
        List<Long> vList = new ArrayList<>();

        for (UserStreamRequestDto user : users) {
            Map<String, Map<String, Long>> normalizedStreams = extractEpisodeStreams(user);
            user.setArtistEpisodeStreams(normalizedStreams);

            long totalStreams = 0;
            for (Map<String, Long> epMap : normalizedStreams.values()) {
                for (Long count : epMap.values()) {
                    totalStreams += (count != null ? count : 0);
                }
            }
            user.setTotalStreams(totalStreams);
            vList.add(totalStreams);
        }
        return vList;
    }

    /**
     * Đảm bảo tương thích ngược: Tự chuyển artistStreams phẳng thành artistEpisodeStreams
     */
    private Map<String, Map<String, Long>> extractEpisodeStreams(UserStreamRequestDto user) {
        if (user.getArtistEpisodeStreams() != null && !user.getArtistEpisodeStreams().isEmpty()) {
            return user.getArtistEpisodeStreams();
        }

        Map<String, Map<String, Long>> result = new HashMap<>();
        if (user.getArtistStreams() != null) {
            for (Map.Entry<String, Long> entry : user.getArtistStreams().entrySet()) {
                Map<String, Long> epMap = new HashMap<>();
                epMap.put("ep_default", entry.getValue());
                result.put(entry.getKey(), epMap);
            }
        }
        return result;
    }

    /**
     * Tìm Gamma (γ) thỏa mãn tổng min(1.0, gamma * v_u) = targetBudgetRatio
     */
    private double solveGamma(List<Long> vList, double targetBudgetRatio) {
        double low = 0.0;
        double high = 1.0;

        while (calculateSumMin(high, vList) < targetBudgetRatio && high < 1e6) {
            high *= 2.0;
        }

        double gamma = 0.0;
        for (int i = 0; i < MAX_BINARY_SEARCH_ITERATIONS; i++) {
            gamma = (low + high) / 2.0;
            double currentSum = calculateSumMin(gamma, vList);

            if (currentSum < targetBudgetRatio) {
                low = gamma;
            } else {
                high = gamma;
            }
        }
        return gamma;
    }

    private double calculateSumMin(double gamma, List<Long> vList) {
        double sum = 0.0;
        for (Long v_u : vList) {
            sum += Math.min(1.0, gamma * v_u);
        }
        return sum;
    }

    /**
     * Xử lý tính toán Payout cho từng User và gom kết quả toàn cục
     */
    private RuleXCalculationResponseDto processUserAllocations(List<UserStreamRequestDto> users,
                                                               double gamma,
                                                               double subscriptionFee,
                                                               double alpha) {
        Map<String, Double> globalArtistPayouts = new HashMap<>();
        Map<String, Double> globalEpisodePayouts = new HashMap<>();
        List<UserAllocationDto> userAllocations = new ArrayList<>();
        double totalCalculatedBudget = 0.0;

        for (UserStreamRequestDto user : users) {
            UserAllocationDto allocation = calculateSingleUserAllocation(user, gamma, subscriptionFee);

            totalCalculatedBudget += allocation.getAllocatedAmount();
            userAllocations.add(allocation);

            // Gom tổng Payout theo Artist
            if (allocation.getArtistPayouts() != null) {
                allocation.getArtistPayouts().forEach((artistId, amount) ->
                        globalArtistPayouts.merge(artistId, amount, Double::sum));
            }

            // Gom tổng Payout theo Episode
            if (allocation.getEpisodePayouts() != null) {
                allocation.getEpisodePayouts().forEach((artistId, epMap) ->
                        epMap.forEach((epId, amount) -> globalEpisodePayouts.merge(epId, amount, Double::sum)));
            }
        }

        return RuleXCalculationResponseDto.builder()
                .gamma(gamma)
                .totalBudget(users.size() * subscriptionFee)
                .targetBudget(alpha * users.size() * subscriptionFee)
                .calculatedBudget(totalCalculatedBudget)
                .artistPayouts(globalArtistPayouts)
                .episodePayouts(globalEpisodePayouts)
                .userAllocations(userAllocations)
                .build();
    }

    /**
     * Tính toán & Cân bằng số dư lớn nhất trên từng Episode cho 1 User duy nhất
     */
    private UserAllocationDto calculateSingleUserAllocation(UserStreamRequestDto user, double gamma, double subscriptionFee) {
        long v_u = user.getTotalStreams();
        double effectiveWeight = Math.min(1.0, gamma * v_u);
        double perStreamWeight = v_u > 0 ? Math.min(1.0 / v_u, gamma) : 0.0;
        double userAllocatedAmount = effectiveWeight * subscriptionFee;

        // 1. Phẳng hóa Map (Composite Key: "artistId::episodeId") để tính tiền thô
        Map<String, Double> flatRawPayouts = new HashMap<>();
        Map<String, Map<String, Long>> streamData = user.getArtistEpisodeStreams();

        if (streamData != null) {
            for (Map.Entry<String, Map<String, Long>> artistEntry : streamData.entrySet()) {
                String artistId = artistEntry.getKey();
                for (Map.Entry<String, Long> epEntry : artistEntry.getValue().entrySet()) {
                    String epId = epEntry.getKey();
                    long streamCount = epEntry.getValue();

                    double rawPayout = streamCount * perStreamWeight * subscriptionFee;
                    flatRawPayouts.put(artistId + KEY_SEPARATOR + epId, rawPayout);
                }
            }
        }

        // 2. Áp dụng Thuật toán Số dư lớn nhất lên tập phẳng
        Map<String, Double> flatReconciledPayouts = applyLargestRemainderMethod(flatRawPayouts, userAllocatedAmount);

        // 3. Giải nén (Unflatten) kết quả về lại cấu trúc Episode và Artist
        Map<String, Map<String, Double>> userEpisodePayouts = new HashMap<>();
        Map<String, Double> userArtistPayouts = new HashMap<>();

        flatReconciledPayouts.forEach((compositeKey, amount) -> {
            String[] parts = compositeKey.split(KEY_SEPARATOR, 2);
            String artistId = parts[0];
            String epId = parts[1];

            userEpisodePayouts.computeIfAbsent(artistId, k -> new HashMap<>()).put(epId, amount);
            userArtistPayouts.merge(artistId, amount, Double::sum);
        });

        return UserAllocationDto.builder()
                .userId(user.getUserId())
                .totalStreams(v_u)
                .effectiveWeight(effectiveWeight)
                .perStreamWeight(perStreamWeight)
                .allocatedAmount(userAllocatedAmount)
                .artistPayouts(userArtistPayouts)
                .episodePayouts(userEpisodePayouts)
                .build();
    }

    /**
     * Phương pháp Số dư lớn nhất Dùng chung (Hamilton / Largest Remainder Method)
     */
    private Map<String, Double> applyLargestRemainderMethod(Map<String, Double> rawPayouts, double targetTotalBudget) {
        long targetTotal = Math.round(targetTotalBudget);
        Map<String, Double> roundedPayouts = new HashMap<>();
        Map<String, Double> remainders = new HashMap<>();
        long allocatedSum = 0;

        for (Map.Entry<String, Double> entry : rawPayouts.entrySet()) {
            double rawAmount = entry.getValue();
            long floorAmount = (long) Math.floor(rawAmount);
            double remainder = rawAmount - floorAmount;

            roundedPayouts.put(entry.getKey(), (double) floorAmount);
            remainders.put(entry.getKey(), remainder);
            allocatedSum += floorAmount;
        }

        long missingAmount = targetTotal - allocatedSum;

        List<Map.Entry<String, Double>> sortedRemainders = new ArrayList<>(remainders.entrySet());
        sortedRemainders.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        for (int i = 0; i < missingAmount && i < sortedRemainders.size(); i++) {
            String key = sortedRemainders.get(i).getKey();
            roundedPayouts.put(key, roundedPayouts.get(key) + 1.0);
        }

        return roundedPayouts;
    }
}
