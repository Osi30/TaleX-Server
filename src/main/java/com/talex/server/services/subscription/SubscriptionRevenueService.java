package com.talex.server.services.subscription;

import com.talex.server.entities.creator.RevenueTransaction;

import java.util.List;

public interface SubscriptionRevenueService {
    List<RevenueTransaction> processAndDistributePremiumRevenue(String monthYear, boolean isDemo);}