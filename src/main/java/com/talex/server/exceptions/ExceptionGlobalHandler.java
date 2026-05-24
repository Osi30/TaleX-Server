package com.talex.server.exceptions;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.exceptions.codes.KycSessionErrorCode;
import com.talex.server.exceptions.details.FptAIIDRecognitionException;
import com.talex.server.exceptions.details.KycSessionException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ExceptionGlobalHandler {
    @ExceptionHandler(FptAIIDRecognitionException.class)
    public ResponseEntity<BaseResponse> handleFptAIIDRecognitionException(FptAIIDRecognitionException ex, WebRequest request) {
        BaseResponse exceptionResponse = BaseResponse.builder()
                .message(ex.getMessage())
                .code(HttpStatus.BAD_REQUEST.value())
                .data(request.getDescription(false))
                .build();
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(KycSessionException.class)
    public ResponseEntity<BaseResponse> handleKYCSessionException(KycSessionException ex, WebRequest request) {
        KycSessionErrorCode errorCode = ex.getErrorCode();

        BaseResponse exceptionResponse = BaseResponse.builder()
                .message(ex.getMessage())
                .code(errorCode.getCode())
                .data(request.getDescription(false))
                .build();

        return new ResponseEntity<>(exceptionResponse, errorCode.getHttpStatus());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<BaseResponse> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(new BaseResponse(413, "File vượt quá giới hạn tối đa cho phép của toàn hệ thống!", null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse> handleValidationException(ConstraintViolationException ex) {
        String errorMsg = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("File không hợp lệ!");

        return ResponseEntity.badRequest().body(new BaseResponse(400, errorMsg, null));
    }
}
