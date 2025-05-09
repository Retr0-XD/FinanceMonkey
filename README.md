

## 📘 README.md — Finance Microservices App

### 🧾 Overview

This project is a **Finance Monkey** built using **Spring Boot microservices** architecture. It allows users to track bank transactions via mail parsing and visualize their expenses.

---

### 📂 Services

| Service                | Port | Description                                |
| ---------------------- | ---- | ------------------------------------------ |
| `auth-service`         | 8081 | Manages user registration & authentication |
| `parser-service`       | 8082 | Parses bank transaction emails             |
| `transactions-service` | 8083 | Handles categorized transactions           |
| `report-service`       | 8084 | Provides financial summaries & charts      |

---

### 🧰 Tech Stack

* Spring Boot (microservices)
* Supabase (auth + DB)
* Docker (for containerization)
* Render (for deployment)
* Vercel (frontend deployment, optional)
* GitHub Codespaces (dev environment)

---

## 🛠️ Local Development (Codespaces or Local Docker)

### 📦 Prerequisites

* GitHub Codespaces **or**
* Docker & Docker Compose installed
* Supabase project created

---

### 🔧 Setup Steps

1. **Clone the Repo**

```bash
git clone https://github.com/your-username/finance-microservices.git
cd finance-microservices
```

2. **Supabase Setup**

Create a Supabase project and get:

* `SUPABASE_URL`
* `SUPABASE_ANON_KEY`

Store these in each service's `.env` or set them in Docker `environment:` section.

3. **Build Services Locally**

```bash
# Build all services
./mvnw clean install
```

4. **Run Locally with Docker Compose**

```bash
docker-compose up --build
```

Each service will run at:

* `localhost:8081` → Auth
* `localhost:8082` → Mail Parser
* `localhost:8083` → Transactions
* `localhost:8084` → Reports

---

## 🚀 Deployment (Docker on Render)

### 1. **Render Web Service per Microservice**

* Go to [Render](https://render.com)
* Create a new **Web Service** → Choose **Docker**
* Connect your repo, set **root directory** of each service
* Set `PORT` environment variable (e.g., `8081`, `8082`, etc.)
* Set Supabase env variables

Repeat for all services.

---

### 2. **Avoid Idle Spin-down (Optional)**

Set up a GitHub Action or use a free uptime service like [UptimeRobot](https://uptimerobot.com) to ping your service’s `/health` endpoint every 5 mins.

---

### 3. **CI/CD (Optional)**

Use GitHub Actions to build Docker images and push to Render automatically.

```yaml
# .github/workflows/docker-build.yml
name: Deploy Service

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - run: docker build -t your-service-name ./auth-service
```

---

## 📥 Mail Parsing Flow (Bank Mails)

* Users log in via Supabase Auth
* App connects to their Gmail using OAuth (coming soon)
* Parses emails from banks (filtered by keyword & sender)
* Extracts UPI, Card, and Netbanking transaction details

---

## 🔐 .env Template

Create a `.env` file inside each service:

```env
SUPABASE_URL=https://xyz.supabase.co
SUPABASE_KEY=your-supabase-anon-key
PORT=8081
```

---

## 📁 Folder Structure

```
finance-microservices/
├── auth-service/
│   ├── Dockerfile
│   └── src/
├── parser-service/
│   ├── Dockerfile
│   └── src/
├── transactions-service/
│   ├── Dockerfile
│   └── src/
├── report-service/
│   ├── Dockerfile
│   └── src/
├── docker-compose.yml
└── README.md
```

---

## 👨‍💻 Dev Tips

* Use GitHub Codespaces for full Docker-based dev & testing.
* Test services with Postman or cURL.
* Logs from each service will be visible in the Docker terminal.
* Supabase has a free-tier generous enough for small apps.

---

## 🧹 TODOs

* [ ] Add Gmail OAuth for auto-fetching mails
* [ ] Add Vercel frontend to show visual analytics
* [ ] Add scheduling job for periodic email parsing
* [ ] Add user profile management (optional)

---
