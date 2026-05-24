package com.talex.server.dtos.responses.idrecognition.back;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BackData {
    private String religion;
    @JsonProperty("religion_prob")
    private Double religionProb;

    private String ethnicity;
    @JsonProperty("ethnicity_prob")
    private Double ethnicityProb;

    private String features;
    @JsonProperty("features_prob")
    private Double featuresProb;

    @JsonProperty("issue_date")
    private String issueDate;
    @JsonProperty("issue_date_prob")
    private Double issueDateProb;

    @JsonProperty("issue_loc")
    private String issueLoc;
    @JsonProperty("issue_loc_prob")
    private Double issueLocProb;

    private String type;
}
