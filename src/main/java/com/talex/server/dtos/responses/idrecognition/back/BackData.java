package com.talex.server.dtos.responses.idrecognition.back;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BackData {
    private String religion;
    private String ethnicity;
    private String features;
    @JsonProperty("issue_date")
    private String issueDate;
    @JsonProperty("issue_loc")
    private String issueLoc;
    private String type;
}
