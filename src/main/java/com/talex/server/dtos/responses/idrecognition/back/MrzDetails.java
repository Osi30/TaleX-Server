package com.talex.server.dtos.responses.idrecognition.back;

import lombok.Data;

@Data
public class MrzDetails {
    private String id;
    private String name;
    private String doe;
    private String dob;
    private String nationality;
    private String sex;
}
