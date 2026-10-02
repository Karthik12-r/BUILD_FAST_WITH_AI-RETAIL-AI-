# RetailOps AI

Retail operations dashboard backed by the existing MySQL `retailops_ai` database. The application reads imported tables and does not create or drop schema objects.

## Requirements

- Java 25
- MySQL 8 with the existing `retailops_ai` schema and imported sales, supplier, and approval rows
- Node.js 22 or later for the React dashboard

Database credentials are supplied through the `DB_USERNAME` and `DB_PASSWORD` environment variables. Keep local credentials private; do not paste them into source control.

## Run the backend

From the project root in PowerShell:

```powershell
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = 'your-local-mysql-password'
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd spring-boot:run
```

The configured backend port is `8081`. If it is occupied, use a temporary alternate port:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--server.port=8082'
```

## Run the dashboard

In a second terminal:

```powershell
Set-Location frontend
npm install
$env:VITE_API_TARGET = 'http://localhost:8081'
npm run dev
```

Set `VITE_API_TARGET` to the backend's alternate port when needed. Vite prints the dashboard URL when ready.

## APIs

- `GET /api/analytics/dashboard`: full-history revenue/units, recent monthly trend, top products, reorder alerts, pending approvals, supplier count
- `GET /api/sales?page=0&size=100` and `GET /api/sales/{id}`: bounded sales pages and records
- `GET /api/suppliers` and `GET /api/suppliers/{supplierId}`
- `GET /api/approvals` and `GET /api/approvals/{actionId}`
- `POST /api/approvals/generate`: create missing pending reorder actions; repeat calls skip existing pending store/product pairs
- `PUT /api/approvals/{actionId}/approve` and `/reject`: JSON body requires `approver`; rejection also requires `managerComment`
- `GET /api/recommendations`: current recommendations from each store/product's latest sales row and supplier terms
- `POST /api/assistant/chat`: optional provider-backed answer using current application data

Reorder coverage compares current inventory with `daily demand × supplier lead time`. Suggested quantity is the larger of the coverage shortage and supplier minimum order quantity. Recommendations with an existing pending action are suppressed. Approval decisions are manager-controlled and audited.

## Optional AI provider

The dashboard's assistant is disabled until an OpenAI-compatible provider key is configured. Set these process environment variables before starting Spring Boot:

- `AI_API_KEY`: provider secret; never put it in Java source or commit it
- `AI_API_URL`: chat-completions URL; defaults to `https://api.openai.com/v1/chat/completions`
- `AI_MODEL`: model name; defaults to `gpt-4o-mini`

Without a key, the endpoint returns HTTP 503 with a setup message. Core dashboard and retail APIs continue to work. Assistant context excludes supplier email/phone and manager audit comments; the assistant cannot approve or reject actions.

## Verify

```powershell
.\mvnw.cmd clean test
```

The integration suite expects the local MySQL database and imported demo rows to be available. Approval-write tests run transactionally and roll back their changes.
