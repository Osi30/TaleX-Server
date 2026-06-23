package com.talex.server.exceptions.details;

import com.talex.server.exceptions.codes.InteractionErrorCode;
import lombok.Getter;

@Getter
public class InteractionException extends RuntimeException {
  private final InteractionErrorCode errorCode;

  public InteractionException(InteractionErrorCode errorCode) {
    super(errorCode.getDefaultMessage());
    this.errorCode = errorCode;
  }

  public InteractionException(InteractionErrorCode errorCode, String customMessage) {
    super(customMessage);
    this.errorCode = errorCode;
  }}
