# Budget App

A personal budget tracking application to manage your finances month by month.

## Features

- **Monthly Budget View** - Track income and expenses for each month
- **Yearly Overview** - See your financial summary across all months
- **Planned vs Actual** - Compare what you budgeted against actual spending
- **Custom Sections** - Add your own budget categories
- **Default Categories** - Comes with Income, Housing, Transportation, Food, Utilities, Healthcare, Entertainment, and Savings

## Screenshots

The app opens to the current month's budget by default, showing all sections with their budget items. You can:
- Edit planned and actual amounts inline
- Add new sections and items
- Navigate between months using the selector
- Switch to yearly view to see the big picture

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
├── backend/          # Spring Boot REST API
├── frontend/         # Vue.js application
├── CLAUDE.md         # Technical documentation
├── README.md         # This file
└── todo.md           # Development log
```

## API Overview

| Endpoint | Description |
|----------|-------------|
| `GET /api/budgets/{year}/{month}` | Get or create monthly budget |
| `GET /api/budgets/{year}` | Get yearly summary |
| `POST /api/sections` | Add a new section |
| `POST /api/items` | Add a new budget item |
| `PUT /api/items/{id}` | Update a budget item |
| `DELETE /api/items/{id}` | Delete a budget item |

## License

MIT
