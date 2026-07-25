# 🚀 OpsPulse — Enterprise AI-Driven SRE & Incident Management Platform

<p align="center">
  <img src="https://img.shields.io/badge/Next.js-16.2-black?style=for-the-badge&logo=next.js" alt="Next.js" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.3-brightgreen?style=for-the-badge&logo=spring" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/PostgreSQL-17-blue?style=for-the-badge&logo=postgresql" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Google_Gemini-2.0_Flash-orange?style=for-the-badge&logo=google" alt="Google Gemini AI" />
  <img src="https://img.shields.io/badge/TypeScript-5.0-blue?style=for-the-badge&logo=typescript" alt="TypeScript" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="License" />
</p>

**OpsPulse** is a full-stack, enterprise-grade Site Reliability Engineering (SRE) and Incident Management platform. It combines real-time error ingestion, role-based triage workflows, automated GitHub Actions CI/CD webhook handling, Java Spring Boot SLA engines, and **Google Gemini 2.0 Flash AI** to automatically diagnose root causes and generate step-by-step code fixes for application crashes.

---

## 🌟 Key Features

- 🤖 **Automated AI Root-Cause Engine**: Powered by Google Gemini 2.0 Flash to analyze error stack traces, assign risk scores, and recommend step-by-step code fixes.
- 👥 **Role-Based Operations Portals**:
  - **Admin Console (`/dashboard/admin`)**: Organization setup, project API key generation, team creation, and audit logging.
  - **Manager Operations Center (`/dashboard/manager`)**: Project telemetry overview, SLA deadline tracking, and developer assignment routing.
  - **Developer Triage Workspace (`/dashboard/developer`)**: Personal incident queue, status progression (`OPEN` ➔ `ASSIGNED` ➔ `IN_PROGRESS` ➔ `RESOLVED`), and resolution history.
- ⚡ **Multi-Language Telemetry Ingestion (`/api/ingest`)**: Real-time crash report collection for Java, Node.js, React, Python, Go, and mobile applications with rate-limiting and deduplication.
- 🔗 **GitHub Webhooks & CI/CD Tracking**: Cryptographically signed (`HMAC-SHA256`) GitHub webhooks that automatically create CRITICAL SLA incidents when GitHub Actions workflows (`workflow_run`) fail.
- ☕ **Java 21 Spring Boot SLA Engine**: Real-time SLA response/resolution deadline monitoring, background monitors, and OpenAPI 3.0 / Swagger UI documentation (`http://localhost:8080/swagger-ui.html`).
- 📧 **Gmail SMTP Notification Pipeline**: Automated verification emails, organization invites, and SLA escalation notifications.

---

## 🏗️ Architecture & Monorepo Structure

```text
OpsPulse/
├── incident-management-system/    # Next.js 16 App Router Frontend & Node API Services
│   ├── src/
│   │   ├── app/                    # Web pages, API endpoints, role dashboard routes
│   │   ├── components/             # React Tailwind components (StatCards, Sidebars, Modals)
│   │   ├── lib/                    # Gemini AI service, JWT auth, database client, mailer
│   │   └── services/               # Issue, Project, Organization, and Notification services
│   ├── prisma/                     # Schema & migrations (@opspulse/prisma-client)
│   └── scripts/                    # Database seed scripts & test tools
│
├── opspulse-backend/               # Java 21 Spring Boot 3 Engine & REST Controllers
│   ├── src/main/java/com/opspulse/
│   │   ├── controller/             # Auth, Issue, Project, Webhook, Health controllers
│   │   ├── entity/                 # JPA database entities & Enums
│   │   ├── repository/             # Spring Data JPA repositories
│   │   └── service/                # SLA calculation rules, AI integration, Auth services
│   └── src/main/resources/         # application.yml configuration
│
└── sdk/                            # @opspulse/sdk Client Error Tracking Library
    └── index.ts                    # Auto-capture, breadcrumbs, and offline queueing
```

---

## 🔐 Pre-Seeded Test Credentials

After running the database seed script (`npm run seed`), you can log in with the following default accounts at `http://localhost:3000/auth/login`:

| Role | Email | Password | Access Level |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin@opspulse.io` | `Password123!` | Full Organization & System Access |
| **Manager** | `manager@opspulse.io` | `Password123!` | Project Operations & Developer Routing |
| **Developer** | `dev@opspulse.io` | `Password123!` | Personal Queue & Incident Resolution Workspace |

---

## ⚡ Quick-Start Guide

### Prerequisites

- **Node.js**: v18.0.0 or higher
- **Java JDK**: 21 or higher
- **Maven**: 3.8 or higher
- **PostgreSQL**: Local or cloud database running on port `5432`

---

### 1. Database Setup

Create a PostgreSQL database named `opspulse`:

```sql
CREATE DATABASE opspulse;
CREATE USER opspulse WITH PASSWORD 'opspulse_secret';
GRANT ALL PRIVILEGES ON DATABASE opspulse TO opspulse;
```

---

### 2. Frontend & Next.js Server

```powershell
cd incident-management-system

# Install dependencies
npm install

# Push Prisma schema to database
npx prisma db push

# Seed initial organization and users
npm run seed

# Start Next.js development server
npm run dev
```

The web application will start at **`http://localhost:3000`**.

---

### 3. Java Spring Boot Engine

```powershell
cd opspulse-backend

# Run Spring Boot application
mvn spring-boot:run
```

The Java engine will start at **`http://localhost:8080`**.
Interactive Swagger documentation is available at **`http://localhost:8080/swagger-ui.html`**.

---

### 4. Setting Environment Variables

Create `.env` inside `incident-management-system/`:

```env
DATABASE_URL="postgresql://opspulse:opspulse_secret@localhost:5432/opspulse"
JWT_SECRET="3cb623335e4503e4bdc2dcb376d373e4d83434af43a16b35013dc5bd5f258702"
GEMINI_API_KEY="YOUR_GEMINI_API_KEY"
GITHUB_WEBHOOK_SECRET="opspulse_github_secret_12345"
NEXT_PUBLIC_APP_URL="http://localhost:3000"
GMAIL_USER="your_email@gmail.com"
GMAIL_APP_PASSWORD="your_gmail_app_password"
```

---

## 📦 Client SDK Integration (`@opspulse/sdk`)

In any Node.js, React, or TypeScript project, install the SDK to start monitoring errors:

```bash
npm install opspulse-sdk
```

Initialize at app startup:

```typescript
import { OpsPulse, IssueSeverity } from 'opspulse-sdk';

OpsPulse.init({
  apiKey: 'opspulse_sk_demo_key_12345',
  baseUrl: 'http://localhost:3000/api/ingest',
});

// Capture manual exception
try {
  processPayment();
} catch (error) {
  OpsPulse.captureException(error, {
    severity: IssueSeverity.HIGH,
    tags: { component: 'checkout' }
  });
}
```

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more details.
