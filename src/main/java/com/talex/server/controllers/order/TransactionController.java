package com.talex.server.controllers.order;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.responses.payment.TransactionResponseDto;
import com.talex.server.enums.transaction.ReferenceType;
import com.talex.server.services.payment.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "API Quản lý và tra cứu giao dịch (Transactions)")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/by-reference")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy danh sách giao dịch theo Reference Type và Reference ID",
            description = "Truyền vào refType (ORDER, SETTLEMENT, PREMIUM_RESULT, PENALTY) và refId để lấy danh sách các Transaction tương ứng."
    )
    public ResponseEntity<BaseResponse> getTransactionsByReference(
            @RequestParam("refType") ReferenceType referenceType,
            @RequestParam("refId") String referenceId) {

        List<TransactionResponseDto> transactions = transactionService.getTransactionsByReference(referenceType, referenceId);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(transactions)
                .build());
    }
}