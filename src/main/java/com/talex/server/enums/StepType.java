package com.talex.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StepType {
    FRONT_ID ("CCCD mặt trước"),
    BACK_ID ("CCCD mặt sau"),
    LIVENESS_FACEMATCH ("Kiểm tra tính sống và khớp khuôn mặt");

    private final String detail;
}
