# Cinevora Backend

Spring Boot 3 / Java 21 REST API for the Cinevora web application.

## Run locally

1. Start PostgreSQL from the repository root:

   ```powershell
   docker compose up -d postgres
   ```

2. Start the API from this directory. Flyway runs `V1__init_schema.sql` and
   `V2__seed_data.sql` automatically on the first connection:

   ```powershell
   mvn spring-boot:run
   ```

   The default development connection is `jdbc:postgresql://localhost:5432/cinevora_db`
   with `postgres/postgres`. Set `SPRING_PROFILES_ACTIVE=prod` and the
   `DATABASE_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and
   `CORS_ALLOWED_ORIGINS` environment variables for production.

3. Open Swagger at <http://localhost:8080/swagger-ui.html>.

All application endpoints use the `/api/v1` prefix. Authentication returns a
JWT; send it as `Authorization: Bearer <token>`. Public reads are movie and
category browsing. Category/movie mutations and statistics require `ADMIN`.
