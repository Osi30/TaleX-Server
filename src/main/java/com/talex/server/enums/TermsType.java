package com.talex.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TermsType {
    CREATOR("Creator terms"),
    CREATOR_VERIFYING_PROCESS("Creator verify process terms"),
    CREATOR_ENABLE_MONETIZATION("Creator enable money terms"),
    GENERAL_TOS("General Terms of Service");

    private final String detail;
}
