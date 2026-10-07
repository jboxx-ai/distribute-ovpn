# Production Deployment & Docker Infrastructure

## 1. Multi-Stage Container Packaging

The registry server is packaged as an immutable, non-root container using a multi-stage Dockerfile:

```dockerfile
# syntax=docker/dockerfile:1.4
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /src
COPY . .
RUN ./gradlew :backend:registry-server:installDist --no-daemon

FROM eclipse-temurin:17-jre-jammy AS runner
WORKDIR /app
RUN groupadd -g 10001 vpnapp && useradd -u 10001 -g vpnapp -s /bin/sh vpnapp
COPY --from=builder /src/backend/registry-server/build/install/registry-server /app/
USER 10001:10001
ENV PORT=8080
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --retries=3 CMD wget -qO- http://127.0.0.1:8080/healthz || exit 1
ENTRYPOINT ["/app/bin/registry-server"]
```

---

## 2. Docker Compose High-Availability Setup

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: vpn-registry-db
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-vpn_registry}
      POSTGRES_USER: ${POSTGRES_USER:-vpn_user}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?Database password required}
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER:-vpn_user} -d $${POSTGRES_DB:-vpn_registry}"]
      interval: 10s
      timeout: 5s
      retries: 5
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 512M

  registry:
    build:
      context: ../..
      dockerfile: infra/docker/Dockerfile.registry
    container_name: vpn-registry-server
    restart: unless-stopped
    environment:
      PORT: 8080
      JDBC_DATABASE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-vpn_registry}
      JDBC_DRIVER: org.postgresql.Driver
      DB_USER: ${POSTGRES_USER:-vpn_user}
      DB_PASSWORD: ${POSTGRES_PASSWORD:?Database password required}
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1024M

volumes:
  pgdata:
```
