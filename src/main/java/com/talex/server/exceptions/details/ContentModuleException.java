package com.talex.server.exceptions.details;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ContentModuleException extends RuntimeException {
    private final int code;
    private final HttpStatus httpStatus;

    public ContentModuleException(int code, HttpStatus httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public ContentModuleException(int code, HttpStatus httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public static ContentModuleException badRequest(String message) {
        return new ContentModuleException(4300, HttpStatus.BAD_REQUEST, message);
    }

    public static ContentModuleException notFound(String message) {
        return new ContentModuleException(4404, HttpStatus.NOT_FOUND, message);
    }

    public static ContentModuleException conflict(String message) {
        return new ContentModuleException(4409, HttpStatus.CONFLICT, message);
    }
}
