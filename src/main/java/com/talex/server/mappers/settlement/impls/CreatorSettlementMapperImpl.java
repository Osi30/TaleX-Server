package com.talex.server.mappers.settlement.impls;

import com.talex.server.dtos.settlement.response.CreatorDetailDto;
import com.talex.server.dtos.settlement.response.CreatorSettlementDetailResponseDto;
import com.talex.server.dtos.settlement.response.CreatorSettlementResponseDto;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.CreatorIdentity;
import com.talex.server.entities.creator.CreatorMonthlySettlement;
import com.talex.server.mappers.settlement.CreatorSettlementMapper;
import com.talex.server.mappers.settlement.PayoutTransactionMapper;
import com.talex.server.mappers.settlement.RevenueTransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreatorSettlementMapperImpl implements CreatorSettlementMapper {
    private final RevenueTransactionMapper revenueTransactionMapper;
    private final PayoutTransactionMapper payoutTransactionMapper;

    @Override
    public CreatorSettlementResponseDto toResponseDto(CreatorMonthlySettlement entity) {
        if (entity == null) return null;

        String creatorName = null;
        if (entity.getCreator() != null && entity.getCreator().getAccount() != null) {
            creatorName = entity.getCreator().getAccount().getFullName();
        }

        return CreatorSettlementResponseDto.builder()
                .creatorMonthlySettlementId(entity.getCreatorMonthlySettlementId())
                .settlementMonth(entity.getSettlementMonth())
                .cutoffDate(entity.getCutoffDate())
                .grossAmount(entity.getGrossAmount())
                .totalPenaltyAmount(entity.getTotalPenaltyAmount())
                .taxRate(entity.getTaxRate())
                .taxWithheldAmount(entity.getTaxWithheldAmount())
                .netPayoutAmount(entity.getNetPayoutAmount())
                .status(entity.getStatus())
                .creatorId(entity.getCreator() != null ? entity.getCreator().getCreatorId() : null)
                .creatorName(creatorName)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public CreatorSettlementDetailResponseDto toDetailResponseDto(CreatorMonthlySettlement entity) {
        if (entity == null) return null;

        return CreatorSettlementDetailResponseDto.builder()
                .creatorMonthlySettlementId(entity.getCreatorMonthlySettlementId())
                .settlementMonth(entity.getSettlementMonth())
                .cutoffDate(entity.getCutoffDate())
                .grossAmount(entity.getGrossAmount())
                .totalPenaltyAmount(entity.getTotalPenaltyAmount())
                .taxRate(entity.getTaxRate())
                .taxWithheldAmount(entity.getTaxWithheldAmount())
                .netPayoutAmount(entity.getNetPayoutAmount())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .creatorDetail(mapCreatorDetail(entity.getCreator()))
                .revenueTransactions(revenueTransactionMapper.toListDto(entity.getRevenueTransactions()))
                .payoutTransactions(payoutTransactionMapper.toListDto(entity.getPayoutTransactions()))
                .build();
    }

    private CreatorDetailDto mapCreatorDetail(Creator creator) {
        if (creator == null) return null;

        CreatorDetailDto.CreatorDetailDtoBuilder detailBuilder = CreatorDetailDto.builder()
                .creatorId(creator.getCreatorId())
                .isBanned(creator.getIsBanned());

        CreatorIdentity identity = creator.getCreatorIdentity();
        if (identity != null) {
            detailBuilder.taxId(identity.getTaxId());
            detailBuilder.taxStatus(identity.getStatus());
        }

        Account account = creator.getAccount();
        if (account != null) {
            detailBuilder.accountId(account.getAccountId());
            detailBuilder.username(account.getUsername());
            detailBuilder.email(account.getEmail());
            detailBuilder.accountStatus(account.getStatus());
        }

        return detailBuilder.build();
    }
}
