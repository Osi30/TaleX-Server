package com.talex.server.services.subscription.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.subscription.response.SubscriptionResultResponseDto;
import com.talex.server.dtos.subscription.response.SubscriptionRevenueLogDetailResponseDto;
import com.talex.server.entities.subscription.SubscriptionResult;
import com.talex.server.exceptions.details.ResourceNotFoundException;
import com.talex.server.repositories.subscription.SubscriptionResultRepository;
import com.talex.server.repositories.subscription.SubscriptionRevenueLogRepository;
import com.talex.server.services.subscription.SubscriptionResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionResultServiceImpl implements SubscriptionResultService {

    private final SubscriptionResultRepository subscriptionResultRepository;
    private final SubscriptionRevenueLogRepository subscriptionRevenueLogRepository;

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResultResponseDto getSubscriptionResultByMonthYear(int year, int month) {
        String monthYear = String.format("%d-%02d", year, month);

        SubscriptionResult result = subscriptionResultRepository.findByMonthYear(monthYear)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy SubscriptionResult cho thời gian: " + monthYear));

        return SubscriptionResultResponseDto.builder()
                .id(result.getId())
                .alpha(result.getAlpha())
                .gamma(result.getGamma())
                .subscriptionFee(result.getSubscriptionFee())
                .totalBudget(result.getTotalBudget())
                .targetBudget(result.getTargetBudget())
                .calculatedBudget(result.getCalculatedBudget())
                .monthYear(result.getMonthYear())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<SubscriptionRevenueLogDetailResponseDto> getRevenueLogsByResultId(
            String subscriptionResultId, int page, int pageSize) {

        int validPage = Math.max(page, 1);
        int validPageSize = pageSize < 1 ? 20 : pageSize;
        Pageable pageable = PageRequest.of(validPage - 1, validPageSize, Sort.by(Sort.Direction.DESC, "revenue"));

        Page<Object[]> pageResult = subscriptionRevenueLogRepository
                .findDetailedLogsBySubscriptionResultId(subscriptionResultId, pageable);

        List<SubscriptionRevenueLogDetailResponseDto> content = pageResult.stream()
                .map(row -> SubscriptionRevenueLogDetailResponseDto.builder()
                        .id((String) row[0])
                        .monthYear((String) row[1])
                        .revenue(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                        .creatorId((String) row[3])
                        .username((String) row[4])
                        .avatarUrl((String) row[5])
                        .episodeId((String) row[6])
                        .episodeTitle((String) row[7])
                        .episodeNumber(row[8] != null ? ((Number) row[8]).intValue() : null)
                        .seriesId((String) row[9])
                        .seriesTitle((String) row[10])
                        .coverUrl((String) row[11])
                        .bannerUrl((String) row[12])
                        .subscriptionResultId((String) row[13])
                        .build())
                .toList();

        return BasePageResponse.<SubscriptionRevenueLogDetailResponseDto>builder()
                .content(content)
                .pageNumber(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .isFirst(pageResult.isFirst())
                .isLast(pageResult.isLast())
                .build();
    }
}