package com.talex.server.records;

import com.fasterxml.jackson.databind.JsonNode;

public record EKycResult(boolean isSuccess, String message, JsonNode rawResponse) {
}
