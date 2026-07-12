# Phase 05 — Frontend Checkout & QR (Next.js)

## Context Links
- Overview: [plan.md](plan.md) · Depends on [phase-02](phase-02-order-creation-and-qr.md) (POST/GET order endpoints)
- Work context: `C:\TaleX\TaleX-FE-KLTN` (Next.js 16, React 19, TanStack Query, Zustand, shadcn/ui).
- Verified: `src/app/premium/page.tsx:95-97` (`handleSelectPackage` = `console.log` only), button `onClick` `:143-149`;
  `src/features/premium/api/premium.api.ts:20-40` (useQuery + httpClient pattern);
  `src/shared/ui/dialog.tsx` (exists); `src/shared/api/http-client.ts:4-25` (`httpClient` axios instance, `BaseResponse`, `BasePageResponse`).

## Overview
- **Priority:** P2
- **Status:** done
- New `payment` feature: create order, render SePay QR `<img>`, poll order status, countdown timer, checkout modal on premium page + active-subscription stacking warning.

## Key Insights
- QR needs NO library — SePay returns image URL; render `<img src={qrUrl}>`.
- `httpClient` is an axios instance (`.get`/`.post`); follow `premium.api.ts` hook style (`useQuery`/`useMutation`).
- **Status transport = polling** (KISS): `useQuery` on `GET /api/v1/orders/{id}` with `refetchInterval: 3000`, stop when COMPLETED/OUT_OF_TIME. SSE reuse rejected (pipeline-scoped, short window). Reversible if UX needs push.
- Active-subscription warning: user with active sub → new package stacks (does NOT activate now). FE must show expected start = current sub endTime, and total effective end. Need current active subscription — scout existing account-subscription API (e.g. `/api/v1/account-subscriptions` filter by current user) before building; if none, expose via backend (note, minor).
- Order TTL 30 min, but checkout "chờ chuyển khoản" countdown displays 5 min per spec.

## Requirements
Functional:
- Click "Chọn Gói Này" → open checkout `Dialog`: shows package name, price, expected duration; if active sub exists → stacking warning with dates.
- On confirm → `POST /api/v1/orders {subscriptionId}` → show QR image + 5-min countdown + status.
- Poll order status every 3s; on COMPLETED → success state; on OUT_OF_TIME/countdown zero → expired state + retry.
Non-functional:
- FSD: all under `src/features/payment/`. Reuse shared `Dialog`. No new QR dep.

## Architecture
```
premium/page.tsx (PricingCard) → onClick opens <CheckoutDialog subscription=...>
  CheckoutDialog:
    - useActiveSubscription() → warning + expected dates
    - useCreateOrder() mutation → {orderId, paymentCode, qrUrl, totalAmount, expiresAt}
    - <QrPanel qrUrl/> + <CountdownTimer minutes=5/> + useOrderStatus(orderId, poll 3s)
    - status COMPLETED → success; expired → retry (re-mutate)
```

## Related Code Files
Create (under `src/features/payment/`):
- `api/payment.api.ts` — `useCreateOrder()` (mutation POST /orders), `useOrderStatus(orderId)` (query GET /orders/{id}, refetchInterval).
- `types/payment.types.ts` — `CreateOrderRequest`, `OrderResponse`, `OrderStatus`.
- `components/checkout-dialog.tsx` — modal orchestration.
- `components/qr-panel.tsx` — `<img>` + paymentCode display.
- `components/countdown-timer.tsx` — mm:ss countdown (new component; none exists).
- `components/subscription-stacking-warning.tsx` — active-sub warning + dates.
- `hooks/use-active-subscription.ts` (or in api) — current user's active subscription (scout endpoint first).
Modify:
- `src/app/premium/page.tsx` — replace `handleSelectPackage` console.log (`:95-97`) with dialog open state; render `<CheckoutDialog>`.

## Implementation Steps
1. Scout account-subscription "my active sub" endpoint; wire `useActiveSubscription`. If absent → note backend gap (minor, out-of-band).
2. `types/payment.types.ts` mirroring backend `OrderResponseDto`.
3. `payment.api.ts`: `useCreateOrder` (useMutation, httpClient.post), `useOrderStatus` (useQuery, `refetchInterval: (q)=> done ? false : 3000`).
4. `countdown-timer.tsx`: props `expiresAt`/`minutes`; emits `onExpire`.
5. `qr-panel.tsx`: `<img src={qrUrl} alt="VietQR">` + paymentCode + amount.
6. `subscription-stacking-warning.tsx`: if active sub → "Gói mới sẽ kích hoạt sau khi gói hiện tại hết hạn (dd/mm/yyyy)".
7. `checkout-dialog.tsx`: compose; manage states create→awaiting→completed/expired; retry re-runs mutation (backend edge 2/3 handles reuse).
8. `premium/page.tsx`: `useState` selected package; open Dialog on click; render dialog.
9. `npm run build` / typecheck.

## Todo List
- [x] Scout + useActiveSubscription (reused existing `GET /api/v1/account-subscriptions/own`, no backend gap)
- [x] payment.types.ts
- [x] payment.api.ts (createOrder + status polling)
- [x] countdown-timer
- [x] qr-panel
- [x] subscription-stacking-warning
- [x] checkout-dialog
- [x] premium/page.tsx wiring
- [x] typecheck/lint clean

## Success Criteria
- "Chọn Gói Này" opens modal; confirm shows scannable SePay QR + 5-min countdown.
- Polling flips UI to success when webhook completes order (COMPLETED).
- Countdown zero / OUT_OF_TIME → expired UI + working retry.
- Active-sub user sees stacking warning with correct expected start date.

## Risk Assessment
| Risk | L×I | Mitigation |
|------|-----|-----------|
| Poll never stops (memory/API load) | M×M | `refetchInterval` returns false on terminal status; unmount clears |
| Countdown vs server expiry drift | M×L | Server is source of truth (status query); countdown is UX only |
| Active-sub endpoint missing | M×M | Scout first; degrade gracefully (hide warning) + flag backend gap |
| QR img blocked/CORS | L×M | Plain `<img>` to qr.sepay.vn (public CDN), no fetch/CORS |

## Security Considerations
- Never send/trust amount from client; backend derives from subscription (phase 02).
- Order status query is owner-scoped server-side (phase 02) — FE cannot poll others' orders.

## Next Steps
End-to-end demo after backend phases 01–04 + this. Mobile app (React Native) mirrors this later (out of scope).

## Unresolved Questions
- Exact "my active subscription" FE endpoint — scout `src/features/**` + backend account-subscription controller before coding.
