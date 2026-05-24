package com.talex.server.records;

import com.fasterxml.jackson.databind.JsonNode;

public record OcrResult(boolean isSuccess, String message, JsonNode rawResponse) {
}
