package com.talex.server.services.statistic;

import com.talex.server.dtos.statistics.StatisticResponseDto;

import java.time.LocalDateTime;

public interface StatisticService {
    StatisticResponseDto getOrderStatistics(LocalDateTime startTime, LocalDateTime endTime);
}