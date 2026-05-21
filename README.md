# Virtual Classroom – Backend

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-green.svg)](https://spring.io/projects/spring-boot)

## Introduction

**Virtual Classroom** is an interactive teaching platform that connects teachers and students in real-time collaborative sessions. The idea is to bring the classroom onto the tablets live and directly such that students can immediately answer questions of the teacher. Each on their own but the teachers can share the different whiteboards with the whole class. Teachers create courses, start live sessions, and share whiteboards with their class. Students join via QR code, draw on personal whiteboards, chat with peers, and earn Brownie Points for participation. Uploaded session documents can be summarised instantly using AI.

This repository contains the **REST + WebSocket back-end**. The matching front-end lives in [sopra-fs26-group-04-client](https://github.com/sopra-fs26-group-04/sopra-fs26-group-04-client).

---

## Technologies

| Layer            | Technology                       |
|------------------|----------------------------------|
| Language         | Java 17                          |
| Framework        | Spring Boot 4.0                  |
| Persistence      | Spring Data JPA · H2 (in-memory) |
| Real-time        | Spring WebSocket                 |
| DTO mapping      | MapStruct                        |
| AI summary       | Google Gemini API                |
| QR codes         | ZXing (Google)                   |
| PDF parsing      | Apache PDFBox                    |
| Build            | Gradle 9                         |
| CI quality       | SonarQube · JaCoCo               |
| Containerisation | Docker                           |

---

## High-Level Components

### 1. REST Controllers
[`controller/`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller)

Four controllers expose the HTTP API:

| Controller | Responsibility                                                          |
|------------|-------------------------------------------------------------------------|
| [`UserController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/UserController.java) | Registration, login, logout, profile update                             |
| [`CourseController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/CourseController.java) | Course CRUD, QR-code enrollment, email invite                           |
| [`SessionController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/SessionController.java) | Session lifecycle, whiteboard state, file management system, AI summary |
| [`BrowniePointController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/BrowniePointController.java) | Award students with points, fetch leaderboard                           |

---

### 2. Service Layer
[`service/`](src/main/java/ch/uzh/ifi/hase/soprafs26/service)

Business logic and authorisation live here, isolated from HTTP concerns. Key services:

- [`UserService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/UserService.java) – token-based auth, password-strength enforcement
- [`SessionService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/SessionService.java) – session state machine, whiteboard coordination, file management
- [`GeminiSummaryService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/GeminiSummaryService.java) – sends extracted PDF text to Google Gemini and returns a structured summary

---

### 3. WebSocket Handlers
[`rest/`](src/main/java/ch/uzh/ifi/hase/soprafs26/rest)

Three handlers provide push-based real-time communication:

| Handler | Endpoint | Purpose |
|---------|----------|---------|
| [`ChatWebSocketHandler`](src/main/java/ch/uzh/ifi/hase/soprafs26/rest/ChatWebSocketHandler.java) | `/ws/chat/{sessionId}` | Persist and broadcast chat messages |
| [`WhiteboardWebSocketHandler`](src/main/java/ch/uzh/ifi/hase/soprafs26/rest/WhiteboardWebSocketHandler.java) | `/ws/whiteboard/{courseId}` | Broadcast drawing strokes to all participants |
| [`SessionWebSocketHandler`](src/main/java/ch/uzh/ifi/hase/soprafs26/rest/SessionWebSocketHandler.java) | `/ws/session/{sessionId}` | Broadcast session-mode events (collaboration start/end) |

---

### 4. Domain Model
[`entity/`](src/main/java/ch/uzh/ifi/hase/soprafs26/entity)

Core JPA entities and their relationships:

```
User ──< CourseEnrollment >── Course ──< Session
                                           │
                                   TeacherWhiteboard
                                   PersonalWhiteboard ──< WhiteboardPage
                                   ChatMessage
                                   SessionFile
                                   BrowniePointEntry
```

---

## Launch & Deployment

### Prerequisites
- **Java 17** (JDK)
- **Git**
- **Gemini API Key** (optional – only needed for the AI summary feature)

### 1. Clone & configure
```bash
git clone https://github.com/sopra-fs26-group-04/sopra-fs26-group-04-server.git
cd sopra-fs26-group-04-server
```

Create `local.properties` in the project root (gitignored):
```properties
GEMINI_API_KEY=your-key-here
GEMINI_API_MODEL=gemini-1.5-flash   # optional, this is the default
```

### 2. Build
```bash
./gradlew build
```

### 3. Run locally
```bash
./gradlew bootRun
```

The API is now available at `http://localhost:8080`.  
H2 console (database browser): `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa` · Password: *(empty)*

### 4. Run tests
```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests "ch.uzh.ifi.hase.soprafs26.service.UserServiceTest"
```

Coverage report: `build/reports/jacoco/test/html/index.html`

### 5. Development mode
Run both commands in separate terminals for hot-reload on file changes:
```bash
./gradlew build --continuous -xtest
./gradlew bootRun
```

### 6. Release / deployment
```bash
# Build a runnable JAR
./gradlew bootJar

# The JAR is located at:
build/libs/sopra-fs26-group-04-server-*.jar

# Run it
java -jar build/libs/sopra-fs26-group-04-server-*.jar \
  --GEMINI_API_KEY=your-key
```

For production deployments (e.g. Google Cloud Run / Docker), set the environment
variables `GEMINI_API_KEY` and `GEMINI_API_MODEL` in your deployment configuration.

#### Docker
```bash
# Pull the latest image from Docker Hub
docker pull <dockerhub_username>/sopra-fs26-group-04-server

# Run the container
docker run -p 8080:8080 \
  -e GEMINI_API_KEY=your-key \
  <dockerhub_username>/sopra-fs26-group-04-server
```



## Roadmap

Contributions are welcome! Here are the top features we suggest tackling next:

1. **Persistent database**  
   Replace the in-memory H2 database with a PostgreSQL instance so that courses, sessions, and whiteboard snapshots survive server restarts. Add a `docker-compose.yml` so developers can spin up the database with one command.

2. **Password hashing**  
   Passwords are currently stored as plain text (flagged in the code as "for testing only"). Integrate BCrypt (available via Spring Security) to hash passwords at registration and verify them at login.

3. **Session recording & playback**  
   Persist every whiteboard stroke event with a timestamp so a session can be replayed step-by-step after it ends – useful for students who missed the live session.

---

## Authors & Acknowledgment

Group 04 – SoPra FS26, University of Zurich:

## Authors and Acknowledgment

| Name                | GitHub                                                 | Matrikelnumber |
|---------------------|--------------------------------------------------------|----------------|
| Antonio Afram       | [@AQuant1](https://github.com/AQuant1)                 | 23-729-775     |
| Michelle Brauch     | [@Meimira](https://github.com/Meimira)                 | 24-748-618     |
| Moritz Davinghausen | [@MoritzDav](https://github.com/MoritzDav)             | 24-722-795     |
| Valya Sorokivska    | [@ValyaSorokivska](https://github.com/ValyaSorokivska) | 24-743-247     |
| Lars Pataky         | [@bablandan](https://github.com/bablandan)             | 19-923-697     |

Supervised by the SoPra teaching team at the Department of Informatics, UZH.  
Built on the [SoPra Server Template](https://github.com/HASEL-UZH/sopra-fs25-template-server).

---

## License

This project is licensed under the [MIT License](LICENSE).