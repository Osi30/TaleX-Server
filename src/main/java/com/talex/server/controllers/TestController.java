package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.entities.series.EpisodeUnlockedContent;
import com.talex.server.entities.transaction.Order;
import com.talex.server.entities.Account;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.repositories.AccountRepository;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.EpisodeUnlockedContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Tag(name = "Test", description = "API dùng để test các logic mock")
public class TestController {

    private final EpisodeUnlockedContentService episodeUnlockedContentService;
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/api/v1/test/orders/mock")
    @Operation(summary = "Tạo mock Order", description = "Lưu một order vào DB với số tiền là 0 để dùng cho bước tạo unlocked content")
    public ResponseEntity<BaseResponse> createMockOrder(
            @RequestBody MockOrderRequest request,
            @CurrentAccountId UUID accountId) {
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        
        Order mockOrder = Order.builder()
                .account(account)
                .itemType(request.getItemType())
                .itemId(request.getItemId())
                .totalAmount(BigDecimal.ZERO)
                .fiatAmount(BigDecimal.ZERO)
                .coinAmount(0L)
                .status(OrderStatus.COMPLETED)
                .build();
                
        mockOrder = orderRepository.save(mockOrder);
        
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Mock order created successfully in DB")
                .data(new MockOrderResponse(mockOrder.getOrderId(), request.getItemId(), request.getItemType()))
                .build());
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/api/v1/test/unlocked-contents/mock")
    @Operation(summary = "Test tạo EpisodeUnlockedContent", description = "Tạo các record EpisodeUnlockedContent dựa trên order mock")
    public ResponseEntity<BaseResponse> testUnlockContent(
            @RequestBody UnlockContentRequest request,
            @CurrentAccountId UUID accountId) {
        
        List<EpisodeUnlockedContent> unlockedContents = episodeUnlockedContentService.createFromOrder(
                request.getOrderId(),
                request.getItemId(),
                request.getItemType(),
                accountId
        );

        // Convert entities to simple representation for the response
        List<Object> responseData = unlockedContents.stream().map(uc -> 
            new UnlockedContentDto(
                    uc.getId(), 
                    uc.getAccount().getAccountId(), 
                    uc.getEpisode().getEpisodeId(), 
                    uc.getOrderId(), 
                    uc.getPurchasePriceVnd(), 
                    uc.getUnlockMethod()
            )
        ).collect(Collectors.toList());

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Unlocked contents created: " + unlockedContents.size())
                .data(responseData)
                .build());
    }

    @Data
    public static class MockOrderRequest {
        private String itemId;
        private String itemType; // EPISODE or COMBO
    }

    @Data
    @RequiredArgsConstructor
    public static class MockOrderResponse {
        private final String orderId;
        private final String itemId;
        private final String itemType;
    }

    @Data
    public static class UnlockContentRequest {
        private String orderId;
        private String itemId;
        private String itemType; // EPISODE or COMBO
    }

    @Data
    @RequiredArgsConstructor
    public static class UnlockedContentDto {
        private final UUID id;
        private final UUID accountId;
        private final String episodeId;
        private final String orderId;
        private final Long purchasePriceVnd;
        private final String unlockMethod;
    }
}
