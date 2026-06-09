package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.AccountSubscriptionRequestDto;
import com.talex.server.dtos.requests.filters.AccountSubscriptionFilterRequestDto;
import com.talex.server.dtos.responses.AccountSubscriptionResponseDto;
import com.talex.server.services.IAccountSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/account-subscriptions")
@RequiredArgsConstructor
public class AccountSubscriptionController {
    private final IAccountSubscriptionService accountSubscriptionService;

    @PostMapping
    public ResponseEntity<BaseResponse> create(
            @RequestBody AccountSubscriptionRequestDto request,
            @CurrentAccountId UUID accountId
    ) {
        request.setAccountId(accountId);
        AccountSubscriptionResponseDto response = accountSubscriptionService.createAccountSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Account subscription created")
                        .data(response)
                        .build());
    }

    @GetMapping
    public ResponseEntity<BaseResponse> list(
            @RequestParam(required = false) Map<String, Object> criteria,
            @RequestParam(required = false) String[] statuses,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        AccountSubscriptionFilterRequestDto filterRequest = AccountSubscriptionFilterRequestDto.builder()
                .criteria(criteria)
                .statuses(statuses)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .page(page)
                .pageSize(pageSize)
                .build();

        BasePageResponse<AccountSubscriptionResponseDto> pageResponse = accountSubscriptionService
                .filterAndSortAccountSubscriptions(filterRequest);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(pageResponse)
                .build());
    }

    @GetMapping("/{accountSubscriptionId}")
    public ResponseEntity<BaseResponse> getById(@PathVariable String accountSubscriptionId) {
        AccountSubscriptionResponseDto response = accountSubscriptionService
                .getAccountSubscriptionById(accountSubscriptionId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }

    @PutMapping("/{accountSubscriptionId}")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String accountSubscriptionId,
            @Valid @RequestBody AccountSubscriptionRequestDto request) {
        AccountSubscriptionResponseDto response = accountSubscriptionService
                .updateAccountSubscription(accountSubscriptionId, request);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Account subscription updated")
                .data(response)
                .build());
    }

    @DeleteMapping("/{accountSubscriptionId}")
    public ResponseEntity<BaseResponse> delete(@PathVariable String accountSubscriptionId) {
        accountSubscriptionService.deleteAccountSubscription(accountSubscriptionId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Account subscription deleted")
                .data(null)
                .build());
    }
}
