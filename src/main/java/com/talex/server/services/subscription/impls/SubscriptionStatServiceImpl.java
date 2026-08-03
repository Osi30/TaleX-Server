package com.talex.server.services.subscription.impls;

import com.talex.server.dtos.revenue.request.RuleXCalculationRequestDto;
import com.talex.server.dtos.revenue.request.UserStreamRequestDto;
import com.talex.server.dtos.revenue.response.RuleXCalculationResponseDto;
import com.talex.server.dtos.revenue.response.UserAllocationDto;
import com.talex.server.entities.subscription.AccountSubscription;
import com.talex.server.entities.subscription.SubscriptionStat;
import com.talex.server.repositories.subscription.SubscriptionStatRepository;
import com.talex.server.services.subscription.SubscriptionStatService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionStatServiceImpl implements SubscriptionStatService {
    private final SubscriptionStatRepository subscriptionStatRepository;
    private final EntityManager entityManager;

    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int MAX_BINARY_SEARCH_ITERATIONS = 100;
    private static final String KEY_SEPARATOR = "::";

    @Transactional
    public void upsertSubscriptionStat(UUID accountId, String creatorId, LocalDateTime startTime) {
        if (accountId == null || creatorId == null || startTime == null) {
            return;
        }

        // 1. Kiểm tra startTime có nằm trong khoảng start_time và end_time của AccountSub hay không
        String activeSubId = subscriptionStatRepository.findActiveAccountSubId(accountId, startTime)
                .orElse(null);

        if (activeSubId == null) {
            // Không nằm trong thời gian gia hạn subscription hợp lệ
            return;
        }

        // 2. Lấy monthYear (định dạng "YYYY-MM") từ startTime
        String monthYear = startTime.format(MONTH_YEAR_FORMATTER);

        // 3. Thử atomic update (+1 view)
        int rowsUpdated = subscriptionStatRepository.incrementViews(activeSubId, creatorId, monthYear);

        // 4. Nếu chưa tồn tại record trong tháng đó, tiến hành INSERT
        if (rowsUpdated == 0) {
            AccountSubscription subRef = entityManager.getReference(AccountSubscription.class, activeSubId);

            SubscriptionStat newStat = SubscriptionStat.builder()
                    .accountSubscription(subRef)
                    .creatorId(creatorId)
                    .monthYear(monthYear)
                    .views(1L)
                    .build();

            subscriptionStatRepository.save(newStat);
        }
    }

    @Override
    public RuleXCalculationResponseDto calculateRuleX(RuleXCalculationRequestDto request) {
        double alpha = request.getAlpha() != null ? request.getAlpha() : 1.0;
        double subscriptionFee = request.getSubscriptionFee() != null ? request.getSubscriptionFee() : 1.0;
        List<UserStreamRequestDto> users = request.getUsers();

        if (users == null || users.isEmpty()) {
            throw new IllegalArgumentException("Danh sách người dùng không được rỗng");
        }

        // Bước 1: Chuẩn hóa dữ liệu đầu vào & tính tổng lượt nghe từng User
        List<Long> vList = normalizeAndCalculateUserStreams(users);

        // Bước 2: Giải thuật Binary Search tìm Gamma (γ)
        double targetBudgetRatio = alpha * users.size();
        double gamma = solveGamma(vList, targetBudgetRatio);

        // Bước 3: Phân bổ ngân sách & Làm tròn số dư lớn nhất ở cấp độ Episode
        return processUserAllocations(users, gamma, subscriptionFee, alpha);
    }

    // =========================================================================
    // HELPER METHODS (CLEAN CODE / MODULAR)
    // =========================================================================

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
