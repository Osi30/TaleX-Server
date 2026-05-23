package com.talex.server.exceptions;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.exceptions.details.FptAIIDRecognitionException;
import com.talex.server.exceptions.details.KYCSessionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

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

    @ExceptionHandler(KYCSessionException.class)
    public ResponseEntity<BaseResponse> handleKYCSessionException(KYCSessionException ex, WebRequest request) {
        BaseResponse exceptionResponse = BaseResponse.builder()
                .message(ex.getMessage())
                .code(HttpStatus.BAD_REQUEST.value())
                .data(request.getDescription(false))
                .build();
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }
}
