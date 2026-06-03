# Budget App

A personal budget tracking application to manage your finances month by month.

## Features

### Budget Management
- **Monthly Budget View** - Track income and expenses for each month
- **Yearly Overview** - See your financial summary across all months
- **Planned vs Actual** - Compare what you budgeted against actual spending
- **Custom Sections** - Add your own budget categories
- **Default Categories** - Comes with Income, Housing, Transportation, Food, Utilities, Healthcare, Entertainment, and Savings
- **Exclude Items** - Optionally exclude specific budget items from overall calculations
- **Key Item Tags** - Tag budget items as "key" from the monthly view (bookmark icon); tagged items and their diff appear beneath the month row on the yearly overview to quickly explain savings variance

### Transactions
- **Transaction Tracking** - Record individual transactions with date, amount, description, and type
- **Link to Budget** - Associate transactions with budget sections and items
- **CSV Import** - Bulk import transactions from CSV files with column mapping; budget items that exist in the CSV but not in the budget month are auto-created in the correct section
- **Click-to-View** - Click on actual amounts to view related transactions
- **Uncategorized Filter** - One-click chip to show only transactions not yet assigned to any budget section
- **Link to Savings** - Link any transaction to a savings account event or savings fund event; bank icon on each row turns teal when linked; deleting a linked transaction also deletes its associated savings events and reverses their balance impact
- **Bulk Link by Budget Item** - From the Monthly Budget view, bulk-link all transactions for a budget item to a savings account or fund in one action; icon turns teal when all transactions are linked
- **Bulk Delete** - Select individual transactions via checkboxes or use "Select all N matching results" to select every transaction matching the current filter; delete all selected in one action with a confirmation dialog

### Planner
- **Monthly Plans** - Create detailed plans for individual budget items
- **Plan Items** - Break down planned amounts into specific line items
- **Auto-Sync** - Plan totals automatically update budget item planned amounts
- **Copy From carries Plans** - When copying planned amounts from a previous month, any Plan linked to a source budget item is automatically copied to the target month and linked to the matching target budget item (existing target plans are replaced)
- **Live Source Links** - Plan items added from a recurring payment or salary stay linked to their source via a database FK; clicking the icon in the plan opens the source directly in edit mode
- **Cascade Updates** - Editing a recurring payment or salary automatically updates the name and amount on all linked plan items across all months, then recalculates plan totals and budget item planned amounts; existing plans are backfilled on first edit so the live link applies retroactively

### Recurring Payments
- **Recurring Payment Templates** - Define recurring payments once (Netflix, Gym, etc.)
- **Quick Add to Plans** - Easily add recurring payments to any plan
- **Recurrence Options** - Weekly, Monthly, Quarterly, Semi-Annually, or Yearly
- **Visual Indicator** - Plan items from recurring payments show a clickable repeat icon that opens the source in edit mode

### Salaries
- **Salary Templates** - Define salary entries with gross pay and payroll deductions
- **Payroll Deductions** - Track Federal Tax, Medicare, Social Security, 401K, HSA, Medical Insurance, FSA, and extra tax withholding
- **Net Pay Calculation** - Automatically calculates net pay from gross minus all deductions
- **Quick Add to Plans** - Add salaries to plans as income items using the computed net pay
- **Visual Indicator** - Plan items from salaries show a clickable dollar icon that opens the source in edit mode

### Savings
- **Savings Pool** - Aggregate multiple physical savings accounts into a single tracked pool
- **Fund Buckets** - Allocate pool money into named funds with five goal types: Target, Target with Deadline, Spend-Down, Spend-as-You-Go, and No Goal
- **Deposit & Withdrawal** - Log deposits to funds and withdrawals from funds; pool enforcement prevents over-allocation
- **Reallocation** - Move money between funds atomically with paired ledger events
- **Spend-Down Payout** - Process scheduled payouts for spend-down funds once the payout date is reached
- **Edit & Delete Events** - Correct deposit and withdrawal events; balance is automatically reversed or adjusted
- **Event History** - Full per-fund event history with type badges (Deposit, Withdrawal, Reallocation, Payout)
- **Account Management** - Add and manage physical savings accounts with balance and as-of date
- **Account Event History** - Per-account deposit/withdrawal history with correct running balances computed chronologically (not by insertion order); as-of date automatically reflects the latest transaction
- **Summary Panel** - Year summary with total pool, allocated, unassigned, remaining-to-save, and upcoming deadlines
- **Transaction Links** - Link a budget transaction to a savings account event or fund event; navigate directly from a savings event back to the linked transaction
- **Bulk Transaction Links** - From the Monthly Budget view, bulk-link all transactions for a budget item to a savings account or fund; linked state shown in the dialog and reflected on the budget item icon

## Tech Stack

| Layer | Technology |
|-------|------------|
| Frontend | Vue 3, Vuetify 3, Pinia, Vue Router |
| Backend | Java 25, Spring Boot 3.5, Spring Data JPA |
| Database | PostgreSQL 17 |

## Prerequisites

- Java 25 or higher
- Node.js 18 or higher
- Docker and Docker Compose

## Getting Started

### Option 1: Run Everything with Docker (Recommended)

Build and start the entire application:

```bash
docker-compose up --build
```

This starts:
- PostgreSQL 17 database on port 5433
- Budget app (frontend + backend) on port 8080

Access the app at http://localhost:8080

### Option 2: Run Services Separately

#### 1. Start the Database (Local DB)

```bash
docker-compose up postgres -d
```

#### 2. Run the Backend (Local DB)

```bash
cd backend
mvn spring-boot:run
```

#### 3. Run the Backend (Neon DB)

Set the following Windows environment variables first (search "Environment Variables" in the Start menu):

| Variable | Value |
|----------|-------|
| `NEON_DATABASE_URL` | `jdbc:postgresql://<your-neon-host>/budget_db?sslmode=require` |
| `NEON_DATABASE_USERNAME` | your Neon username |
| `NEON_DATABASE_PASSWORD` | your Neon password |

Then run with the `neon` profile:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=neon
```

#### 4. Run the Frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173` in your browser.

## Database

The app supports two database configurations:

| Profile | Database | Usage |
|---------|----------|-------|
| default | PostgreSQL 17 (Docker) | Local development |
| `neon` | Neon PostgreSQL 17 (cloud) | Production / remote |

Database schema is managed by **Liquibase** — migrations run automatically on startup.

## Deployment

The app is deployed on **AWS App Runner** with **Neon PostgreSQL** as the database.

### Architecture

```
Internet → AWS App Runner (Docker container)
                    ↕
           Neon PostgreSQL (serverless)
```

### Build and Deploy

Use the included PowerShell script (requires AWS CLI and Docker):

```powershell
.\deploy.ps1                      # deploys to us-east-1 (default)
.\deploy.ps1 -Region us-west-2    # override region
```

Or type `/deploy` in Claude Code for a guided one-click deployment.

The script handles ECR authentication, Docker build & push, and triggers the App Runner redeployment automatically.

### App Runner Environment Variables

| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | Neon JDBC connection string |
| `SPRING_DATASOURCE_USERNAME` | Neon username |
| `SPRING_DATASOURCE_PASSWORD` | Neon password |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Set to `validate` |
| `SPRING_PROFILES_ACTIVE` | Set to `neon` |

## Project Structure

```
budget-by-defo/
├── backend/                    # Spring Boot REST API
│   └── src/main/
│       ├── java/com/budget/
│       │   ├── controller/     # REST endpoints
│       │   ├── service/        # Business logic
│       │   ├── repository/     # JPA repositories
│       │   ├── model/          # Entity classes
│       │   └── dto/            # Request/Response DTOs
│       └── resources/
│           ├── application.properties        # Local (Docker) config
│           ├── application-neon.properties   # Neon (cloud) config
│           └── db/changelog/                 # Liquibase migrations
├── frontend/                   # Vue.js application
│   └── src/
│       ├── views/              # Page components
│       ├── components/         # Reusable components
│       ├── stores/             # Pinia state management
│       └── services/           # API client
├── Dockerfile                  # Multi-stage build
├── docker-compose.yml          # Local dev with PostgreSQL 17
├── CLAUDE.md                   # Technical documentation
└── README.md                   # This file
```

## API Overview

### Budgets
| Endpoint | Description |
|----------|-------------|
| `GET /api/budgets/{year}/{month}` | Get or create monthly budget |
| `GET /api/budgets/{year}` | Get yearly summary |
| `POST /api/sections` | Add a new section |
| `POST /api/items` | Add a new budget item |
| `PUT /api/items/{id}` | Update a budget item |
| `DELETE /api/items/{id}` | Delete a budget item |

### Transactions
| Endpoint | Description |
|----------|-------------|
| `GET /api/transactions` | List transactions with filters (startDate, endDate, type, merchant, sectionName, budgetItemName, transactionId, uncategorized) |
| `POST /api/transactions` | Create a transaction |
| `PUT /api/transactions/{id}` | Update a transaction |
| `DELETE /api/transactions/{id}` | Delete a transaction |
| `POST /api/transactions/import` | Import transactions from CSV |

### Plans
| Endpoint | Description |
|----------|-------------|
| `GET /api/plans` | List plans for a month |
| `GET /api/plans/{id}` | Get a plan with items |
| `POST /api/plans` | Create a plan |
| `PUT /api/plans/{id}` | Update plan items |
| `DELETE /api/plans/{id}` | Delete a plan |

### Recurring Payments
| Endpoint | Description |
|----------|-------------|
| `GET /api/subscriptions` | List all active recurring payments |
| `POST /api/subscriptions` | Create a recurring payment |
| `PUT /api/subscriptions/{id}` | Update a recurring payment |
| `DELETE /api/subscriptions/{id}` | Delete (deactivate) a recurring payment |

### Salaries
| Endpoint | Description |
|----------|-------------|
| `GET /api/salaries` | List all active salaries |
| `GET /api/salaries/{id}` | Get a salary with computed net pay |
| `POST /api/salaries` | Create a salary |
| `PUT /api/salaries/{id}` | Update a salary |
| `DELETE /api/salaries/{id}` | Delete (deactivate) a salary |

### Savings Accounts
| Endpoint | Description |
|----------|-------------|
| `GET /api/savings/accounts` | List all active savings accounts |
| `GET /api/savings/accounts/{id}` | Get a savings account |
| `POST /api/savings/accounts` | Create a savings account |
| `PUT /api/savings/accounts/{id}` | Update a savings account |
| `DELETE /api/savings/accounts/{id}` | Deactivate a savings account |
| `GET /api/savings/accounts/pool-balance` | Get total pool balance |
| `GET /api/savings/accounts/{id}/events` | Get account event history |
| `POST /api/savings/accounts/{id}/deposit` | Log a deposit to an account |
| `POST /api/savings/accounts/{id}/withdrawal` | Log a withdrawal from an account |
| `PUT /api/savings/accounts/events/{eventId}` | Update an account deposit or withdrawal event |
| `DELETE /api/savings/accounts/events/{eventId}` | Delete an account deposit or withdrawal event |

### Savings Funds
| Endpoint | Description |
|----------|-------------|
| `GET /api/savings/funds` | List all active funds |
| `GET /api/savings/funds/{id}` | Get a fund |
| `POST /api/savings/funds` | Create a fund |
| `PUT /api/savings/funds/{id}` | Update a fund |
| `DELETE /api/savings/funds/{id}` | Deactivate a fund (must have zero balance) |
| `GET /api/savings/funds/summary` | Get year summary with upcoming deadlines |

### Savings Accounts — Transaction Links
| Endpoint | Description |
|----------|-------------|
| `POST /api/savings/accounts/{id}/link-transaction` | Link a single transaction to an account event |
| `POST /api/savings/accounts/{id}/bulk-link-budget-item` | Bulk-link all transactions for a budget item to an account |

### Savings Events (Fund)
| Endpoint | Description |
|----------|-------------|
| `GET /api/savings/events/fund/{fundId}` | Get event history for a fund |
| `POST /api/savings/events/deposit` | Log a deposit to a fund |
| `POST /api/savings/events/withdrawal` | Log a withdrawal from a fund |
| `POST /api/savings/events/reallocate` | Move money between two funds |
| `POST /api/savings/events/payout/{fundId}` | Process payout for a spend-down fund |
| `PUT /api/savings/events/{id}` | Update a deposit or withdrawal event |
| `DELETE /api/savings/events/{id}` | Delete a deposit or withdrawal event |
| `POST /api/savings/events/link-transaction` | Link a single transaction to a fund event |
| `POST /api/savings/events/bulk-link-budget-item` | Bulk-link all transactions for a budget item to a fund |

### Savings — Link Status
| Endpoint | Description |
|----------|-------------|
| `GET /api/savings/link-status/budget-items` | Batch link status for budget items (`ids`, `startDate`, `endDate`) |

## License

MIT
