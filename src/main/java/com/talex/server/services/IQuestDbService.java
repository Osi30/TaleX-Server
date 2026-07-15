package com.talex.server.services;

import com.talex.server.dtos.mongo.QuestDbPreferenceResult;
import com.talex.server.dtos.mongo.QuestDbQueryResult;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IQuestDbService {
    CompletableFuture<List<QuestDbQueryResult>> queryInteractionsAsync(Instant startTime, Instant endTime);
    CompletableFuture<List<QuestDbPreferenceResult>> queryPreferencesAsync(Instant startTime, Instant endTime);
}
