package com.talex.server.dtos.responses.idrecognition.back;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class FptAiIdBackResponse {
    @JsonProperty("errorCode")
    private int errorCode;
    @JsonProperty("errorMessage")
    private String errorMessage;
    private List<BackData> data;
}
