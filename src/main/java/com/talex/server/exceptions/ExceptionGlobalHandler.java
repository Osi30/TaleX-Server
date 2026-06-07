package com.talex.server.exceptions;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.exceptions.codes.AuthErrorCode;
import com.talex.server.exceptions.codes.KycSessionErrorCode;
import com.talex.server.exceptions.details.AuthException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.TypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Locale;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class ExceptionGlobalHandler {
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<BaseResponse> handleAuthException(AuthException ex, WebRequest request) {
        AuthErrorCode errorCode = ex.getErrorCode();
        log.warn("Auth error [{}]: {}", errorCode.getCode(), ex.getMessage());
        BaseResponse exceptionResponse = BaseResponse.builder()
                .message(ex.getMessage())
                .code(errorCode.getCode())
                .data(request.getDescription(false))
                .build();
        return new ResponseEntity<>(exceptionResponse, errorCode.getHttpStatus());
    }

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse> handleMethodArgumentValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Client error [400]: Validation failed - {}", errors);
        return ResponseEntity.badRequest().body(new BaseResponse(400, errors, null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.warn("Client error [400]: Malformed request body");
        return ResponseEntity.badRequest().body(new BaseResponse(400, "Malformed request body", null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<BaseResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String detail = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
        String message = resolveDataIntegrityMessage(detail);
        log.warn("Client error [409]: Data integrity violation - {}", detail);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new BaseResponse(409, message, null));
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<BaseResponse> handleMailError(MailException ex) {
        log.error("Server error: Email service unavailable", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new BaseResponse(503, "Email service unavailable", null));
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse> handleGeneral(Exception ex, WebRequest request) {
        log.error("Server error: {}", ex.getMessage(), ex);
        BaseResponse exceptionResponse = BaseResponse.builder()
                .message("Internal server error")
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .data(request.getDescription(false))
                .build();
        return new ResponseEntity<>(exceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String resolveDataIntegrityMessage(String detail) {
        String normalized = detail == null ? "" : detail.toLowerCase(Locale.ROOT);

        if (normalized.contains("media_status")
                || (normalized.contains("media") && normalized.contains("status") && normalized.contains("check constraint"))) {
            return "Media status is not allowed by the current database constraint. Update the media status constraint to include HLS_PROCESSING and HLS_READY.";
        }

        if (normalized.contains("duplicate key") || normalized.contains("unique constraint")) {
            return "Data conflict - resource already exists";
        }

        if (normalized.contains("not-null constraint") || normalized.contains("null value in column")) {
            return "Data conflict - required database field is missing";
        }

        if (normalized.contains("foreign key constraint")) {
            return "Data conflict - referenced resource does not exist";
        }

        if (normalized.contains("check constraint")) {
            return "Data conflict - value violates a database check constraint";
        }

        return "Data conflict - database constraint violation";
    }
}
