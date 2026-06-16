package com.talex.server.controllers.subscription;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.subscription.SubscriptionRequestDto;
import com.talex.server.dtos.requests.filters.SubscriptionFilterRequestDto;
import com.talex.server.dtos.responses.subscription.SubscriptionResponseDto;
import com.talex.server.services.subscription.ISubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    private final ISubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<BaseResponse> create(@Valid @RequestBody SubscriptionRequestDto request) {
        SubscriptionResponseDto response = subscriptionService.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Subscription created")
                        .data(response)
                        .build());
    }

    @GetMapping
    public ResponseEntity<BaseResponse> list(
            @RequestParam(required = false) Map<String, Object> criteria,
            @RequestParam(required = false) String[] durationUnits,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
            ) {
        BasePageResponse<SubscriptionResponseDto> pageResponse = subscriptionService
                .filterSubscriptions(SubscriptionFilterRequestDto.builder()
                        .criteria(criteria)
                        .durationUnits(durationUnits == null ? new String[0] : durationUnits)
                        .sortBy(sortBy)
                        .sortDirection(sortDirection)
                        .page(page)
                        .pageSize(pageSize)
                        .build());
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(pageResponse)
                .build());
    }

    @GetMapping("/{subscriptionId}")
    public ResponseEntity<BaseResponse> getById(@PathVariable String subscriptionId) {
        SubscriptionResponseDto response = subscriptionService
                .getSubscriptionById(subscriptionId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }

    @PutMapping("/{subscriptionId}")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String subscriptionId,
            @Valid @RequestBody SubscriptionRequestDto request) {
        SubscriptionResponseDto response = subscriptionService.updateSubscription(subscriptionId, request);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Subscription updated")
                .data(response)
                .build());
    }

    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<BaseResponse> delete(@PathVariable String subscriptionId) {
        subscriptionService.deleteSubscription(subscriptionId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Subscription deleted")
                .data(null)
                .build());
    }
}
