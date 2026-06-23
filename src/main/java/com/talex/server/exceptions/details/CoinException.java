package com.talex.server.exceptions.details;

import com.talex.server.exceptions.codes.CoinErrorCode;
import lombok.Getter;

/**
 * Exception chuyên biệt cho module Coin.
 * Được bắt bởi {@link com.talex.server.exceptions.ExceptionGlobalHandler}.
 */
@Getter
public class CoinException extends RuntimeException {

    private final CoinErrorCode errorCode;

    public CoinException(CoinErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public CoinException(CoinErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CoinException(CoinErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
