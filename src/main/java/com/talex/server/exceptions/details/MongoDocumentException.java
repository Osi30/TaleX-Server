package com.talex.server.exceptions.details;

import com.talex.server.exceptions.codes.MongoDocumentErrorCode;
import lombok.Getter;

@Getter
public class MongoDocumentException extends RuntimeException {
    private final MongoDocumentErrorCode errorCode;

    public MongoDocumentException(MongoDocumentErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public MongoDocumentException(MongoDocumentErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public MongoDocumentException(MongoDocumentErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
