package com.talex.server.dtos.responses.creator;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talex.server.enums.creator.PaymentProfileStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProfileResponseDto {
    private String paymentProfileId;
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private Boolean isPrimary;
    private PaymentProfileStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime verifiedAt;

    private String verifiedNote;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private String creatorId;
}
