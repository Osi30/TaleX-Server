package com.talex.server.dtos.responses.coin;

import lombok.Data;

import java.util.List;

@Data
public class AdmobKeyResponse {
    private List<AdmobKey> keys;
}
