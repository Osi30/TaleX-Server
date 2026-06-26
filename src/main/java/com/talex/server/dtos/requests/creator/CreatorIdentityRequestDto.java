package com.talex.server.dtos.requests.creator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorIdentityRequestDto {
    private String idNumber;
    private String fullName;
    private LocalDate dob;
    private String sex;
    private String address;
    private LocalDate doe;
    private String taxId;
}
