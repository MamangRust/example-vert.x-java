# Vert.x CRUD Example

This is a comprehensive REST API project built with Vert.x and Java, showcasing a clean architecture and modern development practices. It implements a full-featured CRUD application with authentication, role-based access control, and a complete observability stack.

This project has been significantly updated to include Docker for containerization, Flyway for database migrations, and a suite of observability tools.

## Features

-   **Authentication:** User registration and login with JWT (JSON Web Token).
-   **Authorization:** Role-based access control (e.g., admin, user).
-   **CRUD Operations:** Full Create, Read, Update, Delete operations for users and roles.
-   **Database Migrations:** Automated database schema management using Flyway.
-   **Containerized:** Ready to run with Docker and Docker Compose.
-   **Observability:** Integrated with Prometheus for metrics, Loki for logging, and Grafana for visualization.
-   **API Testing:** Comes with `hurl` scripts for easy API endpoint testing.
-   **Clean Architecture:** Well-organized folder structure for better maintainability.

## Folder Structure

The project follows a clean and organized folder structure:

```
/
├── docker-compose.yml
├── Dockerfile
├── hurl/                     # API tests with Hurl
├── observability/            # Configs for Grafana, Prometheus, Loki
├── src/
│   ├── main/
│   │   ├── java/com/sanedge/example_crud/
│   │   │   ├── config/       # Application configuration (DB, JWT, etc.)
│   │   │   ├── domain/       # Request and response objects
│   │   │   ├── handler/      # Request handlers
│   │   │   ├── middleware/   # JWT and role-based middleware
│   │   │   ├── model/        # Data models/entities
│   │   │   ├── repository/   # Data access layer
│   │   │   ├── routes/       # API route definitions
│   │   │   ├── service/      # Business logic
│   │   │   └── starter/      # Main application entrypoint
│   │   └── resources/
│   │       ├── db/migration/ # Flyway SQL migrations
│   │       └── logback.xml   # Logging configuration
│   └── test/
└── pom.xml
```

## Technologies Used

-   Java 17
-   [Vert.x](https://vertx.io/) 4.5.1
-   [Maven](https://maven.apache.org/)
-   [PostgreSQL](https://www.postgresql.org/)
-   [Redis](https://redis.io/)
-   [Flyway](https://flywaydb.org/) for database migrations
-   [Docker](https://www.docker.com/) & [Docker Compose](https://docs.docker.com/compose/)
-   [Hurl](https://hurl.dev/) for API testing
-   **Observability Stack:**
    -   [Prometheus](https://prometheus.io/) for metrics
    -   [Grafana Loki](https://grafana.com/oss/loki/) for logs
    -   [Grafana](https://grafana.com/oss/grafana/) for dashboards
    -   [OpenTelemetry Collector](https://opentelemetry.io/)

## Getting Started

### Prerequisites

-   Java 17+
-   Maven 3.8+
-   Docker and Docker Compose

### Running with Docker Compose (Recommended)

This is the easiest way to get the entire stack up and running, including the database, Redis, and observability tools.

1.  **Build the application JAR:**
    ```bash
    ./mvnw clean package
    ```

2.  **Start all services:**
    ```bash
    docker-compose up -d
    ```

The application will be available at `http://localhost:8888`.

### Running Locally (for Development)

If you prefer to run the application directly on your machine:

1.  **Start Database and Redis:**
    You can use the `docker-compose.yml` to start only the database and Redis:
    ```bash
    docker-compose up -d postgres redis
    ```

2.  **Run the application:**
    ```bash
    ./mvnw clean compile exec:java
    ```

The server will run at `http://localhost:8888`.

## API Testing with Hurl

The `hurl/` directory contains scripts to test the API endpoints.

To run a test, for example `login.hurl`:
```bash
hurl --variable host=http://localhost:8888 hurl/auth/login.hurl
```

## Observability

The included observability stack allows you to monitor the application's health and performance.

-   **Grafana:** `http://localhost:3000` (user: `admin`, password: `admin`)
    -   **Logs Dashboard:** Explore application logs from Loki.
    -   **JVM Dashboard:** Monitor JVM metrics from Prometheus.
-   **Prometheus:** `http://localhost:9090`
-   **Loki:** `http://localhost:3100`

The `observability/` directory contains all the configuration files for these services.