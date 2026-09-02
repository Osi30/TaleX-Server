package com.talex.server.services.creator;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.creator.PaymentProfileRequestDto;
import com.talex.server.dtos.requests.creator.PaymentProfileVerifiedDto;
import com.talex.server.dtos.requests.filters.PaymentProfileFilterRequestDto;
import com.talex.server.dtos.responses.creator.PaymentProfileResponseDto;
import com.talex.server.entities.creator.PaymentProfile;
import com.talex.server.enums.BankBin;

import java.util.List;
import java.util.UUID;

public interface PaymentProfileService {
    PaymentProfileResponseDto create(UUID accountId, PaymentProfileRequestDto dto);

    PaymentProfileResponseDto getById(String id);

    PaymentProfileResponseDto getPrimaryProfile(UUID accountId);

    PaymentProfileResponseDto getPrimaryProfileByCreatorId(String creatorId);

    List<PaymentProfileResponseDto> getOwnProfiles(UUID accountId);

    PaymentProfileResponseDto update(String id, PaymentProfileRequestDto dto);

    PaymentProfileResponseDto updateVerifiedStatus(String id, PaymentProfileVerifiedDto dto);

    void delete(String id);

    BasePageResponse<PaymentProfileResponseDto> list(PaymentProfileFilterRequestDto filterRequest);

    PaymentProfile findById(String id);

    List<BankBin> getAllBankBins();
}
