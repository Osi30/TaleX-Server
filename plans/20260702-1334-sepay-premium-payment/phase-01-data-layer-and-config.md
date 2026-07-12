# Phase 01 — Data Layer & Config

## Context Links
- Overview: [plan.md](plan.md)
- Verified files: `entities/transaction/Order.java:22-58`, `repositories/transaction/OrderRepository.java:1-9`,
  `configs/SecurityConfig.java:44-57`, `src/main/resources/application.yaml:127-138`,
  `controllers/TestController.java:37-63`.

## Overview
- **Priority:** P1 (blocker for all other phases)
- **Status:** done
- Foundation: add missing Order fields, paymentCode sequence, repositories, SePay config, security rule, DTOs.

## Key Insights
- `Order` already has `itemType`/`itemId` (polymorphic), `status`, `totalAmount`, `coinAmount`, `fiatAmount`
  (all `nullable=false`), `account` FK. MISSING: `paymentCode` (unique), `expiresAt`. Verified `Order.java:49-53`.
- `OrderRepository` is empty (`OrderRepository.java:8`). `TransactionRepository`, `InvoiceRepository` do NOT exist.
- No Flyway/Liquibase — schema via Hibernate `ddl-auto: update` (`application.yaml:30`). Standalone sequence
  must be created explicitly (`CREATE SEQUENCE IF NOT EXISTS`) since no entity generator references it.
- Security uses `permitAll` list (`SecurityConfig.java:45-56`); `/api/v1/webhooks/**` does NOT match
  `/api/v1/payments/sepay-webhook` → new matcher required.
- `TestController` writes `Order` rows directly (`TestController.java:46-56`) — unrelated to Premium but shares Order.

## Requirements
Functional:
- Order persists `paymentCode` (unique, indexed) + `expiresAt`.
- Generate incremental unique `paymentCode` = `TLX` + 6 zero-padded digits.
- Query Order by `paymentCode`; query expired `AWAITING_PAYMENT` orders in batches.
- SePay account/bank/webhook-key read from config, never hardcoded.
Non-functional:
- No breaking change to existing Order consumers (new fields nullable at DB level; `ddl-auto: update` adds columns).

## Architecture
Data in → out:
- New columns `payment_code VARCHAR UNIQUE`, `expires_at TIMESTAMP` on `orders`.
- `payment_code_seq` Postgres sequence; `nextval` → `String.format("TLX%06d", n)`.
- `SePayProperties` (`@ConfigurationProperties(prefix="sepay")`): `accountNumber`, `bankName`, `webhookApiKey`.

## Related Code Files
Modify:
- `entities/transaction/Order.java` — add `paymentCode` (`@Column(name="payment_code", unique=true)`), `expiresAt`.
- `repositories/transaction/OrderRepository.java` — add `findByPaymentCode`, `findByOrderIdAndAccount_AccountId`(optional owner check), expiry batch query, `nextPaymentCodeSequence` native query.
- `configs/SecurityConfig.java` — add `/api/v1/payments/sepay-webhook` to `permitAll`.
- `src/main/resources/application.yaml` — add `sepay:` block with env placeholders.
- `.env` (+ `.env.example` if present) — `SEPAY_ACCOUNT_NUMBER=100881945065`, `SEPAY_BANK_NAME=VietinBank`, `SEPAY_WEBHOOK_API_KEY=` (placeholder).
- `controllers/TestController.java` — gate behind non-prod profile (`@Profile("!prod")` on class) OR delete mock order/unlocked endpoints (best-effort; note only, not blocker).

Create:
- `configs/SePayProperties.java` (`@ConfigurationProperties`, registered via `@EnableConfigurationProperties` or `@Component`).
- `repositories/transaction/TransactionRepository.java` (`JpaRepository<Transaction,String>`).
- `repositories/transaction/InvoiceRepository.java` (`JpaRepository<Invoice,UUID>`).
- `components/PaymentCodeGenerator.java` (or in service) — wraps `nextval` + formatting.
- DTOs under `dtos/requests/payment/` & `dtos/responses/payment/`: `CreateOrderRequestDto` (`subscriptionId`),
  `OrderResponseDto` (`orderId`, `paymentCode`, `qrUrl`, `totalAmount`, `status`, `expiresAt`), `SePayWebhookPayloadDto` (phase 03 uses; can create here).

## Implementation Steps
1. Add `paymentCode` + `expiresAt` to `Order.java` with column annotations; keep Lombok `@Builder`.
2. Add repo methods: `Optional<Order> findByPaymentCode(String code)`;
   `@Query(value="SELECT nextval('payment_code_seq')", nativeQuery=true) long nextPaymentCodeSequence();`
   expiry batch: `List<Order> findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(OrderStatus s, LocalDateTime now)`.
3. Create `payment_code_seq` at startup: `ApplicationRunner`/`@PostConstruct` running
   `CREATE SEQUENCE IF NOT EXISTS payment_code_seq START 100000` (start high → 6-digit width). Put in a small `@Component`.
4. Create `SePayProperties` + wire into `application.yaml` (`sepay.account-number`, `sepay.bank-name`, `sepay.webhook-api-key`).
5. Append env vars to `.env`/`.env.example` (no real secret committed).
6. Add security matcher for `/api/v1/payments/sepay-webhook`.
7. Create `TransactionRepository`, `InvoiceRepository`.
8. Create DTO classes (records or Lombok `@Data @Builder`).
9. Gate `TestController` (best-effort).
10. `./mvnw compile` — verify no errors.

## Todo List
- [x] Order fields + unique index
- [x] OrderRepository methods + sequence native query
- [x] payment_code_seq initializer
- [x] SePayProperties + application.yaml + .env
- [x] SecurityConfig webhook matcher
- [x] TransactionRepository, InvoiceRepository
- [x] Payment DTOs
- [x] TestController gating (best-effort)
- [x] Compile clean

## Success Criteria
- App boots; `orders` table gains `payment_code` (unique) + `expires_at`; `payment_code_seq` exists.
- `nextPaymentCodeSequence()` returns increasing values; formatting yields `TLX100000`, `TLX100001`…
- `POST /api/v1/payments/sepay-webhook` reachable without JWT (401 not thrown by security layer).
- `./mvnw compile` passes.

## Risk Assessment
| Risk | L×I | Mitigation |
|------|-----|-----------|
| Sequence not created (ddl-auto ignores standalone seq) | M×H | Explicit `CREATE SEQUENCE IF NOT EXISTS` initializer, verified on boot |
| paymentCode wraparound >999999 | L×M | START 100000 → ~900k orders headroom; document; widen format if exceeded |
| Adding `unique` to existing `orders` with dup nulls | L×M | New column all-null initially; unique allows multiple NULLs in Postgres |
| Opening webhook path widens attack surface | M×M | Auth enforced in controller via API key (phase 03), not security layer |

## Security Considerations
- Webhook path is `permitAll` at Spring Security → authentication MUST happen in controller (phase 03 API-key check).
- No secrets in `application.yaml` defaults; `SEPAY_WEBHOOK_API_KEY` empty default, real value via `.env`.

## Next Steps
Unblocks Phase 02 (order creation) and Phase 03 (webhook). Phase 04 uses expiry batch query.
