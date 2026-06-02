package com.talex.server.dtos.responses.idrecognition.back;

import lombok.Data;

import java.util.List;

@Data
public class FptAiIdBackResponse {
    private int errorCode;
    private String errorMessage;
    private List<BackData> data;
}
