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

### Transactions
- **Transaction Tracking** - Record individual transactions with date, amount, description, and type
- **Link to Budget** - Associate transactions with budget sections and items
- **CSV Import** - Bulk import transactions from CSV files
- **Click-to-View** - Click on actual amounts to view related transactions

### Planner
- **Monthly Plans** - Create detailed plans for individual budget items
- **Plan Items** - Break down planned amounts into specific line items
- **Auto-Sync** - Plan totals automatically update budget item planned amounts

### Recurring Payments
- **Recurring Payment Templates** - Define recurring payments once (Netflix, Gym, etc.)
- **Quick Add to Plans** - Easily add recurring payments to any plan
- **Recurrence Options** - Weekly, Monthly, Quarterly, Semi-Annually, or Yearly
- **Visual Indicator** - Plan items from recurring payments show a repeat icon

### Salaries
- **Salary Templates** - Define salary entries with gross pay and payroll deductions
- **Payroll Deductions** - Track Federal Tax, Medicare, Social Security, 401K, HSA, Medical Insurance, FSA, and extra tax withholding
- **Net Pay Calculation** - Automatically calculates net pay from gross minus all deductions
- **Quick Add to Plans** - Add salaries to plans as income items using the computed net pay
- **Visual Indicator** - Plan items from salaries show a dollar icon

## Screenshots

The app opens to the current month's budget by default, showing all sections with their budget items. You can:
- Edit planned and actual amounts inline
- Add new sections and items
- Navigate between months using the selector
- Switch to yearly view to see the big picture
- Access the Planner to create detailed monthly plans
- Manage recurring payments from the Planner

## Tech Stack

| Layer | Technology |
|-------|------------|
| Frontend | Vue 3, Vuetify 3, Pinia, Vue Router |
| Backend | Java 17, Spring Boot 3.2, Spring Data JPA |
| Database | PostgreSQL |

## Prerequisites

- Java 17 or higher
- Node.js 18 or higher
- Docker and Docker Compose

## Getting Started

### Option 1: Run Everything with Docker (Recommended)

Build and start the entire application:

```bash
docker-compose up --build
```

This starts:
- PostgreSQL database on port 5433
- Budget app (frontend + backend) on port 8080

Access the app at http://localhost:8080

### Option 2: Run Services Separately

#### 1. Start the Database

```bash
docker-compose up postgres -d
```

#### 2. Run the Backend

```bash
cd backend
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

#### 3. Run the Frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173` in your browser.

## Project Structure

```
budget-by-defo/
├── backend/                    # Spring Boot REST API
│   └── src/main/java/com/budget/
│       ├── controller/         # REST endpoints
│       ├── service/            # Business logic
│       ├── repository/         # JPA repositories
│       ├── model/              # Entity classes
│       └── dto/                # Request/Response DTOs
├── frontend/                   # Vue.js application
│   └── src/
│       ├── views/              # Page components
│       ├── components/         # Reusable components
│       ├── stores/             # Pinia state management
│       └── services/           # API client
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
| `GET /api/transactions` | List transactions with filters |
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

## License

MIT
