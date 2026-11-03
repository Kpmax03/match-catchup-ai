# MatchCatchup AI — Full-Stack Microservices Platform

**Parent Project:** Dream11  
**Application:** MatchCatchup AI  
**Purpose:** AI-powered cricket match catch-up platform  
**Architecture:** Full-stack, microservice-based application registered with Eureka  
**Interface Design:** JioHotstar-inspired live cricket broadcast & commentary experience

---

## 1. Project Directory Structure

```text
dream11/
│
├── discovery-service/          # Spring Boot Eureka Discovery Server (Port 8761)
│   └── pom.xml
│
├── match-score-generator/      # Existing Ball-by-Ball Live Score Provider (Port 8081)
│   └── pom.xml
│
└── match-catchup-ai/
    ├── backend/                # MatchCatchup AI Spring Boot Core (Port 8082)
    │   ├── src/main/java/com/matchcatchup/
    │   │   ├── client/         # OpenFeign client to match-score-generator
    │   │   ├── config/         # SecurityConfig, JwtService, RedisConfig, CorsConfig
    │   │   ├── controller/     # AuthController, MatchController, ActivityController, CatchupController, ChatController
    │   │   ├── entity/         # AppUser, MatchEntity, CollectedDelivery, MatchCollectionState, UserMatchActivity, CatchupSession
    │   │   ├── repository/     # Spring Data JPA Repositories
    │   │   ├── scheduler/      # 50s Match Collector & 15s Inactivity Detector
    │   │   └── service/        # AuthService, CollectionService, MatchService, ActivityService, CatchupService, SummaryService, RagChatService
    │   └── pom.xml
    │
    └── frontend/               # React + Vite + TypeScript (Port 5173)
        ├── src/
        │   ├── api/            # Authenticated API client (JWT in Authorization headers)
        │   ├── components/     # BroadcastScorecard, LiveCommentaryFeed, FloatingChatbot, AiSummaryCard, TimelineSection
        │   └── pages/          # AuthPage (Login/Signup), HomePage (Fixtures), LiveMatchPage, CatchupPage
        └── package.json
```

---

## 2. Ports and Service Discovery

| Service | Port | Technology | Registration Name |
|---|---|---|---|
| **Eureka Discovery Server** | `8761` | Spring Cloud Netflix Eureka | `discovery-service` |
| **Match Score Generator** | `8081` | Spring Boot 4.1.2 + MySQL | `match-score-generator` |
| **MatchCatchup AI Backend** | `8082` | Spring Boot 4.1.2 + Spring Security + OpenFeign | `match-catchup-ai` |
| **MatchCatchup AI Frontend** | `5173` | React 19 + Vite + TypeScript | Client SPA |
| **MySQL Database** | `3306` (container network) | Relational Storage | DBs: `score_generator_service`, `match_catchup_ai` |
| **Redis Cache** | `6379` (container network) | In-Memory Cache (TTL: 6h) | Key: `match:IND-AUS-001:live` |
| **pgvector** | `5432` (container network) | Vector Storage | DB: `match_catchup_vector` |
| **Ollama Local LLM** | `11434` (container network) | Models: `llama3.1:8b`, `nomic-embed-text` | RAG Chatbot & Summary |

With Docker Compose, only the frontend (`5173`) and backend (`8082`) are published to the host. Other services communicate over the private Compose network.

---

## 3. Key Features & Workflows

1. **Authentication & User Profiles:**
   - Full signup and login with BCrypt password hashing and JWT token issuance.
   - User profile tracks `lastActiveAt`.
   - Per-user, per-match viewing progress is tracked separately in MySQL. One user's viewed delivery cursor never affects another user's catch-up state.

2. **JioHotstar-Inspired Live Match Experience:**
   - **Broadcast Scorecard:** TV lower-third style displaying batting team vs bowling team, total score, wickets, overs, current run rate (CRR), and target.
   - **On Crease Strip:** Striker with asterisk (`*`), non-striker, runs scored, balls faced, current bowler figures (wickets/runs), and horizontal "This Over" ball strip (`• 1 4 2 W 6`).
   - **Live Commentary Feed:** Separate dedicated live ball-by-ball commentary stream updating in real-time with event descriptions and boundary/wicket tags.
   - **Floating Bottom-Right Chatbot:** Collapsible AI cricket bot icon in the bottom-right corner.

3. **Automatic Inactivity Detection (No Simulator Buttons):**
   - Frontend automatically sends heartbeats every 15 seconds while viewing the match.
   - If no heartbeat is received for 45 seconds (e.g. user leaves page or closes tab), backend flags user `INACTIVE`.
   - Background collector continues fetching match deliveries independently every 10 seconds (temporarily adjusted for testing).

4. **Automatic Return & Catch-Up Trigger:**
   - When the user logs in again and returns to the match:
     - Immediate heartbeat fires on mount.
     - Backend detects inactivity, compares `lastViewedDeliveryId` with `lastCollectedDeliveryId`, and creates a `CatchupSession`.
     - A high-visibility **Match Catch-Up** button and alert banner appear on the live match page showing the number of missed deliveries and overs range.
     - Clicking **Match Catch-Up** opens the missed delivery timeline.

5. **Match Catch-Up Page:**
   - Score progression through the missed interval.
   - Factual AI match summary with turning points and key boundaries.
   - Chronological delivery timeline grouped by over and innings.
   - Session delivery boundaries are preserved so newer deliveries do not shift the current catch-up view.

6. **RAG Chatbot Restricted to Missed Deliveries:**
   - Floating chat panel slides out from the bottom-right corner.
   - Grounded strictly to the current user's missed deliveries.
   - Arithmetic questions (runs, wickets, boundaries) are computed deterministically from the database to eliminate hallucinations.
   - Cites over and ball numbers in answers.

---

## 4. How to Run the Platform

### Run the published Docker images

Requirements: Docker Desktop with its Linux container engine running and internet access on the first start. The published app images are `kpmax03/dream11-discovery-service`, `kpmax03/dream11-match-score-generator`, `kpmax03/dream11-match-catchup-ai`, and `kpmax03/dream11-frontend`.

From the project root, create a private environment file and set your own secrets:

```powershell
Copy-Item .env.example .env
notepad .env
```

Start the application:

```powershell
docker compose up -d
```

Compose downloads the published app images and official dependency images. It also downloads the required Ollama models on first startup. Open the frontend at http://localhost:5173. Allow several minutes for the first startup and model download.

Check service status and logs:

```powershell
docker compose ps
docker compose logs -f match-catchup-ai
```

Stop the services while preserving database and model data:

```powershell
docker compose down
```

Do not use `docker compose down -v` unless you intend to delete the persistent database and model volumes. Keep `.env` private; it is ignored by Git.

### Build and publish updated app images

After logging in with `docker login`, build from the source and push only the four app services:

```powershell
docker compose -f compose.yaml -f compose.build.yaml build
docker compose push discovery-service match-score-generator match-catchup-ai frontend
```
