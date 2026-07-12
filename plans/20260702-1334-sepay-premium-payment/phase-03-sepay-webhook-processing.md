# Phase 03 — SePay Webhook Processing

## Context Links
- Overview: [plan.md](plan.md) · Depends on [phase-01](phase-01-data-layer-and-config.md), [phase-02](phase-02-order-creation-and-qr.md)
- Verified: `controllers/CloudinaryWebhookController.java:14-27` (webhook controller pattern, `@RequestBody String` + `@RequestHeader`),
  `services/subscription/impls/AccountSubscriptionService.java:43-61` (createAccountSubscription, auto duration-stacking `:160-175`),
  `dtos/requests/subscription/AccountSubscriptionRequestDto.java:16-24` (accountId UUID + subscriptionId String),
  `entities/transaction/Transaction.java:23-71` (referenceType/referenceId, no direct Order FK),
  `enums/transaction/{TransactionStatus,PaymentMethod,ReferenceType}.java`.

## Overview
- **Priority:** P1
- **Status:** done
- Receive SePay webhook → authenticate (API key) → verify → idempotently complete Order + Transaction + AccountSubscription.

## Key Insights
- SePay payload fields (verified from docs, use EXACT names): `id`, `gateway`, `transactionDate`, `accountNumber`,
  `subAccount`, `code`, `content`, `transferType`, `description`, `transferAmount`, `accumulated`, `referenceCode`.
- `content` = "TLX100234 chuyen tien" — bank appends noise → extract paymentCode by regex `TLX\d{6}` (case-insensitive), NOT exact match.
- Auth: SePay sends header `Authorization: Apikey <SEPAY_WEBHOOK_API_KEY>`. Verify BEFORE parsing payload; 401 on mismatch.
- MUST return HTTP 200/201 + `{"success":true}` within 30s; SePay retries ≤7× on non-success → handler MUST be idempotent.
- `Transaction` links Order via `referenceType=ORDER` + `referenceId=orderId` (no FK column). Idempotency guard = Order status `COMPLETED`, not a Transaction dedupe column.
- `AccountSubscriptionService.createAccountSubscription(AccountSubscriptionRequestDto)` already stacks duration
  (`calculateStartTime` uses latest valid sub endTime+1s, `:160-175`). Reuse verbatim — set `accountId`(UUID from Order.account) + `subscriptionId`(Order.itemId).

## Requirements
Functional:
- `POST /api/v1/payments/sepay-webhook` accepts SePay JSON + `Authorization` header.
- Verify: API key matches; `accountNumber` == `SEPAY_ACCOUNT_NUMBER`; `transferType`=="in"; `transferAmount >= Order.totalAmount` (>=, not ==).
- Parse paymentCode from `content`; find Order. If not found / already COMPLETED / amount short → return `{"success":true}` (or documented) WITHOUT double-crediting.
- On match: create Transaction (SUCCESS, SEPAY, ORDER ref), set Order.status=COMPLETED, call createAccountSubscription.
Non-functional:
- Idempotent under ≤7 retries and concurrent delivery. Fast (<30s) — invoice/email deferred (phase 04).

## Architecture
Data flow:
```
SePay POST /payments/sepay-webhook  [Authorization: Apikey KEY]
  → SePayWebhookController: verify API key (else 401)
  → SePayService.handleWebhook(payload)   [@Transactional]
     → extract paymentCode (regex TLX\d{6}) from content
     → orderRepo.findByPaymentCodeForUpdate(code)   // pessimistic lock
     → guards: exists? status==AWAITING_PAYMENT? accountNumber ok? amount>=total?
        - if status==COMPLETED → return success (idempotent no-op)
     → Transaction{paidAmount=transferAmount, method=SEPAY, status=SUCCESS,
                   referenceType=ORDER, referenceId=orderId}  → save
     → order.status=COMPLETED → save
     → accountSubscriptionService.createAccountSubscription(dto{accountId, subscriptionId=order.itemId})
  ← {"success":true}   (invoice+email fired best-effort in phase 04, non-blocking)
```

## Related Code Files
Create:
- `controllers/SePayWebhookController.java` (mirror `CloudinaryWebhookController`; path `/api/v1/payments/sepay-webhook`).
- `services/payment/impls/SePayServiceImpl.java` — add `handleWebhook(...)`, `verifyApiKey(header)`, `extractPaymentCode(content)`.
- `services/payment/ITransactionService.java` + impl (create Transaction from completed order).
- `dtos/requests/payment/SePayWebhookPayloadDto.java` (exact field names; Jackson binds JSON).
Modify:
- `repositories/transaction/OrderRepository.java` — add `@Lock(PESSIMISTIC_WRITE)` `findByPaymentCode` variant (`findWithLockByPaymentCode`).
- `exceptions/codes/PaymentErrorCode.java` — add `WEBHOOK_UNAUTHORIZED`, `WEBHOOK_ACCOUNT_MISMATCH`, `WEBHOOK_AMOUNT_MISMATCH` (SHARED with phase 02 — sequential edit).

## Implementation Steps
1. `SePayWebhookPayloadDto` with exact fields (`transferAmount` Long/BigDecimal, `transferType`, `content`, `accountNumber`, `referenceCode`, `id`, `code`, `gateway`, `transactionDate`, `accumulated`, `subAccount`, `description`).
2. `SePayServiceImpl.verifyApiKey(authHeader)` → strip `Apikey ` prefix, constant-time compare to `SePayProperties.webhookApiKey`. Reject empty configured key defensively.
3. `extractPaymentCode(content)` → regex `(?i)TLX\d{6}`; first match or null.
4. `handleWebhook` `@Transactional`:
   - parse code → `findWithLockByPaymentCode` → if empty: log + return success (nothing to do).
   - if `status==COMPLETED`: return success (idempotent).
   - if `status!=AWAITING_PAYMENT`: return success (expired/cancelled — no credit).
   - verify accountNumber & transferType & amount>=total; on verify fail → log + return success (do NOT credit) — avoid SePay retry storm on permanent mismatch. (Decision note: return success even on business-verify fail so SePay stops retrying; anomalies logged for manual review.)
   - create Transaction, set COMPLETED, call createAccountSubscription.
5. `SePayWebhookController`: verify key → call service → return `ResponseEntity.ok(Map.of("success", true))`. On bad key → 401.
6. `OrderRepository.findWithLockByPaymentCode` with `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
7. `./mvnw compile`.

## Todo List
- [x] SePayWebhookPayloadDto (exact fields)
- [x] verifyApiKey (constant-time)
- [x] extractPaymentCode regex
- [x] handleWebhook idempotent + locked
- [x] TransactionService create
- [x] Reuse createAccountSubscription
- [x] SePayWebhookController 200/401
- [x] Pessimistic-lock repo method
- [x] Compile clean

## Success Criteria
- Valid webhook → Order COMPLETED, one Transaction (SUCCESS/SEPAY/ORDER), one new AccountSubscription (stacked).
- Duplicate webhook (same paymentCode) → no second Transaction, no double subscription (verified by count).
- Wrong API key → 401. Wrong accountNumber / short amount → no credit, logged.
- Response `{"success":true}` returned well under 30s.

## Risk Assessment
| Risk | L×I | Mitigation |
|------|-----|-----------|
| Double-credit on concurrent retries | M×H | Pessimistic lock + COMPLETED status guard in one tx |
| paymentCode regex misses bank-mangled content | M×H | `(?i)TLX\d{6}`; log unmatched content for tuning |
| Returning success on verify-fail hides fraud attempt | M×M | Log WARN with payload id/referenceCode for manual review; accountNumber check blocks spoof credit |
| createAccountSubscription throws → tx rollback → SePay retries | L×M | Method already `@Transactional`; whole handler atomic; retry safe (idempotent) |
| Long tx holding lock | L×M | Keep invoice/email OUT of this tx (phase 04) |

## Security Considerations
- API-key auth in controller (path is `permitAll`). Constant-time compare; reject if configured key blank.
- `accountNumber` match prevents spoofed "paid" notifications crediting via arbitrary account.
- Amount `>=` prevents underpayment credit; overpayment accepted per spec (no auto-refund in scope).

## Next Steps
Unblocks Phase 04 (invoice/email fired after completion; expiry job complements). FE (phase 05) observes COMPLETED via polling.

## Unresolved Questions
- SePay exact response contract on business-verify failure (retry vs stop) — assumed return success to stop retries; confirm with SePay docs if stricter needed.
