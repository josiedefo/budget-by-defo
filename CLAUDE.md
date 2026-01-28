# Budget App

A personal budget tracking application built with Vue.js + Vuetify frontend and Java Spring Boot + PostgreSQL backend.

## Project Structure

```
budget-by-defo/
├── backend/                    # Spring Boot REST API (Java 17)
│   ├── src/main/java/com/budget/
│   │   ├── controller/         # REST endpoints
│   │   ├── service/            # Business logic
│   │   ├── repository/         # JPA repositories
│   │   ├── model/              # Entity classes (Budget, Section, BudgetItem)
│   │   ├── dto/                # Request/Response DTOs
│   │   └── config/             # CORS, exception handling
│   └── pom.xml
│
└── frontend/                   # Vue 3 + Vuetify 3
    ├── src/
    │   ├── views/              # MonthlyBudgetView, YearlyBudgetView
    │   ├── components/         # BudgetSection, BudgetSummary, dialogs
    │   ├── services/           # API client (axios)
    │   ├── stores/             # Pinia state management
    │   ├── router/             # Vue Router config
    │   └── plugins/            # Vuetify setup
    └── package.json
```

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.2, Spring Data JPA, PostgreSQL
- **Frontend**: Vue 3, Vuetify 3, Pinia, Vue Router, Axios, Vite

## Running the App

### Prerequisites
- Java 17+
- Node.js 18+
- PostgreSQL with database `budget_db`

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

## Database Configuration

Edit `backend/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/budget_db
spring.datasource.username=postgres
spring.datasource.password=postgres
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/budgets/{year}/{month} | Get monthly budget (auto-creates if missing) |
| GET | /api/budgets/{year} | Get yearly summary |
| POST | /api/sections | Add section to budget |
| PUT | /api/sections/{id} | Update section |
| DELETE | /api/sections/{id} | Delete section |
| POST | /api/items | Add item to section |
| PUT | /api/items/{id} | Update item |
| DELETE | /api/items/{id} | Delete item |

## Data Model

- **Budget**: year, month, sections[]
- **Section**: name, isIncome, displayOrder, items[]
- **BudgetItem**: name, plannedAmount, actualAmount, displayOrder

## Default Sections

New budgets are created with these sections:
1. Income (isIncome: true)
2. Housing
3. Transportation
4. Food
5. Utilities
6. Healthcare
7. Entertainment
8. Savings

## Key Features

- Monthly budget view with inline editing
- Yearly overview with month-by-month breakdown
- Track planned vs actual amounts
- Add/edit/delete sections and items
- Auto-navigation to current month on load
