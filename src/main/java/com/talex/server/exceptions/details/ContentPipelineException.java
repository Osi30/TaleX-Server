package com.talex.server.exceptions.details;

import com.talex.server.exceptions.codes.ContentPipelineErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ContentPipelineException extends RuntimeException {
    private final int code;
    private final HttpStatus httpStatus;

    public ContentPipelineException(ContentPipelineErrorCode errorCode, String detail) {
        super(detail);
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

    public ContentPipelineException(ContentPipelineErrorCode errorCode, String detail, Throwable cause) {
        super(detail, cause);
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }
}
