# AutoTestX — Setup & Execution Guide

This guide covers how to run the BookVerse API and AutoTestX framework on your local machine.

## Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose (optional but recommended)
- Git

---

## 🚀 Option 1: The "One Command" Docker Way (Recommended)

This is the easiest way to run everything. It will spin up PostgreSQL, build/start the BookVerse API, and run the AutoTestX suite.

```bash
# 1. Clone the repository
git clone https://github.com/yourusername/AutoTestX.git
cd AutoTestX

# 2. Run the full stack
docker compose up --build
```

**What this does:**
1. Starts `postgres:15-alpine` and creates the `bookversedb` database.
2. Builds and starts `bookverse-api` on port `8080`.
3. Waits for the API to be healthy.
4. Builds and runs `autotestx` to execute the full test suite.
5. Generates Extent Reports in the `autotestx/reports/` folder.

---

## 💻 Option 2: Running Locally (For Development)

If you want to run the application and tests on your host machine without Docker:

### 1. Start PostgreSQL
You can either install PostgreSQL locally or run it via Docker:
```bash
docker run --name bookverse-db \
  -e POSTGRES_DB=bookversedb \
  -e POSTGRES_USER=bookverse \
  -e POSTGRES_PASSWORD=bookverse123 \
  -p 5432:5432 \
  -d postgres:15-alpine
```

### 2. Start the BookVerse API
```bash
cd bookverse-api
mvn clean spring-boot:run
```
The API will start on `http://localhost:8080`. Flyway will automatically run database migrations (creating tables and inserting seed data).

### 3. Run the AutoTestX Test Suites
Open a new terminal window:
```bash
cd autotestx

# Run all tests using the 'qa' environment configuration
mvn clean test -Denv=qa

# Or run tests using the 'local' environment configuration
mvn clean test -Denv=local
```

---

## ⚙️ Test Execution Options

### Running Specific Suites
You can specify which XML suite file to run:
```bash
# Run the full regression suite (150+ tests)
mvn test -DsuiteFile=testng.xml

# Run only a subset of critical tests
mvn test -DsuiteFile=testng-regression.xml
```

### Running Specific Test Classes
To run a specific test class from the command line:
```bash
mvn test -Dtest=LoginTests
```

### Environment Switching
AutoTestX supports environment switching via the `-Denv` system property. Configuration properties are loaded from `src/test/resources/config/{env}.properties`.
```bash
mvn test -Denv=local   # Uses local.properties
mvn test -Denv=qa      # Uses qa.properties
mvn test -Denv=docker  # Uses docker.properties
```

---

## 📊 Viewing Test Reports

### Extent Reports (HTML Dashboard)
After running tests, a rich HTML report is generated.
- Location: `autotestx/reports/extent-report-YYYY-MM-DD_HH-mm-ss.html`
- Open this file in any web browser to view detailed test results, logs, and system information.

### Allure Reports (History & Trends)
If you prefer Allure, AutoTestX is configured to generate Allure results.
```bash
cd autotestx
# Ensure tests have run and target/allure-results exists
mvn allure:serve
```
This will start a local web server and open the Allure dashboard in your browser.

---

## 🔧 Troubleshooting

### "Connection refused" when running tests locally
Make sure the `bookverse-api` is running on port 8080 and that the environment property (`-Denv=qa` or `-Denv=local`) points to `http://localhost:8080`.

### Database Authentication Failed
Ensure PostgreSQL is running on port 5432 with the credentials:
- Username: `bookverse`
- Password: `bookverse123`
- Database: `bookversedb`

### Tests timing out
If the BookVerse API is slow to start, increase the timeout configuration in your target environment's `.properties` file (e.g., `request.timeout.ms=30000`).
