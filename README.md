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
| **MySQL Database** | `3307` | Relational Storage | DB: `match_catchup_ai` |
| **Redis Cache** | `6379` | In-Memory Cache (TTL: 6h) | Key: `match:IND-AUS-001:live` |
| **Ollama Local LLM** | `11434`| Model: `llama3.1:8b` | RAG Chatbot & Summary |

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

### Step 1: Start Discovery Service
```powershell
cd discovery-service
.\mvnw.cmd spring-boot:run
```
*(Eureka dashboard: http://localhost:8761)*

### Step 2: Start Match Score Generator
```powershell
cd match-score-generator
.\mvnw.cmd spring-boot:run
```
*(Runs on port 8081 and registers with Eureka)*

### Step 3: Start MatchCatchup AI Backend
```powershell
cd match-catchup-ai\backend
.\mvnw.cmd spring-boot:run
```
*(Runs on port 8082 with JWT authentication, Redis 6379, and 50s background collector)*

### Step 4: Start Frontend
```powershell
cd match-catchup-ai\frontend
npm install
npm run dev
```
*(Runs on http://localhost:5173 with proxy to backend port 8082)*
