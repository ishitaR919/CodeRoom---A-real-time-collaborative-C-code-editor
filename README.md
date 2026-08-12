# Minimal Real-Time Collaborative C++ Code Editor

A full-stack, real-time collaborative C++ code editor designed to allow developers to register, create coding rooms, edit code live with peers using Yjs CRDT over WebSockets, and safely execute C++ code inside isolated Docker containers via an asynchronous Apache Kafka task queue.

---

## Architecture Diagram

```text
                    ┌─────────────────┐
                    │    Next.js      │
                    │     Frontend    │
                    │ Monaco + Yjs    │
                    └────────┬────────┘
                             │
                      REST / WebSocket
                             │
                    ┌────────▼────────┐
                    │   Spring Boot   │
                    │     Backend     │
                    │                 │
                    │ Auth            │
                    │ Rooms           │
                    │ REST APIs       │
                    │ WebSocket       │
                    │ Kafka Producer  │
                    └──────┬─────┬────┘
                           │     │
                    PostgreSQL  Kafka
                           │     │
                           │     ▼
                           │  ┌──────────────┐
                           │  │ Spring Boot  │
                           │  │ Execution    │
                           │  │ Worker       │
                           │  └──────┬───────┘
                           │         │
                           │       Docker
                           │         │
                           │       C++
                           │         │
                           │       Output
                           │         │
                           └─────────┘
```

---

## Features

- **Authentication:** Registration, login, logout with BCrypt password hashing & JWT token security.
- **Room System:** Instant 6-character room code creation (`ABC123`), sharing, and room joining.
- **Real-Time Collaboration:** Conflict-free real-time C++ editing powered by Monaco Editor & Yjs CRDT over WebSocket connections.
- **Online Presence:** Real-time list of online users connected to the room.
- **Asynchronous Execution:** Code execution requests are pushed to Apache Kafka and processed asynchronously by a dedicated Spring Boot worker.
- **Docker Sandbox:** C++ compilation (`g++`) and execution isolated in short-lived, temporary Docker containers with memory (128MB), CPU, network isolation, and process timeout protection.
- **Live Output Streaming:** Execution results (stdout, stderr, compilation errors, runtime errors, timeouts) delivered directly back to room participants via WebSocket.
- **Persistence:** Automatic & manual code persistence in PostgreSQL powered by Spring Data JPA and Flyway schema migrations.

---

## Technology Stack

### Frontend
- **Framework:** Next.js 14 (App Router)
- **Language:** TypeScript
- **Styling:** Tailwind CSS (Dark theme UI)
- **Code Editor:** Monaco Editor (`@monaco-editor/react`)
- **CRDT Collaboration:** Yjs & `y-monaco`
- **Real-time Transport:** WebSocket client

### Backend
- **Framework:** Java 21 & Spring Boot 3.2
- **Security:** Spring Security & JWT
- **Persistence:** Spring Data JPA, Hibernate, PostgreSQL 16
- **Database Migrations:** Flyway
- **Real-time Messaging:** Spring WebSocket
- **Queueing:** Apache Kafka (KRaft mode)

### Execution Worker & Sandbox
- **Worker:** Spring Boot micro-worker app with `@KafkaListener`
- **Sandbox:** Docker (`cpp-runner` image with GCC 13 compiler)

---

## Monorepo Directory Structure

```text
collaborative-code-editor/
├── frontend/                     # Next.js App Router Frontend
│   ├── src/
│   │   ├── app/
│   │   │   ├── login/
│   │   │   ├── register/
│   │   │   ├── dashboard/
│   │   │   └── room/[roomId]/
│   │   ├── components/
│   │   │   ├── Navbar.tsx
│   │   │   ├── MonacoEditorComponent.tsx
│   │   │   ├── PresenceList.tsx
│   │   │   └── OutputPanel.tsx
│   │   ├── hooks/
│   │   │   ├── useAuth.ts
│   │   │   └── useCollaboration.ts
│   │   ├── lib/
│   │   │   └── api.ts
│   │   └── types/
│   └── package.json
│
├── backend/                      # Spring Boot Backend
│   ├── src/main/java/com/example/collabeditor/
│   │   ├── auth/
│   │   ├── user/
│   │   ├── room/
│   │   ├── file/
│   │   ├── websocket/
│   │   ├── kafka/
│   │   └── execution/
│   ├── src/main/resources/
│   │   └── db/migration/
│   └── pom.xml
│
├── worker/                       # Spring Boot Execution Worker
│   ├── src/main/java/com/example/executionworker/
│   │   ├── kafka/
│   │   ├── runner/
│   │   └── model/
│   └── pom.xml
│
├── docker/
│   └── cpp-runner/               # GCC 13 runner image
│       └── Dockerfile
│
├── docker-compose.yml            # PostgreSQL 16 & Kafka (KRaft mode)
├── .env.example
└── README.md
```

---

## Architectural Rationale

### Why Yjs & WebSocket for Collaboration?
Yjs is a high-performance Conflict-free Replicated Data Type (CRDT) library that resolves concurrent editing conflicts deterministically without requiring a central authority to recalculate document offsets. The browser clients hold document state, while Spring Boot WebSocket server multiplexes binary/JSON update vectors and presence frames.

### Why Kafka for Code Execution?
C++ compilation and execution is compute-intensive and untrusted. Offloading execution to Apache Kafka prevents web server HTTP thread starvation, isolates slow or malicious code execution into scalable worker queues, and ensures non-blocking browser user experiences.

---

## Local Setup & Development Commands

### 1. Prerequisites
- **Java 21**
- **Node.js v20+**
- **Docker Desktop** (with Docker CLI available)

### 2. Start Infrastructure (PostgreSQL & Kafka)

```bash
docker compose up -d
```

Build the C++ sandbox Docker image:

```bash
docker build -t cpp-runner docker/cpp-runner
```

### 3. Run Backend (Port 8080)

```bash
cd backend
./mvnw spring-boot:run
```

### 4. Run Execution Worker (Port 8081)

```bash
cd worker
./mvnw spring-boot:run
```

### 5. Run Frontend (Port 3000)

```bash
cd frontend
npm install
npm run dev
```

Visit [http://localhost:3000](http://localhost:3000) in your browser.

---

## Manual Verification Steps

### Test 1: Real-Time Collaboration
1. Open Browser Window 1 -> Register User A -> Create Room -> Note 6-char Room Code (e.g. `ABC123`).
2. Open Browser Window 2 (Incognito) -> Register User B -> Join Room with code `ABC123`.
3. Type in Window 1; verify edits appear instantly in Window 2 without page refreshes.
4. Verify both User A and User B appear in the **Online Users** sidebar list.

### Test 2: Safe C++ Execution
1. In any active room, write C++ code:
   ```cpp
   #include <iostream>

   int main() {
       std::cout << "Hello from Docker Sandbox!";
       return 0;
   }
   ```
2. Click **[ RUN ]**.
3. Observe status changing to `Running...` and receiving output `"Hello from Docker Sandbox!"` with execution duration.
4. Intentionally introduce a syntax error (e.g. remove semicolon) and click **RUN** to verify compiler error reporting.

### Test 3: Code Persistence
1. Make changes to code in a room.
2. Refresh the browser page or close and reopen the room link.
3. Verify that saved C++ code is restored from PostgreSQL.

---

## Environment Variables (.env)

```env
DATABASE_URL=jdbc:postgresql://localhost:5432/collab_editor
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
FRONTEND_URL=http://localhost:3000
WEBSOCKET_URL=ws://localhost:8080/ws
```

---

