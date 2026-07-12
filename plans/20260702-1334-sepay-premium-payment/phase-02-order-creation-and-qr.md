# Phase 02 — Order Creation & QR Generation

## Context Links
- Overview: [plan.md](plan.md) · Depends on [phase-01](phase-01-data-layer-and-config.md)
- Verified: `entities/subscription/Subscription.java:34-41` (price, duration, durationUnit),
  `services/subscription/ISubscriptionService.java:20` (`getSubscriptionByIdEntity`),
  `exceptions/codes/SubscriptionErrorCode.java`, `exceptions/details/SubscriptionException.java`,
  `exceptions/ExceptionGlobalHandler.java:122-133` (per-exception handler pattern),
  `annotations/CurrentAccountId.java`, `dtos/BaseResponse.java`.

## Overview
- **Priority:** P1
- **Status:** done
- Create Order (`AWAITING_PAYMENT`, 30-min expiry), generate SePay VietQR URL, return to client. Handle retry edge cases.

## Key Insights
- `Subscription` entity carries `price` (BigDecimal), `duration`, `durationUnit` — source for `totalAmount` and FE "thời hạn dự kiến".
- `Order` non-null fields require setting `totalAmount`, `coinAmount`(0L), `fiatAmount` for SEPAY purchase.
- `itemType="SUBSCRIPTION"`, `itemId=subscriptionId` (String) — no entity/enum rename.
- QR: `https://qr.sepay.vn/img?acc={SEPAY_ACCOUNT_NUMBER}&bank={SEPAY_BANK_NAME}&amount={totalAmount}&des={paymentCode}` — built from `SePayProperties`, no external call (deterministic URL, FE `<img>` fetches it).
- Exceptions surface via `ExceptionGlobalHandler` — a new `PaymentException`+`PaymentErrorCode` needs a matching `@ExceptionHandler` block (same shape as `handleSubscriptionException`, `ExceptionGlobalHandler.java:122-133`).

## Requirements
Functional:
- `POST /api/v1/orders {subscriptionId}` (auth required, `@CurrentAccountId`) → validate subscription exists/not deleted → create Order → return `orderId, paymentCode, qrUrl, totalAmount, status, expiresAt`.
- `GET /api/v1/orders/{id}` → order status for FE polling (owner-scoped).
- **Edge 3 (normal retry):** Order still `AWAITING_PAYMENT` within 30 min → return SAME `paymentCode` + regenerated `qrUrl`, no new Order.
- **Edge 2 (retry near expiry):** re-request while remaining time < 5 min → set Order `OUT_OF_TIME`, throw error (no new payment).
Non-functional:
- QR generation is pure string build (no I/O). totalAmount uses `BigDecimal` (VND integer).

## Architecture
Data flow:
```
FE POST /orders {subscriptionId}
  → OrderService.createOrder(accountId, subscriptionId)
     → subscriptionService.getSubscriptionByIdEntity(subscriptionId)   // validate
     → totalAmount = subscription.price; paymentCode = generator.next()
     → Order{status=AWAITING_PAYMENT, expiresAt=now+30m, itemType=SUBSCRIPTION, itemId=subscriptionId}
     → sePayService.buildQrUrl(paymentCode, totalAmount)
  ← OrderResponseDto
```
Retry: FE re-POST or GET `?regenerate` → `getOrCreateActiveOrder`: if existing `AWAITING_PAYMENT` order for
(account,itemId) → apply edge 2/3 logic; else create new.

## Related Code Files
Create:
- `services/payment/IOrderService.java`, `services/payment/impls/OrderServiceImpl.java`.
- `services/payment/ISePayService.java`, `services/payment/impls/SePayServiceImpl.java` (QR build; webhook verify added phase 03).
- `controllers/OrderController.java` (`POST /api/v1/orders`, `GET /api/v1/orders/{id}`).
- `exceptions/codes/PaymentErrorCode.java`, `exceptions/details/PaymentException.java` (mirror Subscription* pattern).
Modify:
- `exceptions/ExceptionGlobalHandler.java` — add `@ExceptionHandler(PaymentException.class)` (SHARED FILE — coordinate; no parallel edits).
- Reuse DTOs from phase 01.

## Implementation Steps
1. `PaymentErrorCode` enum: `SUBSCRIPTION_NOT_FOUND_FOR_ORDER`, `ORDER_NOT_FOUND`, `ORDER_EXPIRED`, `ORDER_NOT_OWNED`, `ORDER_ALREADY_COMPLETED` (code/httpStatus/message triple like SubscriptionErrorCode).
2. `PaymentException` extends `RuntimeException` with `PaymentErrorCode` (mirror `SubscriptionException.java`).
3. `ExceptionGlobalHandler`: add handler returning `BaseResponse.code(errorCode.getCode())` + `errorCode.getHttpStatus()`.
4. `SePayServiceImpl.buildQrUrl(paymentCode, amount)` → URL from `SePayProperties`; URL-encode `des`/`bank`.
5. `OrderServiceImpl.createOrder(accountId, subscriptionId)`:
   - validate subscription; compute amounts; generate paymentCode; build+save Order; return DTO with qrUrl.
   - retry: look up existing active order for (accountId,itemId,AWAITING_PAYMENT). If found & remaining≥5min → return same. If remaining<5min → set OUT_OF_TIME, save, throw `ORDER_EXPIRED`.
6. `OrderServiceImpl.getOrder(orderId, accountId)` → owner check → DTO (status for polling).
7. `OrderController` with `@PreAuthorize("isAuthenticated()")` + `@CurrentAccountId UUID accountId`.
8. `./mvnw compile`.

## Todo List
- [x] PaymentErrorCode + PaymentException
- [x] ExceptionGlobalHandler entry
- [x] SePayService.buildQrUrl
- [x] OrderService createOrder + retry edges 2/3
- [x] OrderService getOrder (polling, owner-scoped)
- [x] OrderController POST + GET
- [x] Compile clean

## Success Criteria
- `POST /orders` with valid subscriptionId → 200 + valid `qr.sepay.vn` URL containing correct `des=TLXxxxxxx` & `amount`.
- Invalid subscriptionId → 404 via PaymentException.
- Repeated POST within window → same paymentCode (no duplicate Order).
- Retry with <5 min left → order flips `OUT_OF_TIME` + error.
- `GET /orders/{id}` returns status; other users get 403/404.

## Risk Assessment
| Risk | L×I | Mitigation |
|------|-----|-----------|
| Concurrent double-create for same account/item | M×M | Look up active order before create; unique paymentCode; DB constraint |
| amount formatting (decimals in URL) | M×M | totalAmount stored as integer VND; strip fraction in `des`/`amount` |
| Edge-2 flips order others may still pay | L×M | 5-min guard is business rule (user-confirmed); OUT_OF_TIME final |
| ExceptionGlobalHandler merge conflict | M×L | Single-owner edit; sequential after 01 |

## Security Considerations
- `@CurrentAccountId` binds order to authenticated user; `getOrder` enforces ownership (no IDOR).
- Never trust client-sent amount — always derive `totalAmount` from Subscription server-side.

## Next Steps
Unblocks Phase 03 (webhook completes these orders) and Phase 05 (FE consumes POST/GET).
