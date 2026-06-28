package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ContentPipelineErrorCode {
    KAFKA_SEND_FAILED(6001, HttpStatus.INTERNAL_SERVER_ERROR, "Pipeline job dispatch failed"),
    MEDIA_NOT_FOUND(6002, HttpStatus.NOT_FOUND, "Media not found for pipeline processing"),
    COPYRIGHT_CHECK_FAILED(6003, HttpStatus.INTERNAL_SERVER_ERROR, "Copyright check failed"),
    MODERATION_CHECK_FAILED(6004, HttpStatus.INTERNAL_SERVER_ERROR, "Content moderation failed"),
    PIPELINE_TIMEOUT(6005, HttpStatus.REQUEST_TIMEOUT, "Pipeline processing timed out");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
