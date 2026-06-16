package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.requests.InteractionRequest;
import com.talex.server.repositories.subscription.SubscriptionStatRepository;
import com.talex.server.services.IInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
public class InteractionController {
    private final IInteractionService interactionService;
    private final SubscriptionStatRepository statRepo;

    @PostMapping()
    public ResponseEntity<String> userInteractContent(
            @CurrentAccountId UUID accountId,
            @RequestBody InteractionRequest request
    ) {
        interactionService.processInteraction(accountId, request);
        return ResponseEntity.ok("Tương tác thành công!");
    }

    @GetMapping("")
    public ResponseEntity<String> getInteractions(
            @CurrentAccountId UUID accountId
    ) {
        String subId = statRepo.findActiveAccountSubByAccountId(accountId, LocalDateTime.now());
        return ResponseEntity.ok(subId);
    }

    @GetMapping("/e")
    public ResponseEntity<String> getInteractionsCreator(
            @RequestParam String episodeId
    ) {
        String subId = statRepo.findCreatorIdByEpisodeId(episodeId);
        return ResponseEntity.ok(subId);
    }
}
