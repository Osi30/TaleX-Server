package com.talex.server.enums.engagement;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EngagementType {
    BROAD("Phân phối tới người dùng ngẫu nhiên"),
    TARGETED("Phân phối tới người dùng có chọn lọc")
    ;

    private final String detail;
}
