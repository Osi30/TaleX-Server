package com.talex.server.exceptions.details;

import com.talex.server.exceptions.codes.FptAIIDRecognitionErrorCode;
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
