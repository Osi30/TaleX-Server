package com.talex.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TermsType {
    CREATOR("Creator terms"),
    GENERAL_TOS("General Terms of Service");

    private final String detail;
}
