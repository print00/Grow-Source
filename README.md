# Restaurant P&L MVP

A focused first version of a restaurant profit-and-loss dashboard for small owners.

The MVP flow is:

1. Create/select a restaurant.
2. Upload bank CSV data.
3. Normalize transactions.
4. Categorize vendors and transactions.
5. Ask the owner to confirm questionable categories.
6. Calculate deterministic P&L metrics in the backend.
7. Show a clear dashboard, transaction review, P&L, insights, and Ask screen.

## Project Structure

- `frontend/` - Next.js + TypeScript owner-facing dashboard.
- `backend/` - Java Spring Boot modular monolith API with deterministic finance calculations.
- `db/` - PostgreSQL schema and demo seed data.

## Run Locally

Backend:

```bash
cd backend
mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:3000`.

Demo login:

```text
owner@harborspoon.test
TableProof123!
```

Production-like local stack:

```bash
docker compose up --build
```

## Stage 2 Cloud Setup

The app is now split into two deployable services:

- Frontend: deploy `frontend/` to Vercel and set `NEXT_PUBLIC_API_URL` to the backend URL.
- Backend: deploy `backend/` as a Docker web service and set `FRONTEND_ORIGIN`, `JWT_SECRET`, and PostgreSQL connection variables.
- Database: use PostgreSQL and run `db/schema.sql`; `db/seed-demo.sql` is only for demo data.

`render.yaml` is included as a ready starting point for a Spring Boot API plus managed PostgreSQL. The frontend has `frontend/vercel.json` for Vercel.

## CSV Import Rules

The backend stores every transaction from the restaurant's point of view:

- Positive amount means money came into the restaurant.
- Negative amount means money left the restaurant.

During upload, choose the source account:

- `Bank / debit card`: positive signed amounts are treated as money in and negative signed amounts as money out.
- `Credit card`: positive signed amounts are treated as card purchases, so they are normalized to money out.
- Separate `Debit` and `Credit` columns are also supported. Debit/charge/purchase columns become money out; credit/deposit/refund/payment columns become money in.

## Product Boundaries

Included in this first version:

- Restaurant setup/demo restaurant.
- CSV upload endpoint and UI affordance.
- CSV normalization for bank/debit-card exports, credit-card exports, signed amount columns, and separate debit/credit columns.
- Transaction normalization and vendor/category mapping.
- Owner review for questionable categories.
- Revenue, food cost %, labor cost %, prime cost, prime cost %, net profit, profit margin, average daily sales.
- Month-over-month comparisons, top vendors, and unusual expense changes.
- AI-style Ask screen that only explains calculated facts.

Intentionally out of scope:

- Payroll processing.
- Tax filing.
- Inventory management.
- Invoicing.
- Scheduling.
- Payment processing.
