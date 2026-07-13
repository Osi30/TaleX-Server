package com.talex.server.exceptions.details;

import com.talex.server.exceptions.codes.AdminAccountErrorCode;
import lombok.Getter;

@Getter
public class AdminAccountException extends RuntimeException {
    private final AdminAccountErrorCode errorCode;

    public AdminAccountException(AdminAccountErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public AdminAccountException(AdminAccountErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
