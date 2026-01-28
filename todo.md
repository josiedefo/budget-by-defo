# Budget App - Development Log

## Completed Tasks

### Initial Setup
- [x] Initialize Spring Boot backend project with dependencies
- [x] Create entity classes (Budget, Section, BudgetItem)
- [x] Create JPA repositories
- [x] Create service layer with business logic
- [x] Create REST controllers
- [x] Initialize Vue.js frontend with Vuetify
- [x] Create API service and Pinia store
- [x] Build monthly budget view with components
- [x] Build yearly budget view
- [x] Add navigation and polish

### Offline Fallback
- [x] Add default sections fallback when backend unreachable
- [x] Show warning alert in offline mode
- [x] Disable editing controls when offline
- [x] Add isOffline state to budget store

### Database & DevOps
- [x] Add Liquibase for database migrations
- [x] Create changelog files for budget, section, budget_item tables
- [x] Add Docker Compose for PostgreSQL container
- [x] Update Hibernate to validate mode (Liquibase manages schema)

## Implementation Summary

### Backend (Spring Boot)
- Created `Budget`, `Section`, `BudgetItem` entities with JPA annotations
- Implemented repositories with custom queries for fetching budgets with sections/items
- Built services: `BudgetService`, `SectionService`, `BudgetItemService`
- Created REST controllers for all CRUD operations
- Added CORS configuration for frontend communication
- Global exception handler for consistent error responses
- **Liquibase** manages database schema migrations

### Frontend (Vue.js + Vuetify)
- Set up Vue 3 project with Vite, Vuetify 3, Pinia, Vue Router
- Created `MonthlyBudgetView` - displays current month budget with sections
- Created `YearlyBudgetView` - shows yearly summary with all months
- Built components:
  - `MonthSelector` - navigate between months
  - `BudgetSummary` - income/expenses/balance totals
  - `BudgetSection` - displays section with editable items
  - `AddSectionDialog` - add new sections
  - `AddItemDialog` - add new budget items
- Implemented Pinia store for state management
- API service with axios for backend communication
- **Offline fallback** shows default sections when backend unreachable

### DevOps
- **Docker Compose** for PostgreSQL database container
- **Liquibase** changelog files in `backend/src/main/resources/db/changelog/`

## Git Commits

1. `dd2ba29` - Initial commit: Budget tracking app
2. `fb797b4` - Add offline fallback with default sections
3. `ef100a1` - Add Liquibase and Docker for database management

## Future Enhancements

- [ ] Add user authentication
- [ ] Export budget to CSV/PDF
- [ ] Budget templates (copy from previous month)
- [ ] Recurring items
- [ ] Budget categories/tags
- [ ] Charts and visualizations
- [ ] Mobile responsive improvements
- [ ] Dark mode support
