---
title: "SePay VietQR Payment — Premium Subscription Purchase"
description: "Order/Payment layer + SePay VietQR + webhook to buy Premium subscriptions, extensible to Campaign/Combo later."
status: done
priority: P1
effort: ~3.5d (BE ~2.5d, FE ~1d)
branch: main
tags: [payment, sepay, subscription, webhook, backend, frontend]
created: 2026-07-02
---

# SePay VietQR — Premium Subscription Payment

Buy Premium via SePay VietQR. User creates Order → gets dynamic QR → pays → SePay webhook completes Order,
creates Transaction, credits AccountSubscription (stacking duration). Generic Order layer (`itemType`/`itemId`)
so Campaign & Episode/Combo flows extend later (OUT OF SCOPE now).

## Scope (user-confirmed decisions — do NOT re-propose alternatives)
- Premium subscription flow ONLY. Campaign / Episode-Combo later.
- Order-expiry via Spring `@Scheduled` polling (NOT Kafka).
- Reuse existing enums, `Order`/`Transaction`/`Invoice` entities, `AccountSubscriptionService`.

## Phases
| # | Phase | One-liner | Depends on |
|---|-------|-----------|-----------|
| 01 | [Data layer & config](phase-01-data-layer-and-config.md) | Order fields (paymentCode, expiresAt), paymentCode sequence, repos, SePay config props, Security permitAll, DTOs, TestController gating | — |
| 02 | [Order creation & QR](phase-02-order-creation-and-qr.md) | OrderService + SePayService (QR URL), OrderController POST/GET, PaymentException, retry edge cases 2&3 | 01 |
| 03 | [SePay webhook processing](phase-03-sepay-webhook-processing.md) | Webhook controller/service: API-key auth, content parse, amount/account verify, idempotent completion, Transaction + AccountSubscription | 01, 02 |
| 04 | [Order expiry & invoice](phase-04-order-expiry-and-invoice.md) | @Scheduled expiry job (AWAITING_PAYMENT→OUT_OF_TIME), best-effort Invoice + email (eInvoice stub) | 01, 03 |
| 05 | [Frontend checkout & QR](phase-05-frontend-checkout-and-qr.md) | payment feature: create-order mutation, QR `<img>`, status polling, countdown, checkout Dialog on premium page | 02 |

## Key dependencies
- BE 01 → 02 → 03; 04 after 03; FE 05 after 02.
- Reuse: `AccountSubscriptionService.createAccountSubscription()` (auto duration-stacking).
- No Flyway — schema via Hibernate `ddl-auto: update`; `payment_code_seq` created via `CREATE SEQUENCE IF NOT EXISTS`.

## Cross-cutting decisions
- **Status transport (FE): POLLING** `GET /api/v1/orders/{id}` every 3s (KISS). SSE reuse considered but rejected —
  existing `SseController` is pipeline/creator-scoped; payment window is short (5 min). Proposal, reversible — see phase 05.
- **paymentCode**: `TLX` + zero-padded 6 digits from a dedicated Postgres sequence (incremental, unique).
- **Idempotency**: pessimistic lock on Order + `COMPLETED` status guard (SePay retries ≤7×).
- **eInvoice**: best-effort/stub — Order/Transaction/AccountSubscription MUST succeed even if invoice/email fails.

## Out of scope
Campaign/Engagement payment, Episode/Combo purchase, coin top-up, refunds, IAP.
