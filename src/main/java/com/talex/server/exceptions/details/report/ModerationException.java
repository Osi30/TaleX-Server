package com.talex.server.exceptions.details.report;

import com.talex.server.exceptions.codes.report.ModerationErrorCode;
import lombok.Getter;

@Getter
public class ModerationException extends RuntimeException {
  private final com.talex.server.exceptions.codes.report.ModerationErrorCode errorCode;

  public ModerationException(ModerationErrorCode errorCode) {
    super(errorCode.getDefaultMessage());
    this.errorCode = errorCode;
  }

  public ModerationException(ModerationErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }
}