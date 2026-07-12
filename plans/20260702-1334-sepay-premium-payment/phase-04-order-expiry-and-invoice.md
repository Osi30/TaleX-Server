# Phase 04 — Order Expiry Scheduler & Invoice/Email

## Context Links
- Overview: [plan.md](plan.md) · Depends on [phase-01](phase-01-data-layer-and-config.md), [phase-03](phase-03-sepay-webhook-processing.md)
- Verified: `schedulers/ContentScheduledPublishingScheduler.java:18-55` (@Scheduled fixedDelayString batch + per-item try/catch),
  `entities/transaction/Invoice.java:16-36` (invoiceNumber/invoiceSeries/reservationCode/invoiceUrl + Transaction FK),
  `application.yaml:62-72` (Spring Mail configured: `smtp.gmail.com`, MAIL_USERNAME/PASSWORD).

## Overview
- **Priority:** P2 (expiry P1-ish for correctness; invoice best-effort)
- **Status:** done (scope reduced — user decision 2026-07-02)
- Two independent concerns: (a) expire stale orders via polling scheduler — IMPLEMENTED; (b) best-effort
  Invoice + email after completion — **CUT**, user explicitly decided not to build Invoice/eInvoice/email in
  this phase. Order/Transaction/AccountSubscription flow is complete and correct without it.

## Key Insights
- Scheduler pattern established: `@Scheduled(fixedDelayString="${...}")`, `findTop100By...` batch, per-item try/catch so one failure doesn't abort batch (`ContentScheduledPublishingScheduler.java:34-55`). Mirror exactly.
- `expiresAt` + expiry batch query added in phase 01. Job: `AWAITING_PAYMENT` & `expiresAt<=now` → `OUT_OF_TIME`.
- Invoice/email MUST be best-effort — Order/Transaction/AccountSubscription already committed in phase 03; invoice failure must NOT roll them back or block webhook response. Fire from a separate async path.
- SePay eInvoice API NOT yet confirmed → `InvoiceService.issueInvoice` stubbed (persist local Invoice row + TODO for real eInvoice call). Email uses existing Spring Mail.

## Requirements
Functional:
- Scheduler flips expired `AWAITING_PAYMENT` orders to `OUT_OF_TIME` (idempotent, batched).
- After Order COMPLETED: create Invoice (invoiceNumber generated locally), attempt email receipt. Failures logged, non-fatal.
Non-functional:
- Invoice/email async (`@Async` or event listener) — decoupled from webhook tx.
- Scheduler safe on empty batches; configurable delay via properties.

## Architecture
```
Expiry:  @Scheduled → orderRepo.findTop100ByStatusAndExpiresAtLessThanEqual(AWAITING_PAYMENT, now)
         → each: status=OUT_OF_TIME, save (try/catch per item)

Invoice: phase-03 publishes ApplicationEvent(OrderCompletedEvent{transactionId})
         → @TransactionalEventListener(AFTER_COMMIT) + @Async
         → InvoiceService.issueInvoice(transaction): build Invoice (local number), save,
           [TODO real SePay eInvoice], then EmailService.sendReceipt(...)  (best-effort)
```

## Related Code Files
Create:
- `schedulers/OrderExpiryScheduler.java` (mirror ContentScheduledPublishingScheduler).
- `services/payment/IInvoiceService.java` + `services/payment/impls/InvoiceServiceImpl.java` (build/save Invoice; stub eInvoice).
- `events/OrderCompletedEvent.java` + listener (or method in InvoiceService annotated `@TransactionalEventListener`).
- Email: reuse existing mail sender/service if present (scout for `JavaMailSender`/existing `EmailService`); else minimal `PaymentReceiptMailer`.
Modify:
- `services/payment/impls/SePayServiceImpl.java` — publish `OrderCompletedEvent` after successful completion (phase 03 hands off here).
- `application.yaml` — `payment.order.expiry-fixed-delay-ms:60000`, `payment.order.ttl-minutes:30`.

## Implementation Steps
1. `OrderExpiryScheduler`: `@Scheduled(fixedDelayString="${payment.order.expiry-fixed-delay-ms:60000}", initialDelayString=...)`; batch fetch; per-item flip to OUT_OF_TIME with try/catch + logging.
2. `OrderCompletedEvent` (carries transactionId/orderId). Publish via `ApplicationEventPublisher` at end of successful `handleWebhook` (phase 03 wiring point).
3. `InvoiceServiceImpl.issueInvoice(transaction)`: generate `invoiceNumber` (e.g. `INV` + timestamp/seq), build+save `Invoice` linked to Transaction; `invoiceUrl` null until real eInvoice; log `TODO eInvoice`.
4. Listener `@TransactionalEventListener(phase=AFTER_COMMIT)` + `@Async` → issueInvoice → sendReceipt; wrap in try/catch (best-effort).
5. Scout for existing mail service; reuse. Compose Vietnamese receipt email.
6. `./mvnw compile`.

## Todo List
- [x] OrderExpiryScheduler + config props
- [ ] OrderCompletedEvent + publisher wiring — cut, see below
- [ ] InvoiceService (build/save + eInvoice stub) — cut, see below
- [ ] AFTER_COMMIT @Async listener — cut, see below
- [ ] Email receipt (reuse existing mailer) — cut, see below
- [x] Compile clean

## Success Criteria
- Order left unpaid >30 min → becomes `OUT_OF_TIME` within one scheduler cycle.
- Completed order → Invoice row created; email attempt logged; webhook still returns fast (invoice off critical path).
- Invoice/email failure does NOT revert Order/Transaction/AccountSubscription.

## Risk Assessment
| Risk | L×I | Mitigation |
|------|-----|-----------|
| Scheduler races webhook (expire an order being paid) | L×M | Webhook uses lock + only credits AWAITING_PAYMENT; if expired first, no credit (payment arrived late — manual/refund out of scope, log) |
| @Async listener swallows errors silently | M×L | Explicit WARN logging; invoice retry deferred (YAGNI now) |
| eInvoice unavailable on user's SePay acct | H×L | Stub persists local Invoice; real call gated behind TODO/flag |
| Email creds missing (MAIL_* blank) | M×L | Best-effort; catch + log; no user-facing failure |

## Security Considerations
- Receipt email contains no card/bank secrets (SePay handles transfer). Only order/amount/subscription info.
- eInvoice credentials (when added) via env, never committed.

## Next Steps
Backend feature-complete after this phase. FE (phase 05) is independent, needs only phase 02 endpoints.

## Unresolved Questions
- SePay eInvoice API shape (developer.sepay.vn) — deferred; stub until user provides account access/spec.
- Invoice numbering scheme (series/reservationCode semantics) — local placeholder until eInvoice defined.
