package com.talex.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StepType {
    FRONT_ID ("CCCD mặt trước"),
    BACK_ID ("CCCD mặt sau"),
    LIVENESS_VIDEO ("Kiểm tra tính sống"),
    FACE_MATCH ("Kiểm tra khớp khuôn mặt");

    private final String detail;
}
