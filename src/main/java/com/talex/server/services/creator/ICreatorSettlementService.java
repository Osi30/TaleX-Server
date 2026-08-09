package com.talex.server.services.creator;

import com.talex.server.entities.creator.CreatorMonthlySettlement;

import java.util.List;

public interface ICreatorSettlementService {
    List<CreatorMonthlySettlement> processMonthlySettlement(boolean isDemo);
}