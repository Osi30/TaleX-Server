package com.talex.server.mappers;

import com.talex.server.dtos.requests.creator.PaymentProfileRequestDto;
import com.talex.server.dtos.responses.PaymentProfileResponseDto;
import com.talex.server.entities.creator.PaymentProfile;

public interface IPaymentProfileMapper {
    PaymentProfileResponseDto toResponseDto(PaymentProfile entity);

    PaymentProfile toEntity(PaymentProfileRequestDto dto);

    void updateEntity(PaymentProfileRequestDto dto, PaymentProfile entity);
}
