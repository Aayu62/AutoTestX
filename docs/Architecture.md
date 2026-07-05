# AutoTestX — Architecture

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         DOCKER COMPOSE                              │
│                                                                     │
│  ┌──────────────┐    ┌──────────────────┐    ┌─────────────────┐   │
│  │  PostgreSQL  │◄───│  BookVerse API   │◄───│   AutoTestX    │   │
│  │     :5432    │    │   Spring Boot    │    │  Test Runner   │   │
│  │              │    │     :8080        │    │   Maven/Java   │   │
│  │  bookversedb │    │                  │    │                │   │
│  │  bookverse   │    │  /api/auth       │    │  150+ Tests    │   │
│  │  bookverse   │    │  /api/books      │    │  Extent Report │   │
│  │   123        │    │  /api/orders     │    │  Allure Report │   │
│  └──────────────┘    │  /api/reviews    │    └─────────────────┘   │
│       ▲              │  /swagger-ui     │            │             │
│       │              └──────────────────┘            │             │
│       └────────────────────────────────────────────┘             │
│                    JDBC Direct DB Validation                        │
└─────────────────────────────────────────────────────────────────────┘
              │                    │                    │
              ▼                    ▼                    ▼
      GitHub Actions          Extent HTML          Allure Pages
         CI/CD               Reports Dir          GitHub Pages
```

## Automation Framework Layers

```
┌─────────────────────────────────────────────────────────────┐
│                       TEST LAYER                            │
│   RegisterTests  LoginTests  BookCrudTests  OrderTests ...  │
│   SecurityTests  ReviewTests  DBValidationTests             │
└───────────────────────┬─────────────────────────────────────┘
                        │ uses
┌───────────────────────▼─────────────────────────────────────┐
│                    ASSERTION LAYER                          │
│   APIAssert.verifyStatus()      APIAssert.verifySchema()    │
│   APIAssert.verifyBookCreated() APIAssert.verifyResponseTime │
└───────────────────────┬─────────────────────────────────────┘
                        │ uses
┌───────────────────────▼─────────────────────────────────────┐
│                     API LAYER                               │
│   AuthAPI   BookAPI   OrderAPI   ReviewAPI   UserAPI        │
│             (wraps Rest Assured requests)                   │
└───────────────────────┬─────────────────────────────────────┘
                        │ extends
┌───────────────────────▼─────────────────────────────────────┐
│                    BASE API LAYER                           │
│   BaseAPI — RequestSpecification, logging filters, headers  │
└─────────────────────────────────────────────────────────────┘

┌──────────────────── UTILITY LAYER ──────────────────────────┐
│  ConfigReader    → Reads env-specific .properties           │
│  TokenManager   → Singleton JWT, auto-refresh               │
│  DBUtils        → JDBC PostgreSQL validation queries        │
│  ExcelReader    → Apache POI DDT test data                  │
│  JsonReader     → Jackson JSON test data                    │
│  FakerUtils     → JavaFaker test data generation            │
│  ReportManager  → Extent Reports singleton                  │
└─────────────────────────────────────────────────────────────┘

┌────────────────── LISTENER LAYER ───────────────────────────┐
│  TestListener   → Wires test events to Extent + Log4j2      │
│  RetryAnalyzer  → Auto-retry flaky tests (3 max)            │
└─────────────────────────────────────────────────────────────┘
```

## Data Flow: Request Lifecycle

```
Test Method
    │
    ▼
API Class (e.g., BookAPI.createBook)
    │
    ▼
BaseAPI.authSpec(token)         ← RequestSpecification with JWT
    │
    ▼
Rest Assured  ──────────────────────────────────►  BookVerse API
    │                    HTTP POST /api/books           │
    │         ◄──────────────────────────────────       │
    │                    JSON Response                  │
    ▼                                                   │
APIAssert.verifyBookCreated(response)                   │
    │                                                   │
    ▼                                                   ▼
DBUtils.verifyBookExists(id)       ──► PostgreSQL: SELECT * FROM books WHERE id=?
    │
    ▼
ReportManager / Log4j2
    │
    ▼
Extent Report + Allure Results
```

## Database Schema

```
users (1) ─────────── (M) orders ─────────── (M) order_items ─── (M) books (1) ─── (M) reviews
                                                                       │
                                                                       ├── (M) wishlists
                                                                       │
users (1) ─────────── (M) reviews                                      │
                                                               authors (1) ──── (M) books
users (1) ─────────── (M) wishlists                                    │
                                                             categories (1) ── (M) books
users (1) ─────────── (M) refresh_tokens
```

## CI/CD Pipeline Flow

```
Developer Push
      │
      ▼
GitHub Actions Trigger
      │
      ├──► Job 1: Build API
      │         mvn package -DskipTests
      │         Upload JAR as artifact
      │
      └──► Job 2: Run Tests (after build)
                │
                ├── Start PostgreSQL (service container)
                ├── Download API JAR
                ├── java -jar bookverse-api.jar
                ├── Wait for /actuator/health
                ├── mvn test -Denv=qa
                ├── Upload Extent Reports (artifact, 30 days)
                ├── Upload Allure Results (artifact, 30 days)
                └── Upload Test Logs (artifact, 7 days)

                If branch == main:
                └──► Job 3: Publish Allure → GitHub Pages
```
