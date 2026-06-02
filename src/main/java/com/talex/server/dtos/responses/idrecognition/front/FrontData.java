package com.talex.server.dtos.responses.idrecognition.front;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FrontData {
    private String id;
    @JsonProperty("id_prob")
    private String idProb;

    private String name;
    @JsonProperty("name_prob")
    private String nameProb;

    private String dob;
    @JsonProperty("dob_prob")
    private String dobProb;

    private String sex;
    @JsonProperty("sex_prob")
    private String sexProb;

    private String nationality;
    @JsonProperty("nationality_prob")
    private String nationalityProb;

    private String home;
    @JsonProperty("home_prob")
    private String homeProb;

    private String address;
    @JsonProperty("address_prob")
    private String addressProb;

    @JsonProperty("address_entities")
    private AddressEntities addressEntities;

    private String doe;
    @JsonProperty("doe_prob")
    private String doeProb;

    @JsonProperty("overall_score")
    private String overallScore;

    @JsonProperty("number_of_name_lines")
    private String numberOfNameLines;

    @JsonProperty("type_new")
    private String typeNew;

    private String type;
}
