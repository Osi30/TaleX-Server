package com.talex.server.dtos.responses.idrecognition.back;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class BackData {
    private String features;
    @JsonProperty("features_prob")
    private Double featuresProb;

    @JsonProperty("issue_date")
    private String issueDate;
    @JsonProperty("issue_date_prob")
    private Double issueDateProb;

    private List<String> mrz;
    @JsonProperty("mrz_prob")
    private String mrzProb;

    @JsonProperty("overall_score")
    private String overallScore;

    @JsonProperty("issue_loc")
    private String issueLoc;
    @JsonProperty("issue_loc_prob")
    private String issueLocProb;

    @JsonProperty("type_new")
    private String typeNew;

    private String type;

    @JsonProperty("mrz_details")
    private MrzDetails mrzDetails;

    private String pob;
    @JsonProperty("pob_prob")
    private String pobProb;

    private String address;
    @JsonProperty("address_prob")
    private String addressProb;

    private String doe;
    @JsonProperty("doe_prob")
    private String doeProb;
}
