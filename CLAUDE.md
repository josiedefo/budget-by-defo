# Budget App — Developer & Claude Context

A personal budget tracking application built with Vue.js + Vuetify frontend and Java Spring Boot + PostgreSQL backend.

---

## Project Structure

```
budget-by-defo/
├── backend/                    # Spring Boot REST API (Java 25)
│   ├── src/main/java/com/budget/
│   │   ├── controller/         # REST endpoints
│   │   ├── service/            # Business logic
│   │   ├── repository/         # JPA repositories
│   │   ├── model/              # JPA entities
│   │   ├── dto/                # Request/Response DTOs
│   │   └── config/             # CORS, exception handling
│   └── pom.xml
│
└── frontend/                   # Vue 3 + Vuetify 3
    ├── src/
    │   ├── views/              # MonthlyBudgetView, TransactionsView, SavingsView, …
    │   ├── components/         # BudgetSection, SavingsLinkDialog, BulkSavingsLinkDialog, …
    │   ├── services/           # api.js (axios)
    │   ├── stores/             # Pinia: budget, transaction, savings
    │   ├── router/             # index.js
    │   └── plugins/            # Vuetify setup
    └── package.json
```

---

## Tech Stack

- **Backend**: Java 25, Spring Boot 4.1, Spring Data JPA, Liquibase 5, PostgreSQL 17
- **Frontend**: Vue 3, Vuetify 4, Pinia 4, Vue Router 5, Axios, Vite 8

---

## Running the App

### Database (Docker)
```bash
docker-compose up -d
```
Creates PostgreSQL 17 on port 5433.

### Backend
```bash
cd backend
mvn spring-boot:run
```
Runs on http://localhost:8080

### Frontend
```bash
cd frontend
npm install
npm run dev
```
Runs on http://localhost:5173

---

## Deployment

Deployed on **AWS App Runner** with **Neon PostgreSQL** as the database.

```powershell
# From the project root (PowerShell execution policy workaround required):
powershell.exe -ExecutionPolicy Bypass -File ".\deploy.ps1"
```

Or type `/deploy` in Claude Code. The script handles ECR auth, Docker build/push, and App Runner redeployment.

> **AWS session note**: If the deploy fails with "session has expired", run `aws login` first, then re-run.

---

## Progressive Web App

The deployed app is installable on Android (and desktop Chrome) via `vite-plugin-pwa`, configured
in `frontend/vite.config.js`. Spring Boot serves the SPA and `/api` from one origin and App Runner
terminates HTTPS, so no extra infrastructure is involved.

**Scope**: installable + precached app shell. Data is still fetched live from `/api` — there is no
API response cache and no offline write queue. Offline, the shell loads from the service worker and
`App.vue` shows a warning banner (driven by `useOnline`) instead of the browser's error page.

**Update model**: `registerType: 'prompt'`, not `autoUpdate` — a new build must never swap itself in
under a half-finished budget edit. `usePwaUpdate` exposes `needRefresh`, and `App.vue` shows a
"Reload" snackbar.

**Icons**: `frontend/icon-source.svg` is the single source of truth; everything in `frontend/public/`
is generated from it. To change the mark, edit the SVG and regenerate:

```bash
cd frontend && npm i -D --no-save sharp && node scripts/generate-icons.mjs
```

`sharp` is deliberately not a project dependency — it ships a native binary that would be installed
on every `npm ci` in the Docker build for no runtime benefit. Commit the regenerated PNGs.

**Gotchas worth remembering**

- `SpaController`'s forward list is an allowlist. A new Vue route must be added there *and* stays
  covered by the service worker's `navigateFallback: '/index.html'`.
- `workbox.globPatterns` precaches `woff2` only. `@mdi/font` emits the same icon font four times
  (`.eot`/`.ttf`/`.woff`/`.woff2`); precaching all of them would cost users ~3 MB for nothing.
- `workbox.ignoreURLParametersMatching` includes `/^v$/` because `@mdi/font`'s CSS requests
  `...woff2?v=7.4.47`. Without it the query string defeats the precache match and every icon
  renders as an empty box when offline.
- `WebManifestMimeConfig` registers `.webmanifest` → `application/manifest+json`; embedded Tomcat
  has no mapping for that extension and would otherwise serve the manifest as octet-stream.
- `spring.web.resources.cache.cachecontrol.no-cache=true` keeps browsers from heuristically caching
  `index.html` and `sw.js`, which is the classic way to strand users on a service worker that never
  picks up a deploy.

To exercise the service worker locally you need a production build — the dev server does not
register one. Run the backend, then `npm run preview` (port 4173, proxies `/api` to :8080).

---

## Database Migrations (Liquibase)

Changelogs live in `backend/src/main/resources/db/changelog/`. Always add new YAML files and include them in `db.changelog-master.yaml`. Never reuse a changeSet `id`.

Current migrations: 001–020 (tables through unique savings link constraints).

---

## Data Model

### Core Budget
- **Budget**: year, month, sections[]
- **Section**: name, isIncome, displayOrder, items[]
- **BudgetItem**: name, plannedAmount, actualAmount, displayOrder, isExcludedFromBudget

### Transactions
- **Transaction**: type (INCOME/EXPENSE), transactionDate, merchant, amount, note
  - Optional FK to Section and BudgetItem
- **TransactionDTO** carries 8 savings link fields (populated in TransactionService, not from entity):
  - `linkedSavingsAccountEventId/Id/Name/EventType` (account layer)
  - `linkedSavingsFundEventId/Id/Name/EventType` (fund layer)

### Two-Layer Savings Model

**Layer 1 — SavingsAccount** (physical bank accounts)
- Tracks real account balances with DEPOSIT/WITHDRAWAL events
- `SavingsAccountEvent` has a `@ManyToOne Transaction` FK (`transaction_id`, nullable, ON DELETE SET NULL)

**Layer 2 — SavingsFund** (goal/envelope buckets)
- Allocated from the pool. Five goal types: TARGET, TARGET_WITH_DEADLINE, SPEND_DOWN, SPEND_AS_YOU_GO, NO_GOAL
- `SavingsEvent` tracks DEPOSIT_ALLOCATED, WITHDRAWAL, REALLOCATION_IN, REALLOCATION_OUT, PAYOUT
- `SavingsEvent.transactionRef` is a plain `Long` (not a FK) linking back to a Transaction
- Pool enforcement: sum of all active fund balances ≤ sum of all active account balances

---

## Key Architectural Decisions

### Copy From — Merge Strategy (BudgetService.copyBudget)
- If the target month is **empty**: full structural copy (new IDs for everything)
- If the target month **already has sections**: merge by name (case-insensitive). Matched items get their `plannedAmount`/`displayOrder` updated; their ID and transaction links are preserved. Unmatched source items are added as new. Target items not in source are left untouched.

### Transaction Linking to Savings
A budget transaction can be independently linked to:
1. A savings **account event** — via `SavingsAccountEvent.transaction` (real FK)
2. A savings **fund event** — via `SavingsEvent.transactionRef` (plain Long, already existed as a column)

Both are populated in `TransactionService.getTransactions()` via bulk queries (`findByTransactionIdIn` / `findByTransactionRefIn`) — one extra query per type per page, not N+1.

### Bulk Link — Budget Item to Savings
`POST /api/savings/accounts/{id}/bulk-link-budget-item` and `POST /api/savings/events/bulk-link-budget-item`:
- Scoped to a date range (usually the current month, passed from the frontend)
- Finds all transactions for the budget item, skips already-linked ones (checked via bulk query)
- Pre-flight balance check for the total before creating any events
- Returns `BulkLinkResult { linked, skipped, total, totalLinkedAmount }`

### Bulk Link — Explicit Transaction List (Transactions page multi-select)
`POST /api/savings/accounts/{id}/bulk-link-transactions` and `POST /api/savings/events/bulk-link-transactions`:
- Takes `transactionIds: Long[]` directly (no date range/budget item scoping) — used by the Transactions page's checkbox multi-select, including "select all matching"
- Same skip-already-linked / pre-flight-balance / `BulkLinkResult` behavior as the budget-item variant — both share a private `performBulkLink` core in `SavingsAccountService`/`SavingsEventService`

### Budget Item Link Status
`GET /api/savings/link-status/budget-items?ids=1,2,3&startDate=&endDate=`
- Returns `Map<Long, BudgetItemLinkStatus>` per budget item
- `allLinkedToAccount = true` only if ALL transactions in range are linked AND all to the **same** account
- Same logic for `allLinkedToFund`
- Used by `BudgetSection` on mount (batch fetch) and by `BulkSavingsLinkDialog` on open

---

## Frontend Patterns

### Transaction Store — `setFilters` vs `replaceFilters`
- `setFilters(partial)` — **merges** into existing filters. Use for user-driven filter changes.
- `replaceFilters(partial)` — **resets all filters first**, then applies. **Always use this for URL-driven navigation** (e.g., `?transactionId=N`, `?sectionName=X&budgetItemName=Y`). Using `setFilters` for URL navigation causes stale filter bleed-through from previous sessions.
- `clearFilters()` — resets everything including `uncategorized` and `transactionId`.

### Uncategorized Filter
A boolean filter `uncategorized: false` in the store and `AND (:uncategorized = false OR t.section_id IS NULL)` in the SQL. Toggled by a chip in the TransactionsView filter bar; applies immediately without pressing Search.

### Month Memory (App.vue)
`lastMonthlyYear` / `lastMonthlyMonth` refs track the most recently visited month. The watcher observes **both** `route.name` and `route.params` (combined object) so month-to-month navigation (same route name, different params) also updates the tracked values. Clicking the Monthly tab returns to the last visited month, not always the current month.

### SavingsLinkDialog (single transaction)
- Two independent sections: Savings Account + Savings Fund
- Emits `linked({ type: 'account'|'fund', eventDto })` and `unlinked({ type })`
- `TransactionsView.handleLinked/handleUnlinked` dispatches to `updateLinkedSavings()` based on type
- `updateLinkedSavings` uses `Object.assign` (mutates in-place) so the dialog's `props.transaction` stays live and the dialog refreshes without closing

### BulkSavingsLinkDialog (budget item)
- Fetches link status on open via `savingsApi.getBudgetItemLinkStatuses`
- Shows "All transactions linked to [Name]" (locked state) or the link form, independently per section
- After a successful Link All, re-fetches status → transitions to locked state immediately
- Emits `status-changed(itemId, status)` so `BudgetSection` can update the icon color

### BulkLinkTransactionsDialog (Transactions page multi-select)
- Takes a plain `transactionIds` array prop — no link-status pre-check (there's no such endpoint for an arbitrary id list), so the account/fund sections always show the link form, never a locked state
- Emits `linked({ type: 'account'|'fund', result })` per section on success; `TransactionsView.handleBulkLinked` just refetches the page so linked rows show their updated bank icon
- Selection is deliberately **not** cleared on `linked` — the same batch can be linked to an account and a fund in two separate steps while the dialog stays open. It's cleared by a `watch(showBulkLinkDialog, …)` only once the dialog actually closes

### Navigation: Savings → Transactions
Clicking the `mdi-open-in-new` icon on a savings event row navigates to `/transactions?transactionId=N`. The TransactionsView `onMounted` handler uses `replaceFilters({ transactionId: N })` — equivalent to the budget item link pattern (`?sectionName=X&budgetItemName=Y`).

---

## API Quick Reference

### Transactions
| Method | Endpoint | Notes |
|--------|----------|-------|
| GET | `/api/transactions` | Filters: transactionId, startDate, endDate, type, sectionId, budgetItemId, sectionName, budgetItemName, merchant, uncategorized |
| POST | `/api/transactions/import` | CSV import with column mapping |

### Savings — Account Links
| Method | Endpoint | Notes |
|--------|----------|-------|
| POST | `/api/savings/accounts/{id}/link-transaction` | Links one transaction |
| POST | `/api/savings/accounts/{id}/bulk-link-budget-item` | Bulk links budget item's transactions |
| POST | `/api/savings/accounts/{id}/bulk-link-transactions` | Bulk links an explicit `transactionIds` list |

### Savings — Fund Links
| Method | Endpoint | Notes |
|--------|----------|-------|
| POST | `/api/savings/events/link-transaction` | Links one transaction |
| POST | `/api/savings/events/bulk-link-budget-item` | Bulk links budget item's transactions |
| POST | `/api/savings/events/bulk-link-transactions` | Bulk links an explicit `transactionIds` list |

### Savings — Link Status
| Method | Endpoint | Notes |
|--------|----------|-------|
| GET | `/api/savings/link-status/budget-items` | Batch status by budgetItemIds + date range |

---

## Default Budget Sections

New budgets are created with: Income (isIncome: true), Housing, Transportation, Food, Utilities, Healthcare, Entertainment, Savings.
