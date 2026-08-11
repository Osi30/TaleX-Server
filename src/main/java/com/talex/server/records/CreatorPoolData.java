package com.talex.server.records;

import java.math.BigDecimal;

public record CreatorPoolData (
        BigDecimal totalAmount,
        BigDecimal taxAmount,
        BigDecimal fiatAmount,
        Long totalSubscriptions
){
}
