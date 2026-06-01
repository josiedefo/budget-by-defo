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

- **Backend**: Java 25, Spring Boot 3.5, Spring Data JPA, Liquibase, PostgreSQL 17
- **Frontend**: Vue 3, Vuetify 3, Pinia, Vue Router, Axios, Vite

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

## Database Migrations (Liquibase)

Changelogs live in `backend/src/main/resources/db/changelog/`. Always add new YAML files and include them in `db.changelog-master.yaml`. Never reuse a changeSet `id`.

Current migrations: 001–016 (tables through savings_event index).

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

### Savings — Fund Links
| Method | Endpoint | Notes |
|--------|----------|-------|
| POST | `/api/savings/events/link-transaction` | Links one transaction |
| POST | `/api/savings/events/bulk-link-budget-item` | Bulk links budget item's transactions |

### Savings — Link Status
| Method | Endpoint | Notes |
|--------|----------|-------|
| GET | `/api/savings/link-status/budget-items` | Batch status by budgetItemIds + date range |

---

## Default Budget Sections

New budgets are created with: Income (isIncome: true), Housing, Transportation, Food, Utilities, Healthcare, Entertainment, Savings.
