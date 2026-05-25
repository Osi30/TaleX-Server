package com.talex.server.dtos.responses.liveness;

import lombok.Data;

@Data
public class FaceMatchData {
    private String code;
    private String message;
    private String isMatch;
    private String similarity;
    private String warning;
}
