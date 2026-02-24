# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

WhatsApp Trade Intelligence Platform — monitors WhatsApp groups for industrial surplus trade messages, extracts structured listings via LLM, and provides search, chat AI, and notification features.

The full system architecture and requirements are in `prd.md`.

## Tech Stack

- **Backend:** Java 21 + Spring Boot 3.3 (REST API + WebSocket via STOMP)
- **Build:** Maven (use `mvn` / `./mvnw`, not Gradle)
- **Logging:** Log4j2 — always use Log4j2 for all logging. Exclude Spring Boot's default Logback and use `spring-boot-starter-log4j2` instead.
- **Frontend:** React 18 + Vite + TailwindCSS (SPA with React Router)
- **Database:** PostgreSQL 16 — used for **both** RDBMS and semantic search (pgvector for vector similarity, pg_trgm for fuzzy text). No separate vector DB.
- **LLM:** OpenAI API — GPT-4o for chat, GPT-4o-mini for extraction, text-embedding-3-small for embeddings
- **Auth:** Spring Security OAuth2 + Google SSO, JWT session tokens
- **WhatsApp:** Whapi.cloud webhooks for multi-group monitoring
- **Schema migrations:** Flyway (JPA ddl-auto is `validate` only)
- **Caching:** Caffeine (in-process) — no Redis
- **Async:** Spring `@Async` + `ApplicationEventPublisher` — no Redis pub/sub

## Build & Run Commands

### Backend (Spring Boot)
```bash
./mvnw spring-boot:run                    # Run dev server
./mvnw test                               # Run all tests
./mvnw test -Dtest=ClassName              # Run single test class
./mvnw test -Dtest=ClassName#methodName   # Run single test method
./mvnw package -DskipTests                # Build JAR without tests
./mvnw flyway:migrate                     # Run DB migrations manually
```

### Frontend (React/Vite)
```bash
npm install                # Install dependencies
npm run dev                # Dev server (default port 5173)
npm run build              # Production build
npm run preview            # Preview production build
npm run lint               # Lint check
```

## Architecture

### Backend Package Structure
Base package: `com.tradeintel`

| Package | Responsibility |
|---------|---------------|
| `config/` | SecurityConfig, OpenAIConfig, AsyncConfig, CacheConfig, WebSocketConfig |
| `auth/` | Google OAuth2 flow, JWT provider/filter, UserPrincipal |
| `webhook/` | Whapi webhook receiver (POST /api/webhooks/whapi), signature validation |
| `archive/` | Raw message storage, media download (S3/local), embedding generation |
| `processing/` | Async pipeline: jargon expansion → LLM extraction → confidence routing → notification matching |
| `listing/` | CRUD + search endpoints for extracted listings (semantic + direct filters) |
| `chat/` | AI chat with OpenAI function-calling agent; tools in `chat/tools/` subpackage |
| `replay/` | WhatsApp message replay API (paginated, searchable) |
| `normalize/` | Admin CRUD for categories, manufacturers, units, conditions |
| `notification/` | NL rule parsing (via OpenAI), rule matching, email dispatch |
| `review/` | Human review queue for low-confidence extractions |
| `admin/` | Uber admin: user management, audit log, cost reports |
| `common/` | JPA entities, global exception handler, role-based security annotations, OpenAI client wrapper |

### Message Processing Pipeline (async)
Webhook → archive raw message → `NewMessageEvent` →
1. Generate embedding (text-embedding-3-small)
2. Expand jargon (from admin-managed dictionary)
3. LLM extraction (GPT-4o-mini: intent + structured fields)
4. Confidence routing: ≥0.8 auto-accept, 0.5-0.8 human review, <0.5 discard
5. Notification matching for active listings
6. Jargon auto-learning (unknown terms queued for admin verification)

### Frontend Structure
- `api/` — Axios client with JWT interceptor, one module per API domain
- `components/replay/` — WhatsApp-style message viewer (GroupList, MessageThread, MessageBubble)
- `components/chat/` — AI query interface with tool result rendering
- `components/listings/` — Browse/filter/search extracted listings
- `components/admin/normalize/` — CRUD UIs for each normalized value table
- `hooks/` — useAuth, useInfiniteScroll, useDebounce, useWebSocket
- `contexts/` — AuthContext, WebSocketContext
- Uses React Query (`useQuery`/`useMutation`) for server state

### Three-Tier Role System
- **user** — replay, chat, listings, notifications, personal cost view
- **admin** — above + review queue, normalized value CRUD, jargon management
- **uber_admin** — above + user management, all chats view, cost reports, audit log, WhatsApp group management

### Security Boundaries
- `/api/webhooks/**` — public (signature-validated)
- `/api/auth/**` — public
- `/api/chat/**`, `/api/listings/**`, `/api/notifications/**`, `/api/messages/**` — authenticated
- `/api/review/**`, `/api/normalize/**`, `/api/jargon/**` — admin+
- `/api/admin/**` — uber_admin only
- CSRF disabled for webhook endpoints only

## Testing Requirements

Always write end-to-end tests for every feature and run them to verify they pass before considering work complete.

- **Backend:** Use Spring Boot's `@SpringBootTest` with `WebTestClient` or `MockMvc` for E2E API tests. Run with `./mvnw test`.
- **Frontend:** Use Playwright or Cypress for E2E browser tests. Run with `npx playwright test` or `npx cypress run`.

## Key Design Decisions

- **No Redis:** Caffeine cache + Spring ApplicationEventPublisher handles all caching and async needs for the small user base.
- **Flyway owns the schema:** JPA is validate-only. All DDL goes in `resources/db/migration/` as versioned SQL files (V001__, V002__, etc.).
- **LLM prompts are file-based:** Stored in `resources/prompts/` (extraction.txt, chat_system.txt, rule_parser.txt).
- **Extraction normalizes against admin-managed values:** The LLM extraction prompt includes known categories, manufacturers, and jargon so it can match to existing normalized values.
- **Dual search modes:** Both text (pg_trgm fuzzy) and semantic (pgvector cosine similarity) search are supported on messages and listings.
- **Idempotent webhook processing:** `whapi_msg_id` unique constraint prevents duplicate message archival.
- **Soft deletes on listings:** Uses `deleted_at`/`deleted_by` columns, not hard delete.
- **Maven only:** Always use Maven for builds. No Gradle.
- **Log4j2 only:** Exclude `spring-boot-starter-logging` (Logback) from all starters and depend on `spring-boot-starter-log4j2`. Use `org.apache.logging.log4j.LogManager` / `Logger` for all logging — never SLF4J Logback or `java.util.logging`.
- **PostgreSQL is the single data store:** All relational data, vector embeddings (pgvector), and full-text/fuzzy search (pg_trgm) live in PostgreSQL. Do not introduce a separate vector database.

## Environment Variables

```
DB_USER, DB_PASSWORD
GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET
OPENAI_API_KEY
WHAPI_API_KEY, WHAPI_WEBHOOK_SECRET
JWT_SECRET
```

## Implementation Phases

The PRD defines 4 phases — check `prd.md` section 8 for current status:
1. Foundation (scaffold, auth, webhook, replay UI)
2. Admin & Normalized Data (CRUD, jargon, user management)
3. Intelligence (extraction pipeline, embeddings, search, review queue)
4. User Features & Polish (chat AI, notifications, cost tracking)
