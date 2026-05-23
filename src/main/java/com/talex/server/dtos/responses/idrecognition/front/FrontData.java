package com.talex.server.dtos.responses.idrecognition.front;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FrontData {
    private String id;
    private String name;
    private String dob;
    private String sex;
    private String nationality;
    private String home;
    private String address;
    @JsonProperty("address_entities")
    private AddressEntities addressEntities;
    private String doe;
    private String type;
}
