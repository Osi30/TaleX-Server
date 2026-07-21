package com.talex.server.exceptions.details.kyc;

import com.talex.server.exceptions.codes.kyc.FptAIIDRecognitionErrorCode;
import lombok.Getter;

@Getter
public class FptAIIDRecognitionException extends RuntimeException {
    private final FptAIIDRecognitionErrorCode errorCode;

    public FptAIIDRecognitionException(FptAIIDRecognitionErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public FptAIIDRecognitionException(FptAIIDRecognitionErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public FptAIIDRecognitionException(FptAIIDRecognitionErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
