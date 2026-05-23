package com.talex.server.dtos.responses.idrecognition.front;

import lombok.Data;

@Data
public class AddressEntities {
    private String province;
    private String district;
    private String ward;
    private String street;
}
