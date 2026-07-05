package com.talex.server.mappers.impl;

import com.talex.server.dtos.requests.creator.PaymentProfileRequestDto;
import com.talex.server.dtos.responses.PaymentProfileResponseDto;
import com.talex.server.entities.creator.PaymentProfile;
import com.talex.server.enums.creator.PaymentProfileStatus;
import com.talex.server.mappers.IPaymentProfileMapper;
import org.springframework.stereotype.Component;

@Component
public class PaymentProfileMapperImpl implements IPaymentProfileMapper {

    @Override
    public PaymentProfileResponseDto toResponseDto(PaymentProfile entity) {
        if (entity == null) {
            return null;
        }

        return PaymentProfileResponseDto.builder()
                .paymentProfileId(entity.getPaymentProfileId())
                .bankCode(entity.getBankCode())
                .accountNumber(entity.getAccountNumber())
                .accountName(entity.getAccountName())
                .isPrimary(entity.getIsPrimary())
                .status(entity.getStatus())
                .verifiedAt(entity.getVerifiedAt())
                .verifiedNote(entity.getVerifiedNote())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .creatorId(entity.getCreator() != null ? entity.getCreator().getCreatorId() : null)
                .build();
    }

    @Override
    public PaymentProfile toEntity(PaymentProfileRequestDto dto) {
        if (dto == null) {
            return null;
        }

        PaymentProfile entity = new PaymentProfile();
        entity.setBankCode(dto.getBankCode());
        entity.setAccountNumber(dto.getAccountNumber());
        entity.setAccountName(dto.getAccountName());

        return entity;
    }

    @Override
    public void updateEntity(PaymentProfileRequestDto dto, PaymentProfile entity) {
        if (dto == null) {
            return;
        }

        boolean isChange = false;

        if (dto.getBankCode() != null) {
            entity.setBankCode(dto.getBankCode());
            isChange = true;
        }
        if (dto.getAccountNumber() != null) {
            entity.setAccountNumber(dto.getAccountNumber());
            isChange = true;
        }
        if (dto.getAccountName() != null) {
            entity.setAccountName(dto.getAccountName());
            isChange = true;
        }

        if (dto.getStatus() != null
                && dto.getStatus().equals(PaymentProfileStatus.CANCELLED)
                && entity.getStatus().equals(PaymentProfileStatus.PENDING)
        ) {
            entity.setStatus(dto.getStatus());
            entity.setIsPrimary(false);
            isChange = false;
        }

        if (isChange) {
            entity.setStatus(PaymentProfileStatus.PENDING);
        }
    }
}
