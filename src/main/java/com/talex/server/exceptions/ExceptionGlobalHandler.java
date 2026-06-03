package com.talex.server.exceptions;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.exceptions.codes.KycSessionErrorCode;
import com.talex.server.exceptions.details.CreatorException;
import com.talex.server.exceptions.details.CreatorTermsLogException;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.exceptions.details.FptAIIDRecognitionException;
import com.talex.server.exceptions.details.KycSessionException;
import com.talex.server.exceptions.details.KycStepException;
import com.talex.server.exceptions.details.ResourceNotFoundException;
import com.talex.server.exceptions.details.TermVersionException;
import com.talex.server.exceptions.details.CreatorIdentityException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.TypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ExceptionGlobalHandler {
    @ExceptionHandler(ContentModuleException.class)
    public ResponseEntity<BaseResponse> handleContentModuleException(ContentModuleException ex, WebRequest request) {
        BaseResponse exceptionResponse = BaseResponse.builder()
                .message(ex.getMessage())
                .code(ex.getCode())
                .data(request.getDescription(false))
                .build();
        return new ResponseEntity<>(exceptionResponse, ex.getHttpStatus());
    }

    @ExceptionHandler(FptAIIDRecognitionException.class)
    public ResponseEntity<BaseResponse> handleFptAIIDRecognitionException(FptAIIDRecognitionException ex,
            WebRequest request) {
        BaseResponse exceptionResponse = BaseResponse.builder()
                .message(ex.getMessage())
                .code(ex.getErrorCode().getCode())
                .data(request.getDescription(false))
                .build();
        return new ResponseEntity<>(exceptionResponse, ex.getErrorCode().getHttpStatus());
    }

    @ExceptionHandler(CreatorException.class)
    public ResponseEntity<BaseResponse> handleCreatorException(CreatorException ex, WebRequest request) {
        BaseResponse exceptionResponse = BaseResponse.builder()
                .message(ex.getMessage())
                .code(ex.getErrorCode().getCode())
                .data(request.getDescription(false))
                .build();
        return new ResponseEntity<>(exceptionResponse, ex.getErrorCode().getHttpStatus());
    }

    @ExceptionHandler(CreatorTermsLogException.class)
    public ResponseEntity<BaseResponse> handleCreatorTermsLogException(CreatorTermsLogException ex,
            WebRequest request) {
        BaseResponse exceptionResponse = BaseResponse.builder()
                .message(ex.getMessage())
                .code(ex.getErrorCode().getCode())
                .data(request.getDescription(false))
                .build();
        return new ResponseEntity<>(exceptionResponse, ex.getErrorCode().getHttpStatus());
    }

    @ExceptionHandler(KycStepException.class)
    public ResponseEntity<BaseResponse> handleKycStepException(KycStepException ex, WebRequest request) {
        BaseResponse exceptionResponse = BaseResponse.builder()
                .message(ex.getMessage())
                .code(ex.getErrorCode().getCode())
                .data(request.getDescription(false))
                .build();
        return new ResponseEntity<>(exceptionResponse, ex.getErrorCode().getHttpStatus());
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

    @ExceptionHandler(TermVersionException.class)
    public ResponseEntity<BaseResponse> handleTermVersionException(TermVersionException ex, WebRequest request) {
        BaseResponse exceptionResponse = BaseResponse.builder()
                .message(ex.getMessage())
                .code(ex.getErrorCode().getCode())
                .data(request.getDescription(false))
                .build();

        return new ResponseEntity<>(exceptionResponse, ex.getErrorCode().getHttpStatus());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<BaseResponse> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<BaseResponse> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        String message = "Missing required request parameter: " + ex.getParameterName();
        return ResponseEntity.badRequest().body(new BaseResponse(400, message, null));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<BaseResponse> handleMissingRequestPart(MissingServletRequestPartException ex) {
        String message = "Missing required multipart part: " + ex.getRequestPartName();
        return ResponseEntity.badRequest().body(new BaseResponse(400, message, null));
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<BaseResponse> handleTypeMismatch(TypeMismatchException ex) {
        String message = "Invalid parameter type: " + ex.getPropertyName();
        return ResponseEntity.badRequest().body(new BaseResponse(400, message, null));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        BaseResponse exceptionResponse = BaseResponse.builder()
                .message(ex.getMessage())
                .code(HttpStatus.NOT_FOUND.value())
                .data(request.getDescription(false))
                .build();

        return new ResponseEntity<>(exceptionResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CreatorIdentityException.class)
    public ResponseEntity<BaseResponse> handleCreatorIdentityException(CreatorIdentityException ex,
            WebRequest request) {
        BaseResponse exceptionResponse = BaseResponse.builder()
                .message(ex.getMessage())
                .code(ex.getErrorCode().getCode())
                .data(request.getDescription(false))
                .build();

        return new ResponseEntity<>(exceptionResponse, ex.getErrorCode().getHttpStatus());
    }
}
