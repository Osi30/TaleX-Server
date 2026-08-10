package com.talex.server.services.creator;

import com.talex.server.entities.creator.CreatorMonthlySettlement;

import java.util.List;

public interface CreatorSettlementService {
    List<CreatorMonthlySettlement> processMonthlySettlement(boolean isDemo);
}