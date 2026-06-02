package com.talex.server.dtos.responses.idrecognition.front;

import lombok.Data;

import java.util.List;

@Data
public class FptAiIdFrontResponse {
    private int errorCode;
    private String errorMessage;
    private List<FrontData> data;
}
