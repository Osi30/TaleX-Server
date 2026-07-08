package com.talex.server.dtos.responses;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talex.server.enums.creator.CreatorIdentityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorIdentityResponseDto {
    private String creatorIdentityId;
    private String creatorId;
    private String idNumber;
    private String fullName;
    private LocalDate dob;
    private String sex;
    private String address;
    private LocalDate doe;
    private CreatorIdentityStatus status;
    private String taxId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
