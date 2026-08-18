# Production Deployment Guide: Collaborative C++ Code Editor

This document provides a comprehensive guide to containerizing and deploying the Real-Time Collaborative C++ Code Editor in a production environment. 

The application architecture consists of five main components:
1. **Frontend**: Next.js 14 App Router, Monaco Editor, Yjs.
2. **Backend**: Spring Boot API & WebSocket server, Yjs state synchronizer.
3. **Execution Worker**: Spring Boot listener that processes C++ runs from Kafka.
4. **Execution Sandbox**: Short-lived `cpp-runner` Docker containers managed by the Worker.
5. **Infrastructure**: PostgreSQL 16 database and Apache Kafka (KRaft mode).

---

## 1. Architectural Overview for Production

In production, all services should run behind a reverse proxy (like **Nginx**) that handles SSL termination (HTTPS/WSS) and routes traffic to the appropriate containers:

```text
                       HTTPS / WSS (Port 443)
                                 │
                                 ▼
                         ┌───────────────┐
                         │     Nginx     │
                         └───────┬───────┘
                                 │
            ┌────────────────────┼────────────────────┐
            │ /                  │ /api               │ /ws
            ▼                    ▼                    ▼
     ┌─────────────┐      ┌─────────────┐      ┌─────────────┐
     │   Next.js   │      │ Spring Boot │      │ Spring Boot │
     │  Frontend   │      │   Backend   │      │ WebSocket   │
     │ (Port 3000) │      │ (Port 8080) │      │ (Port 8080) │
     └─────────────┘      └─────────────┘      └─────────────┘
                                 │
                         ┌───────┴───────┐
                         │ Apache Kafka  │ (Execution Queue)
                         └───────┬───────┘
                                 │
                                 ▼
                          ┌─────────────┐      Docker Daemon socket mount
                          │  Execution  │ ───> /var/run/docker.sock
                          │   Worker    │ ───> (Spawns "cpp-runner"
                          │ (Port 8081) │       sibling containers)
                          └─────────────┘
```

> [!IMPORTANT]
> **Docker-out-of-Docker (DooD)**: Because the **Execution Worker** uses Java's `ProcessBuilder` to run `docker run --rm ...` commands, it needs Docker CLI installed inside its container and access to the host's Docker socket `/var/run/docker.sock`. This allows the worker container to spin up temporary `cpp-runner` containers side-by-side (sibling containers) on the host machine.

---

## 2. Service Dockerfiles

Create the following `Dockerfile` configurations in their respective directories.

### A. Backend Dockerfile
Create [`backend/Dockerfile`](file:///d:/project-codeRoom/backend/Dockerfile):

```dockerfile
# Stage 1: Build JAR using Maven
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy dependency definition and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Minimal Runtime JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### B. Worker Dockerfile
Create [`worker/Dockerfile`](file:///d:/project-codeRoom/worker/Dockerfile). Note the installation of `docker-cli` so the worker can invoke `docker` commands.

```dockerfile
# Stage 1: Build JAR using Maven
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy dependency definition and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: JRE Runtime with Docker CLI installed
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install Docker CLI tool for Docker-out-of-Docker execution
RUN apk add --no-cache docker-cli

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### C. Frontend Dockerfile
Create [`frontend/Dockerfile`](file:///d:/project-codeRoom/frontend/Dockerfile):

```dockerfile
# Stage 1: Build Next.js
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
# Note: Next.js reads public environment variables during build time.
# If you configure variables in .env, they will be baked in.
RUN npm run build

# Stage 2: Production Server
FROM node:20-alpine
WORKDIR /app

ENV NODE_ENV=production

COPY --from=build /app/package*.json ./
COPY --from=build /app/.next ./.next
COPY --from=build /app/public ./public
COPY --from=build /app/node_modules ./node_modules

EXPOSE 3000

CMD ["npm", "run", "start"]
```

---

## 3. Mandatory Code Adjustments

Before deploying to production, you must fix a few hardcoded addresses in the codebase:

### 1. Frontend WebSocket connection
In [`frontend/src/hooks/useCollaboration.ts`](file:///d:/project-codeRoom/frontend/src/hooks/useCollaboration.ts#L68), the WebSocket port is hardcoded to `8080`:
```typescript
const wsUrl = `${protocol}//${window.location.hostname}:8080/ws`;
```
In a production deployment behind a reverse proxy (where port 80/443 is used for both Next.js and API routing), this should be changed to:
```typescript
const wsUrl = `${protocol}//${window.location.host}/ws`;
```
This ensures the client connects through Nginx, which will proxy `/ws` path requests to the Spring Boot backend.

### 2. Next.js rewrite rules
In production, Next.js rewrite rules in `next.config.js` (used for `/api` matching) are ignored if you build statically or direct requests via Nginx. 
It is recommended to have **Nginx** handle routing for both `/api` and `/ws` directly.

---

## 4. Production Orchestration

Create a [`docker-compose.prod.yml`](file:///d:/project-codeRoom/docker-compose.prod.yml) file in the project root directory:

```yaml
version: '3.8'

services:
  # Database
  postgres:
    image: postgres:16-alpine
    container_name: prod_postgres
    restart: always
    environment:
      POSTGRES_DB: ${DATABASE_NAME:-collab_editor}
      POSTGRES_USER: ${DATABASE_USERNAME:-postgres}
      POSTGRES_PASSWORD: ${DATABASE_PASSWORD}
    volumes:
      - postgres_prod_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $$POSTGRES_USER -d $$POSTGRES_DB"]
      interval: 5s
      timeout: 5s
      retries: 5

  # Kafka KRaft Broker
  kafka:
    image: apache/kafka:3.7.0
    container_name: prod_kafka
    restart: always
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      KAFKA_NUM_PARTITIONS: 1
    healthcheck:
      test: ["CMD-SHELL", "/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Spring Boot Backend REST + WS API
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: prod_backend
    restart: always
    depends_on:
      postgres:
        condition: service_healthy
      kafka:
        condition: service_healthy
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/${DATABASE_NAME:-collab_editor}
      DATABASE_USERNAME: ${DATABASE_USERNAME:-postgres}
      DATABASE_PASSWORD: ${DATABASE_PASSWORD}
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      JWT_SECRET: ${JWT_SECRET}
    expose:
      - "8080"

  # Spring Boot Execution Worker
  worker:
    build:
      context: ./worker
      dockerfile: Dockerfile
    container_name: prod_worker
    restart: always
    depends_on:
      kafka:
        condition: service_healthy
    environment:
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      BACKEND_URL: http://backend:8080
    volumes:
      # CRITICAL: Mount host docker socket to execute C++ sandbox
      - /var/run/docker.sock:/var/run/docker.sock
    expose:
      - "8081"

  # Next.js Frontend
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: prod_frontend
    restart: always
    expose:
      - "3000"

  # Nginx Gateway Proxy
  nginx:
    image: nginx:alpine
    container_name: prod_nginx
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./docker/nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./docker/nginx/certs:/etc/nginx/certs:ro
    depends_on:
      - frontend
      - backend

volumes:
  postgres_prod_data:
```

---

## 5. Nginx Configuration

Create [`docker/nginx/nginx.conf`](file:///d:/project-codeRoom/docker/nginx/nginx.conf) to manage proxying. This configuration correctly intercepts HTTP and WebSocket upgrade commands:

```nginx
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    # Dynamic mapping for WebSockets
    map $http_upgrade $connection_upgrade {
        default upgrade;
        ''      close;
    }

    server {
        listen 80;
        server_name yourdomain.com; # Replace with your domain

        # Redirect all HTTP traffic to HTTPS
        return 301 https://$host$request_uri;
    }

    server {
        listen 443 ssl;
        server_name yourdomain.com; # Replace with your domain

        # SSL Certificates (Volume-mounted)
        ssl_certificate /etc/nginx/certs/fullchain.pem;
        ssl_certificate_key /etc/nginx/certs/privkey.pem;
        
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers HIGH:!aNULL:!MD5;

        # Frontend Routing
        location / {
            proxy_pass http://frontend:3000;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Backend API Routing
        location /api {
            proxy_pass http://backend:8080/api;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Backend WebSockets routing
        location /ws {
            proxy_pass http://backend:8080/ws;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection $connection_upgrade;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            
            # Keep connections alive longer for active session sockets
            proxy_read_timeout 86400s;
            proxy_send_timeout 86400s;
        }
    }
}
```

---

## 6. Step-by-Step Deployment Instructions

### Step 1: Prepare the VM
1. Spin up an Ubuntu Server 22.04+ VPS (AWS EC2, DigitalOcean, Hetzner, etc.).
2. Connect to it via SSH.
3. Install Docker and Docker Compose:
   ```bash
   sudo apt-get update
   sudo apt-get install -y docker.io docker-compose-v2
   sudo usermod -aG docker $USER
   # Re-login or run "newgrp docker" to apply group permissions
   ```

### Step 2: Clone Codebase and Setup Environments
1. Clone the repository onto the server.
2. Navigate to the root directory and create a production `.env` file:
   ```bash
   cat <<EOF > .env
   DATABASE_NAME=collab_editor
   DATABASE_USERNAME=postgres
   DATABASE_PASSWORD=$(openssl rand -hex 16)
   JWT_SECRET=$(openssl rand -hex 32)
   EOF
   ```

### Step 3: Build the GCC 13 Runner Image
The execution worker looks for the `cpp-runner` tag on the host system to run C++ code. Build this image first on the host system:
```bash
docker build -t cpp-runner docker/cpp-runner
```

### Step 4: Setup SSL Certificates (Let's Encrypt / Certbot)
To obtain valid SSL certificates for free:
1. Temporarily spin up Nginx on port 80 or run Certbot standalone:
   ```bash
   sudo apt install certbot -y
   sudo certbot certonly --standalone -d yourdomain.com
   ```
2. Symlink/copy the certificate files into the project structure:
   ```bash
   mkdir -p docker/nginx/certs
   sudo cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem docker/nginx/certs/
   sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem docker/nginx/certs/
   sudo chown -R $USER:$USER docker/nginx/certs
   ```

### Step 5: Start the Container Stack
Build and run the entire suite using Docker Compose:
```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Verify everything is up and healthy:
```bash
docker compose -f docker-compose.prod.yml ps
```

---

## 7. Production Security and Operations Checklist

### Sandbox Containment Security
By default, the C++ execution is already configured to be secure. The worker invokes `cpp-runner` with:
- `-m 128m` (limiting compiler container memory output to 128MB to prevent RAM exhaustion).
- `--cpus 1.0` (capping CPU usage to prevent CPU starvation).
- `--network none` (preventing compiled C++ programs from executing network calls to curl external servers or trigger DDoS attacks).
- `timeout 5s` (killing run tasks that do not exit in 5 seconds).

### Docker Permissions
Mounting `/var/run/docker.sock` grants the worker container significant root-like privileges on the host. To secure this:
- Do not expose the worker service to the public web (only expose it via Nginx or keep it within the private docker network).
- Ensure the host user running Docker is properly secured.

### Log Management
Kafka and Docker logs can consume substantial disk space over time. Configure log rotation in `/etc/docker/daemon.json` on the host:
```json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
```
Restart docker service after applying: `sudo systemctl restart docker`.
