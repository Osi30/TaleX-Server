package com.talex.server.dtos.responses.idrecognition;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IdCardOcrResult {
    private String idNumber;
    private String fullName;
    private String dob;
    private String sex;
    private String nationality;
    private String address;
    private String expiryDate;
    private String issueDate;
    private String issuePlace;
    private String personalFeatures;
}
