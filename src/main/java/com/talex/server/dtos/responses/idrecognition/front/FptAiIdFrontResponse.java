package com.talex.server.dtos.responses.idrecognition.front;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class FptAiIdFrontResponse {
    @JsonProperty("errorCode")
    private int errorCode;
    @JsonProperty("errorMessage")
    private String errorMessage;
    private List<FrontData> data;
}
