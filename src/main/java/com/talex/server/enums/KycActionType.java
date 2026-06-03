package com.talex.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum KycActionType {
    UPDATE_MST("Cập nhật MST"),
    UPDATE_BANK("Cập nhật thông tin ngân hàng"),
    APPROVE_KYC("Duyệt KYC"),
    REJECT_KYC("Từ chối KYC");

    private final String detail;
}
