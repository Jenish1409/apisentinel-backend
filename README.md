# ApiSentinel - Backend

This is the backend service for **ApiSentinel**, a monitoring and observability platform that allows users to register their REST APIs, track their uptime, and view historical analytics. 

It is built with **Spring Boot 3** and uses a reactive `WebClient` scheduling engine to periodically ping registered APIs and log incidents (downtime/recoveries) into a PostgreSQL database.

## Technologies Used

- **Framework**: Spring Boot 3
- **Language**: Java 21
- **Database**: PostgreSQL (via Spring Data JPA / Hibernate)
- **Security**: Spring Security with JWT Authentication
- **HTTP Client**: Spring WebFlux (WebClient) for reactive, non-blocking API polling
- **Build Tool**: Maven (`mvnw`)
- **Other**: Java Mail Sender for contact forms

## Prerequisites

- **Java 21** or higher
- **PostgreSQL** (running locally or remotely)
- **Maven** (optional, you can use the included wrapper `./mvnw`)

## Setup & Configuration

The application is configured via `src/main/resources/application.properties`. For security, most sensitive properties are injected via Environment Variables.

Before running the application, set up the following environment variables (or supply them in an IDE run configuration, or an `.env` file if you have a loader):

### Database Configuration
- `SPRING_DATASOURCE_URL`: e.g., `jdbc:postgresql://localhost:5432/apisentinel`
- `SPRING_DATASOURCE_USERNAME`: your postgres username
- `SPRING_DATASOURCE_PASSWORD`: your postgres password

### Security Configuration
- `APISENTINEL_APP_JWT_SECRET`: A Hex encoded 256-bit secret key for HMAC-SHA256 (used for signing JWTs).
- `APP_ENCRYPTION_SECRET_KEY`: A secret key used for encrypting sensitive data in the database (if applicable).

### Email Configuration (Optional - for Contact Form)
- `SPRING_MAIL_PASSWORD`: SMTP password for Brevo (or your configured mail server).
- `APISENTINEL_MAIL_CONTACT_TARGET`: The email address that should receive contact form submissions.

### Miscellaneous
- `PORT`: The port the server runs on (defaults to 8080).
- `APISENTINEL_CORS_ALLOWED_ORIGINS`: The frontend URL allowed to make requests (defaults to `http://localhost:5173`).

## Running the Application

1. **Clone the repository** and navigate to the `backend` directory.
2. **Ensure PostgreSQL is running** and the target database exists.
3. **Execute the Maven Spring Boot plugin**:

### On Linux / macOS:
```bash
./mvnw spring-boot:run
```

### On Windows:
```cmd
mvnw.cmd spring-boot:run
```

The API will start and be available at `http://localhost:8080` (or your configured `PORT`).

## Key Endpoints

- `POST /auth/register`: Register a new user
- `POST /auth/login`: Authenticate and receive a JWT
- `GET /apis`: Fetch monitored APIs for the current user
- `POST /apis`: Add a new API to monitor
- `GET /apis/{id}/history`: Retrieve ping history for charting
- `GET /apis/{id}/incidents`: Retrieve downtime/recovery incidents
- `GET /health`: System health check

## Deployment

The application includes a `Dockerfile` and a `render.yaml` for easy deployment to container registries or PaaS platforms like Render. Ensure you map all the required environment variables in your deployment environment.
