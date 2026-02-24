# WhatsApp Trade Intelligence Platform — System Architecture v2

## Tech Stack

| Layer | Technology | Notes |
|-------|-----------|-------|
| **Frontend** | React 18 + Vite + TailwindCSS | SPA with React Router |
| **Backend** | Java 21 + Spring Boot 3.3 | REST API + WebSocket |
| **Database** | PostgreSQL 16 + pgvector | Relational + vector search |
| **LLM** | OpenAI API (GPT-4o / GPT-4o-mini) | Extraction + chat |
| **Auth** | Spring Security OAuth2 + Google SSO | JWT session tokens |
| **WhatsApp** | Whapi.cloud webhooks | Multi-group monitoring |
| **Email** | Spring Mail + SendGrid | Notifications |
| **Search** | pgvector + pg_trgm | Semantic + full-text |
| **File Storage** | S3 / local filesystem | WhatsApp media |
| **Hosting** | Single VPS or small cluster | Low user count |

**No Redis** — For a small user base, Spring's in-process `@Async` + `ApplicationEventPublisher` replaces pub/sub, and a simple Caffeine cache replaces Redis caching.

---

## 1. System Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           REACT SPA (Vite + Tailwind)                        │
│                                                                              │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌───────────┐ ┌────────────┐ │
│  │  WhatsApp  │ │  Chat AI   │ │  Listings  │ │  Notify   │ │  Admin     │ │
│  │  Replay UI │ │  Interface │ │  Browser   │ │  Manager  │ │  Dashboard │ │
│  └────────────┘ └────────────┘ └────────────┘ └───────────┘ └────────────┘ │
└──────────────────────────────┬───────────────────────────────────────────────┘
                               │ REST + WebSocket
┌──────────────────────────────┼───────────────────────────────────────────────┐
│                    SPRING BOOT APPLICATION                                    │
│                                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │  Auth        │  │  Whapi       │  │  Processing  │  │  Chat/Query  │    │
│  │  (Google SSO)│  │  Webhook     │  │  Pipeline    │  │  Service     │    │
│  └──────────────┘  │  Receiver    │  │  (async)     │  │  (OpenAI)    │    │
│                    └──────────────┘  └──────────────┘  └──────────────┘    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │  Listing     │  │  Jargon      │  │  Notify      │  │  Admin       │    │
│  │  Service     │  │  Service     │  │  Engine      │  │  Service     │    │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘    │
│                                                                              │
└──────────────────────────────┬───────────────────────────────────────────────┘
                               │
                    ┌──────────┴──────────┐
                    │   PostgreSQL 16     │
                    │   + pgvector        │
                    │   + pg_trgm         │
                    └─────────────────────┘
```

---

## 2. Database Schema

```sql
-- =====================================================================
-- EXTENSIONS
-- =====================================================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "vector";       -- pgvector for semantic search
CREATE EXTENSION IF NOT EXISTS "pg_trgm";      -- trigram for fuzzy text search

-- =====================================================================
-- WHATSAPP GROUPS
-- =====================================================================
CREATE TABLE whatsapp_groups (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    whapi_group_id  TEXT UNIQUE NOT NULL,
    group_name      TEXT NOT NULL,
    description     TEXT,
    avatar_url      TEXT,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

-- =====================================================================
-- RAW MESSAGES (the full archive — WhatsApp replay source)
-- =====================================================================
CREATE TABLE raw_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id        UUID NOT NULL REFERENCES whatsapp_groups(id),
    whapi_msg_id    TEXT UNIQUE NOT NULL,
    sender_phone    TEXT,
    sender_name     TEXT,
    sender_avatar   TEXT,
    message_body    TEXT,
    message_type    TEXT DEFAULT 'text',         -- text, image, document, video, audio
    media_url       TEXT,
    media_mime_type TEXT,
    media_local_path TEXT,                       -- local/S3 copy
    reply_to_msg_id TEXT,                        -- whapi ID of quoted message
    is_forwarded    BOOLEAN DEFAULT FALSE,
    timestamp_wa    TIMESTAMPTZ NOT NULL,
    received_at     TIMESTAMPTZ DEFAULT now(),
    -- Processing state
    processed       BOOLEAN DEFAULT FALSE,
    processing_error TEXT,
    -- Semantic search vector (computed after archival)
    embedding       vector(1536),                -- text-embedding-3-small output
    
    CONSTRAINT raw_messages_group_fk FOREIGN KEY (group_id) REFERENCES whatsapp_groups(id)
);

CREATE INDEX idx_raw_msg_group_time ON raw_messages(group_id, timestamp_wa DESC);
CREATE INDEX idx_raw_msg_unprocessed ON raw_messages(processed) WHERE processed = FALSE;
CREATE INDEX idx_raw_msg_sender ON raw_messages(sender_name, group_id);
CREATE INDEX idx_raw_msg_body_trgm ON raw_messages USING gin(message_body gin_trgm_ops);
CREATE INDEX idx_raw_msg_embedding ON raw_messages USING ivfflat(embedding vector_cosine_ops)
    WITH (lists = 100);

-- =====================================================================
-- EXTRACTED LISTINGS
-- =====================================================================
CREATE TYPE intent_type AS ENUM ('sell', 'want', 'unknown');
CREATE TYPE listing_status AS ENUM ('active', 'expired', 'deleted', 'pending_review');

CREATE TABLE listings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    raw_message_id      UUID NOT NULL REFERENCES raw_messages(id),
    group_id            UUID NOT NULL REFERENCES whatsapp_groups(id),
    
    -- Classification
    intent              intent_type NOT NULL,
    confidence_score    FLOAT NOT NULL DEFAULT 0.0,
    
    -- Normalized fields (all searchable)
    item_description    TEXT NOT NULL,
    item_category_id    UUID REFERENCES categories(id),
    manufacturer_id     UUID REFERENCES manufacturers(id),
    part_number         TEXT,
    quantity            NUMERIC,
    unit_id             UUID REFERENCES units(id),
    price               NUMERIC,
    price_currency      TEXT DEFAULT 'USD',
    condition_id        UUID REFERENCES conditions(id),
    
    -- Provenance
    original_text       TEXT NOT NULL,
    sender_name         TEXT,
    sender_phone        TEXT,
    
    -- Review state
    status              listing_status DEFAULT 'active',
    needs_human_review  BOOLEAN DEFAULT FALSE,
    reviewed_by         UUID REFERENCES users(id),
    reviewed_at         TIMESTAMPTZ,
    
    -- Semantic
    embedding           vector(1536),
    
    -- Lifecycle
    created_at          TIMESTAMPTZ DEFAULT now(),
    updated_at          TIMESTAMPTZ DEFAULT now(),
    expires_at          TIMESTAMPTZ,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID REFERENCES users(id)
);

CREATE INDEX idx_listing_intent_status ON listings(intent, status);
CREATE INDEX idx_listing_category ON listings(item_category_id);
CREATE INDEX idx_listing_manufacturer ON listings(manufacturer_id);
CREATE INDEX idx_listing_part_number ON listings(part_number) WHERE part_number IS NOT NULL;
CREATE INDEX idx_listing_desc_trgm ON listings USING gin(item_description gin_trgm_ops);
CREATE INDEX idx_listing_embedding ON listings USING ivfflat(embedding vector_cosine_ops)
    WITH (lists = 100);
CREATE INDEX idx_listing_review ON listings(needs_human_review) WHERE needs_human_review = TRUE;

-- =====================================================================
-- NORMALIZED VALUE TABLES (Admin-managed CRUD)
-- =====================================================================

CREATE TABLE categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT UNIQUE NOT NULL,           -- "Pipe Fittings"
    parent_id   UUID REFERENCES categories(id), -- hierarchical
    sort_order  INTEGER DEFAULT 0,
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT now(),
    updated_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE manufacturers (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT UNIQUE NOT NULL,           -- "Parker Hannifin"
    aliases     TEXT[],                         -- ["Parker", "PH"]
    website     TEXT,
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT now(),
    updated_at  TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_manufacturers_aliases ON manufacturers USING gin(aliases);

CREATE TABLE units (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT UNIQUE NOT NULL,           -- "each"
    abbreviation TEXT UNIQUE NOT NULL,          -- "ea"
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE conditions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT UNIQUE NOT NULL,           -- "New Old Stock"
    abbreviation TEXT,                          -- "NOS"
    sort_order  INTEGER DEFAULT 0,
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT now()
);

-- =====================================================================
-- JARGON DICTIONARY (Self-improving)
-- =====================================================================

CREATE TABLE jargon_dictionary (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    acronym         TEXT NOT NULL,
    expansion       TEXT NOT NULL,
    industry        TEXT,
    context_example TEXT,
    source          TEXT DEFAULT 'llm',         -- llm, human, seed
    confidence      FLOAT DEFAULT 0.5,
    usage_count     INTEGER DEFAULT 1,
    verified        BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now(),
    UNIQUE(acronym, expansion)
);
CREATE INDEX idx_jargon_acronym ON jargon_dictionary(acronym);

-- =====================================================================
-- USERS & AUTH
-- =====================================================================
CREATE TYPE user_role AS ENUM ('user', 'admin', 'uber_admin');

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    google_id       TEXT UNIQUE NOT NULL,
    email           TEXT UNIQUE NOT NULL,
    display_name    TEXT,
    avatar_url      TEXT,
    role            user_role DEFAULT 'user',
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT now(),
    last_login_at   TIMESTAMPTZ
);

-- =====================================================================
-- CHAT SYSTEM (AI queries about data)
-- =====================================================================

CREATE TABLE chat_sessions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    title       TEXT,
    created_at  TIMESTAMPTZ DEFAULT now(),
    updated_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE chat_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID NOT NULL REFERENCES chat_sessions(id),
    role            TEXT NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content         TEXT NOT NULL,
    model_used      TEXT,
    input_tokens    INTEGER DEFAULT 0,
    output_tokens   INTEGER DEFAULT 0,
    cost_usd        NUMERIC(10,6) DEFAULT 0,
    tool_calls      JSONB,
    created_at      TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_chat_msg_session ON chat_messages(session_id, created_at);

-- =====================================================================
-- USAGE / COST TRACKING
-- =====================================================================

CREATE TABLE usage_ledger (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id),
    period_date         DATE NOT NULL,
    total_input_tokens  BIGINT DEFAULT 0,
    total_output_tokens BIGINT DEFAULT 0,
    total_cost_usd      NUMERIC(12,6) DEFAULT 0,
    session_count       INTEGER DEFAULT 0,
    UNIQUE(user_id, period_date)
);

-- =====================================================================
-- NOTIFICATION RULES (Natural language)
-- =====================================================================

CREATE TABLE notification_rules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    nl_rule         TEXT NOT NULL,
    parsed_intent   intent_type,
    parsed_keywords TEXT[],
    parsed_category_ids UUID[],
    parsed_price_min NUMERIC,
    parsed_price_max NUMERIC,
    notify_channel  TEXT DEFAULT 'email',
    notify_email    TEXT,
    is_active       BOOLEAN DEFAULT TRUE,
    last_triggered  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

-- =====================================================================
-- HUMAN REVIEW QUEUE
-- =====================================================================

CREATE TABLE review_queue (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id      UUID REFERENCES listings(id),
    raw_message_id  UUID NOT NULL REFERENCES raw_messages(id),
    reason          TEXT NOT NULL,
    llm_explanation TEXT,
    suggested_values JSONB,
    status          TEXT DEFAULT 'pending' CHECK (status IN ('pending','resolved','skipped')),
    resolved_by     UUID REFERENCES users(id),
    resolution      JSONB,
    created_at      TIMESTAMPTZ DEFAULT now(),
    resolved_at     TIMESTAMPTZ
);
CREATE INDEX idx_review_pending ON review_queue(status) WHERE status = 'pending';

-- =====================================================================
-- AUDIT LOG
-- =====================================================================

CREATE TABLE audit_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID REFERENCES users(id),
    action      TEXT NOT NULL,
    target_type TEXT,
    target_id   UUID,
    old_values  JSONB,
    new_values  JSONB,
    ip_address  TEXT,
    created_at  TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_audit_actor ON audit_log(actor_id, created_at DESC);
CREATE INDEX idx_audit_target ON audit_log(target_type, target_id);
```

---

## 3. Spring Boot Backend

### 3.1 Project Structure

```
src/main/java/com/tradeintel/
├── TradeintelApplication.java
├── config/
│   ├── SecurityConfig.java          -- OAuth2 + JWT + CORS
│   ├── OpenAIConfig.java            -- OpenAI client bean
│   ├── AsyncConfig.java             -- @Async thread pool
│   ├── CacheConfig.java             -- Caffeine cache (replaces Redis)
│   └── WebSocketConfig.java         -- STOMP WebSocket for live updates
│
├── auth/
│   ├── GoogleOAuth2UserService.java -- Upserts user on login
│   ├── JwtTokenProvider.java        -- Issues/validates JWT
│   ├── JwtAuthFilter.java           -- Extracts JWT from requests
│   └── UserPrincipal.java
│
├── webhook/
│   ├── WhapiWebhookController.java  -- POST /api/webhooks/whapi
│   ├── WhapiSignatureValidator.java
│   └── WhapiMessageDTO.java
│
├── archive/
│   ├── MessageArchiveService.java   -- Stores raw messages
│   ├── MediaDownloadService.java    -- Downloads media to S3/local
│   └── EmbeddingService.java        -- Generates OpenAI embeddings
│
├── processing/
│   ├── MessageProcessingService.java -- Orchestrates pipeline
│   ├── JargonExpander.java          -- Pre-expands known acronyms
│   ├── LLMExtractionService.java    -- Calls OpenAI for extraction
│   ├── ConfidenceRouter.java        -- Routes by confidence score
│   └── NotificationMatcher.java     -- Matches new listings to rules
│
├── listing/
│   ├── ListingController.java       -- CRUD + search endpoints
│   ├── ListingService.java
│   ├── ListingRepository.java
│   ├── ListingSearchService.java    -- Semantic + direct search
│   └── dto/
│       ├── ListingDTO.java
│       ├── ListingSearchRequest.java
│       └── ListingSearchResponse.java
│
├── chat/
│   ├── ChatController.java          -- POST /api/chat
│   ├── ChatService.java             -- Manages sessions + messages
│   ├── ChatAgentService.java        -- OpenAI function-calling agent
│   ├── CostTrackingService.java     -- Tracks tokens + cost
│   └── tools/                       -- Agent tool implementations
│       ├── SearchListingsTool.java
│       ├── MarketStatsTool.java
│       ├── CreateNotificationTool.java
│       └── SearchMessagesTool.java
│
├── replay/
│   ├── ReplayController.java        -- GET /api/messages (paginated)
│   ├── MessageSearchService.java    -- Semantic + text search on archive
│   └── dto/
│       ├── ReplayMessageDTO.java
│       └── MessageSearchRequest.java
│
├── normalize/                        -- Admin CRUD for normalized values
│   ├── CategoryController.java
│   ├── ManufacturerController.java
│   ├── UnitController.java
│   ├── ConditionController.java
│   ├── JargonController.java
│   └── services + repositories...
│
├── notification/
│   ├── NotificationController.java
│   ├── NotificationRuleService.java
│   ├── NotificationDispatcher.java  -- Email sending
│   └── NLRuleParser.java            -- OpenAI parses NL → structured
│
├── review/
│   ├── ReviewController.java
│   ├── ReviewService.java
│   └── dto/...
│
├── admin/
│   ├── AdminController.java         -- Uber admin endpoints
│   ├── UserManagementService.java
│   ├── AuditService.java
│   └── CostReportService.java
│
├── common/
│   ├── entity/                      -- JPA entities
│   ├── exception/                   -- Global exception handler
│   ├── security/                    -- Role-based access annotations
│   └── openai/                      -- OpenAI client wrapper
│       ├── OpenAIClient.java
│       ├── ChatCompletionRequest.java
│       ├── EmbeddingRequest.java
│       └── FunctionDefinition.java
│
└── resources/
    ├── application.yml
    ├── db/migration/                -- Flyway migrations
    │   ├── V001__initial_schema.sql
    │   ├── V002__seed_data.sql
    │   └── ...
    └── prompts/
        ├── extraction.txt           -- LLM extraction prompt template
        ├── chat_system.txt          -- Chat agent system prompt
        └── rule_parser.txt          -- NL notification rule parser
```

### 3.2 Key Services

#### Whapi Webhook Receiver
```java
@RestController
@RequestMapping("/api/webhooks")
public class WhapiWebhookController {

    private final MessageArchiveService archiveService;
    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/whapi")
    public ResponseEntity<Void> receiveMessage(
            @RequestBody WhapiMessageDTO payload,
            @RequestHeader("X-Whapi-Signature") String signature) {

        // 1. Validate signature
        if (!WhapiSignatureValidator.isValid(payload, signature)) {
            return ResponseEntity.status(401).build();
        }

        // 2. Archive immediately (idempotent via whapi_msg_id unique constraint)
        RawMessage saved = archiveService.archive(payload);

        // 3. Fire async processing event (replaces Redis pub/sub)
        if (saved != null) {
            eventPublisher.publishEvent(new NewMessageEvent(saved.getId()));
        }

        return ResponseEntity.ok().build();
    }
}
```

#### Async Processing Pipeline
```java
@Service
public class MessageProcessingService {

    private final JargonExpander jargonExpander;
    private final LLMExtractionService llmExtractor;
    private final ConfidenceRouter confidenceRouter;
    private final NotificationMatcher notificationMatcher;
    private final EmbeddingService embeddingService;

    @Async("processingExecutor")
    @TransactionalEventListener
    public void onNewMessage(NewMessageEvent event) {
        RawMessage msg = rawMessageRepo.findById(event.getMessageId()).orElseThrow();

        try {
            // Step 1: Generate embedding for semantic search
            float[] embedding = embeddingService.embed(msg.getMessageBody());
            msg.setEmbedding(embedding);

            // Step 2: Pre-expand known jargon
            String expandedText = jargonExpander.expand(msg.getMessageBody());

            // Step 3: LLM extraction (intent + normalization)
            ExtractionResult result = llmExtractor.extract(expandedText, msg.getMessageBody());

            // Step 4: Route by confidence
            List<Listing> listings = confidenceRouter.route(result, msg);

            // Step 5: Match notifications for auto-accepted listings
            for (Listing listing : listings) {
                if (listing.getStatus() == ListingStatus.ACTIVE) {
                    notificationMatcher.matchAndDispatch(listing);
                }
            }

            // Step 6: Learn new jargon
            if (result.getUnknownTerms() != null) {
                jargonExpander.learnNewTerms(result.getUnknownTerms());
            }

            msg.setProcessed(true);
        } catch (Exception e) {
            msg.setProcessingError(e.getMessage());
        }
        rawMessageRepo.save(msg);
    }
}
```

#### OpenAI Extraction
```java
@Service
public class LLMExtractionService {

    private final OpenAIClient openAI;
    private final JargonService jargonService;
    private final CategoryService categoryService;
    private final ManufacturerService manufacturerService;

    private static final String EXTRACTION_PROMPT = """
        You are a trade message classifier for industrial surplus markets.
        
        KNOWN CATEGORIES: %s
        KNOWN MANUFACTURERS: %s
        JARGON DICTIONARY: %s
        
        Given this WhatsApp message, extract a JSON object:
        {
          "intent": "sell" | "want" | "unknown",
          "items": [{
            "description": "normalized plain-English description",
            "category": "match to KNOWN CATEGORIES if possible",
            "manufacturer": "match to KNOWN MANUFACTURERS if possible",
            "part_number": "string or null",
            "quantity": number or null,
            "unit": "ea|ft|lot|lbs|etc",
            "price": number or null,
            "condition": "new|used|surplus|NOS|refurbished|etc"
          }],
          "unknown_terms": ["any acronyms you don't recognize"],
          "confidence": 0.0 to 1.0
        }
        
        IMPORTANT: Match categories and manufacturers to the known lists. 
        If no match, use your best normalized description.
        
        MESSAGE: "%s"
        """;

    public ExtractionResult extract(String expandedText, String originalText) {
        String categories = categoryService.getAllNamesAsCSV();
        String manufacturers = manufacturerService.getAllNamesWithAliasesAsCSV();
        String jargon = jargonService.getVerifiedAsCSV();

        String prompt = String.format(EXTRACTION_PROMPT,
            categories, manufacturers, jargon, expandedText);

        ChatCompletionResponse response = openAI.chatCompletion(
            "gpt-4o-mini",   // cost-efficient for extraction
            List.of(
                new Message("system", "Respond with valid JSON only. No markdown."),
                new Message("user", prompt)
            ),
            0.1  // low temperature for consistent extraction
        );

        return parseExtractionResult(response);
    }
}
```

#### Listing Search (Semantic + Direct)
```java
@Service
public class ListingSearchService {

    private final JdbcTemplate jdbc;
    private final EmbeddingService embeddingService;

    /**
     * Combined search: semantic similarity + direct filters on normalized values.
     */
    public Page<ListingDTO> search(ListingSearchRequest req, Pageable pageable) {

        StringBuilder sql = new StringBuilder("""
            SELECT l.*, c.name as category_name, m.name as manufacturer_name,
                   u.abbreviation as unit_abbr, co.name as condition_name
            FROM listings l
            LEFT JOIN categories c ON l.item_category_id = c.id
            LEFT JOIN manufacturers m ON l.manufacturer_id = m.id
            LEFT JOIN units u ON l.unit_id = u.id
            LEFT JOIN conditions co ON l.condition_id = co.id
            WHERE l.status = 'active'
        """);
        List<Object> params = new ArrayList<>();

        // Direct filters on normalized columns
        if (req.getIntent() != null) {
            sql.append(" AND l.intent = ?::intent_type");
            params.add(req.getIntent());
        }
        if (req.getCategoryId() != null) {
            sql.append(" AND l.item_category_id = ?");
            params.add(req.getCategoryId());
        }
        if (req.getManufacturerId() != null) {
            sql.append(" AND l.manufacturer_id = ?");
            params.add(req.getManufacturerId());
        }
        if (req.getConditionId() != null) {
            sql.append(" AND l.condition_id = ?");
            params.add(req.getConditionId());
        }
        if (req.getPartNumber() != null) {
            sql.append(" AND l.part_number ILIKE ?");
            params.add("%" + req.getPartNumber() + "%");
        }
        if (req.getPriceMin() != null) {
            sql.append(" AND l.price >= ?");
            params.add(req.getPriceMin());
        }
        if (req.getPriceMax() != null) {
            sql.append(" AND l.price <= ?");
            params.add(req.getPriceMax());
        }
        if (req.getDateFrom() != null) {
            sql.append(" AND l.created_at >= ?");
            params.add(req.getDateFrom());
        }
        if (req.getDateTo() != null) {
            sql.append(" AND l.created_at <= ?");
            params.add(req.getDateTo());
        }

        // Text search (trigram fuzzy match on description)
        if (req.getTextQuery() != null && !req.getTextQuery().isBlank()) {
            sql.append(" AND l.item_description % ?");  // pg_trgm similarity
            params.add(req.getTextQuery());
        }

        // Semantic search (vector similarity)
        if (req.getSemanticQuery() != null && !req.getSemanticQuery().isBlank()) {
            float[] queryVec = embeddingService.embed(req.getSemanticQuery());
            sql.append(" ORDER BY l.embedding <=> ?::vector");
            params.add(pgVectorString(queryVec));
        } else {
            sql.append(" ORDER BY l.created_at DESC");
        }

        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageable.getPageSize());
        params.add(pageable.getOffset());

        // ... execute and map results
    }
}
```

#### Message Replay Search
```java
@Service
public class MessageSearchService {

    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbc;

    /**
     * Search archived messages — supports:
     * 1. Semantic search (what was that message about X?)
     * 2. Text search (find messages containing "316 SS")
     * 3. Filters: group, sender, date range
     */
    public Page<ReplayMessageDTO> search(MessageSearchRequest req, Pageable pageable) {

        StringBuilder sql = new StringBuilder("""
            SELECT rm.*, wg.group_name, wg.avatar_url as group_avatar
            FROM raw_messages rm
            JOIN whatsapp_groups wg ON rm.group_id = wg.id
            WHERE 1=1
        """);
        List<Object> params = new ArrayList<>();

        if (req.getGroupId() != null) {
            sql.append(" AND rm.group_id = ?");
            params.add(req.getGroupId());
        }
        if (req.getSenderName() != null) {
            sql.append(" AND rm.sender_name ILIKE ?");
            params.add("%" + req.getSenderName() + "%");
        }
        if (req.getDateFrom() != null) {
            sql.append(" AND rm.timestamp_wa >= ?");
            params.add(req.getDateFrom());
        }
        if (req.getDateTo() != null) {
            sql.append(" AND rm.timestamp_wa <= ?");
            params.add(req.getDateTo());
        }

        // Text search
        if (req.getTextQuery() != null) {
            sql.append(" AND rm.message_body % ?");
            params.add(req.getTextQuery());
        }

        // Semantic search
        if (req.getSemanticQuery() != null) {
            float[] vec = embeddingService.embed(req.getSemanticQuery());
            sql.append(" ORDER BY rm.embedding <=> ?::vector LIMIT ?");
            params.add(pgVectorString(vec));
            params.add(pageable.getPageSize());
        } else {
            sql.append(" ORDER BY rm.timestamp_wa DESC LIMIT ? OFFSET ?");
            params.add(pageable.getPageSize());
            params.add(pageable.getOffset());
        }

        // ... execute
    }
}
```

### 3.3 Security Config
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/webhooks/**"))
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public
                .requestMatchers("/api/webhooks/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                // User endpoints
                .requestMatchers("/api/chat/**").authenticated()
                .requestMatchers("/api/listings/**").authenticated()
                .requestMatchers("/api/notifications/**").authenticated()
                .requestMatchers("/api/messages/**").authenticated()
                // Admin
                .requestMatchers("/api/review/**").hasAnyRole("ADMIN", "UBER_ADMIN")
                .requestMatchers("/api/normalize/**").hasAnyRole("ADMIN", "UBER_ADMIN")
                .requestMatchers("/api/jargon/**").hasAnyRole("ADMIN", "UBER_ADMIN")
                // Uber admin
                .requestMatchers("/api/admin/**").hasRole("UBER_ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth -> oauth
                .userInfoEndpoint(u -> u.userService(googleOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

### 3.4 REST API Endpoints

```
AUTH
  POST   /api/auth/google          -- OAuth2 callback → returns JWT
  GET    /api/auth/me              -- Current user profile

WEBHOOKS
  POST   /api/webhooks/whapi       -- Whapi message receiver

MESSAGE REPLAY (WhatsApp-style UI)
  GET    /api/messages                          -- Paginated, filterable
  GET    /api/messages/search?q=&semantic=      -- Text + semantic search
  GET    /api/messages/groups                   -- List monitored groups
  GET    /api/messages/groups/{id}/messages     -- Messages for a specific group

LISTINGS
  GET    /api/listings                          -- Search/filter/paginate
  GET    /api/listings/{id}                     -- Single listing detail
  GET    /api/listings/stats                    -- Aggregates: sell vs want counts, etc.
  PUT    /api/listings/{id}        [ADMIN+]     -- Edit listing
  DELETE /api/listings/{id}        [UBER_ADMIN] -- Soft-delete listing

CHAT (AI Query Interface)
  POST   /api/chat/sessions                    -- Create new session
  GET    /api/chat/sessions                    -- List user's sessions
  GET    /api/chat/sessions/{id}               -- Get session messages
  POST   /api/chat/sessions/{id}/messages      -- Send message → get AI response
  GET    /api/chat/cost                        -- User's own cost summary

NOTIFICATIONS
  GET    /api/notifications                    -- List user's rules
  POST   /api/notifications                    -- Create rule (NL → parsed)
  PUT    /api/notifications/{id}               -- Update rule
  DELETE /api/notifications/{id}               -- Delete rule

REVIEW QUEUE [ADMIN+]
  GET    /api/review                           -- Pending review items
  POST   /api/review/{id}/resolve              -- Resolve with corrections
  POST   /api/review/{id}/skip                 -- Skip item

NORMALIZED VALUE ADMIN [ADMIN+]
  GET    /api/normalize/categories             -- List all categories
  POST   /api/normalize/categories             -- Create category
  PUT    /api/normalize/categories/{id}        -- Update category
  DELETE /api/normalize/categories/{id}        -- Deactivate category
  -- Same pattern for: manufacturers, units, conditions

JARGON DICTIONARY [ADMIN+]
  GET    /api/jargon                           -- List/search jargon
  POST   /api/jargon                           -- Add entry
  PUT    /api/jargon/{id}                      -- Update/verify entry
  DELETE /api/jargon/{id}                      -- Remove entry

UBER ADMIN [UBER_ADMIN]
  GET    /api/admin/users                      -- All users
  PUT    /api/admin/users/{id}/role            -- Change user role
  PUT    /api/admin/users/{id}/active          -- Enable/disable user
  GET    /api/admin/users/{id}/chats           -- View any user's chat history
  GET    /api/admin/costs                      -- All users' cost breakdown
  GET    /api/admin/costs/export               -- CSV export
  GET    /api/admin/audit                      -- Audit log
  GET    /api/admin/groups                     -- Manage WhatsApp groups
  POST   /api/admin/groups                     -- Add group to monitor
  PUT    /api/admin/groups/{id}                -- Update group settings
  DELETE /api/admin/groups/{id}                -- Stop monitoring group
```

---

## 4. React Frontend

### 4.1 Project Structure

```
src/
├── main.tsx
├── App.tsx                        -- Routes + AuthProvider
├── api/
│   ├── client.ts                  -- Axios instance with JWT interceptor
│   ├── auth.ts
│   ├── messages.ts
│   ├── listings.ts
│   ├── chat.ts
│   ├── notifications.ts
│   ├── normalize.ts
│   ├── review.ts
│   └── admin.ts
│
├── hooks/
│   ├── useAuth.ts
│   ├── useInfiniteScroll.ts       -- For message replay
│   ├── useDebounce.ts
│   └── useWebSocket.ts            -- Live message updates
│
├── components/
│   ├── layout/
│   │   ├── AppShell.tsx            -- Sidebar + main content
│   │   ├── Sidebar.tsx
│   │   └── TopBar.tsx
│   │
│   ├── replay/                     -- WhatsApp-style message viewer
│   │   ├── ReplayView.tsx          -- Main container
│   │   ├── GroupList.tsx           -- Left panel: group list
│   │   ├── MessageThread.tsx      -- Center: scrollable messages
│   │   ├── MessageBubble.tsx      -- Individual message (WA style)
│   │   ├── MessageSearch.tsx      -- Search bar (text + semantic)
│   │   ├── SearchResults.tsx      -- Highlighted results
│   │   └── MediaPreview.tsx       -- Image/doc thumbnails
│   │
│   ├── chat/                       -- AI query interface
│   │   ├── ChatView.tsx
│   │   ├── ChatSidebar.tsx        -- Session list
│   │   ├── ChatThread.tsx         -- Messages
│   │   ├── ChatInput.tsx          -- Input with send
│   │   └── ToolResultCard.tsx     -- Formatted tool call results
│   │
│   ├── listings/                   -- Browse sell/want inventory
│   │   ├── ListingsView.tsx
│   │   ├── ListingFilters.tsx     -- Category, mfr, price, intent, condition
│   │   ├── ListingCard.tsx
│   │   ├── ListingDetail.tsx
│   │   └── ListingEditModal.tsx   -- Admin edit form
│   │
│   ├── notifications/
│   │   ├── NotificationRules.tsx
│   │   ├── RuleCard.tsx
│   │   └── CreateRuleModal.tsx    -- NL input
│   │
│   ├── review/                     -- Admin: human review queue
│   │   ├── ReviewQueue.tsx
│   │   ├── ReviewCard.tsx         -- Shows original + LLM guess + edit form
│   │   └── JargonReviewCard.tsx
│   │
│   ├── admin/                      -- Uber admin dashboard
│   │   ├── AdminDashboard.tsx
│   │   ├── UserManagement.tsx
│   │   ├── AllChatsView.tsx       -- Browse any user's chats
│   │   ├── CostReport.tsx
│   │   ├── AuditLog.tsx
│   │   ├── GroupManagement.tsx
│   │   └── normalize/             -- CRUD for normalized values
│   │       ├── CategoriesAdmin.tsx
│   │       ├── ManufacturersAdmin.tsx
│   │       ├── UnitsAdmin.tsx
│   │       ├── ConditionsAdmin.tsx
│   │       └── JargonAdmin.tsx
│   │
│   └── common/
│       ├── DataTable.tsx           -- Reusable sortable/filterable table
│       ├── Pagination.tsx
│       ├── ConfirmDialog.tsx
│       ├── Badge.tsx
│       ├── EmptyState.tsx
│       └── LoadingSpinner.tsx
│
├── pages/                          -- Route-level components
│   ├── LoginPage.tsx
│   ├── ReplayPage.tsx
│   ├── ChatPage.tsx
│   ├── ListingsPage.tsx
│   ├── NotificationsPage.tsx
│   ├── ReviewPage.tsx
│   ├── AdminPage.tsx
│   └── CostPage.tsx
│
├── contexts/
│   ├── AuthContext.tsx
│   └── WebSocketContext.tsx
│
├── types/
│   ├── message.ts
│   ├── listing.ts
│   ├── chat.ts
│   ├── user.ts
│   └── normalize.ts
│
└── utils/
    ├── formatters.ts               -- Date, currency, phone
    ├── colors.ts                   -- Sender color assignment
    └── constants.ts
```

### 4.2 WhatsApp Replay UI — Component Design

```
┌─────────────────────────────────────────────────────────┐
│ ┌───────────┐ ┌───────────────────────────────────────┐ │
│ │           │ │  🔍 Search messages...    [Semantic ▼] │ │
│ │  Groups   │ ├───────────────────────────────────────┤ │
│ │           │ │          March 15, 2026                │ │
│ │ ● Surplus │ │                                       │ │
│ │   Trading │ │  ┌─────────────────────────────┐      │ │
│ │           │ │  │ John D.              10:23 AM│      │ │
│ │   Pipe    │ │  │ WTS: 500ft 316 SS SMLS pipe │      │ │
│ │   Deals   │ │  │ 4" Sch40 NOS $12/ft OBO     │      │ │
│ │           │ │  └─────────────────────────────┘      │ │
│ │   Valve   │ │                                       │ │
│ │   Market  │ │      ┌─────────────────────────────┐  │ │
│ │           │ │      │ 10:31 AM           Sarah M. │  │ │
│ │           │ │      │  WTB: 316 SS pipe 2-4"      │  │ │
│ │           │ │      │  Need 1000ft min, surplus ok │  │ │
│ │           │ │      └─────────────────────────────┘  │ │
│ │           │ │                                       │ │
│ │           │ │  ┌─────────────────────────────┐      │ │
│ │           │ │  │ Mike R.              10:45 AM│      │ │
│ │           │ │  │ 📷 [image]                   │      │ │
│ │           │ │  │ WTS: Parker 3-way valve      │      │ │
│ │           │ │  │ P/N HV-4320 $450 ea, qty 12  │      │ │
│ │           │ │  └─────────────────────────────┘      │ │
│ │           │ │                                       │ │
│ │           │ │  ┌──────────────────┐                 │ │
│ │           │ │  │ ● SELL  ● WANT  │ ← intent badges │ │
│ │           │ │  └──────────────────┘                 │ │
│ └───────────┘ └───────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

#### MessageBubble.tsx — Key Design
```tsx
interface MessageBubbleProps {
  message: ReplayMessage;
  isSearchResult?: boolean;
  highlightText?: string;
}

function MessageBubble({ message, isSearchResult, highlightText }: MessageBubbleProps) {
  // Color-code by sender (consistent hash → color)
  const senderColor = getSenderColor(message.senderName);

  // If this message generated a listing, show intent badge
  const listing = message.extractedListing;

  return (
    <div className={cn(
      "max-w-[75%] rounded-lg px-3 py-2 mb-2 shadow-sm",
      "bg-white border border-gray-100",
      isSearchResult && "ring-2 ring-blue-400"
    )}>
      {/* Sender name */}
      <div className="flex items-center gap-2 mb-1">
        <span className="text-sm font-semibold" style={{ color: senderColor }}>
          {message.senderName}
        </span>
        <span className="text-xs text-gray-400">
          {formatTime(message.timestampWa)}
        </span>
      </div>

      {/* Media */}
      {message.mediaUrl && <MediaPreview message={message} />}

      {/* Message body with optional highlighting */}
      <p className="text-sm text-gray-800 whitespace-pre-wrap">
        {highlightText
          ? highlightMatches(message.messageBody, highlightText)
          : message.messageBody}
      </p>

      {/* Intent badge if listing extracted */}
      {listing && (
        <div className="mt-2 flex items-center gap-2">
          <Badge variant={listing.intent === 'sell' ? 'green' : 'blue'}>
            {listing.intent === 'sell' ? '📤 SELL' : '📥 WANT'}
          </Badge>
          <span className="text-xs text-gray-500">
            {listing.itemDescription}
          </span>
        </div>
      )}
    </div>
  );
}
```

#### Search Modes
```tsx
function MessageSearch({ groupId, onResultSelect }: Props) {
  const [query, setQuery] = useState('');
  const [mode, setMode] = useState<'text' | 'semantic'>('text');

  const { data, isLoading } = useQuery({
    queryKey: ['messageSearch', groupId, query, mode],
    queryFn: () => api.messages.search({
      groupId,
      textQuery: mode === 'text' ? query : undefined,
      semanticQuery: mode === 'semantic' ? query : undefined,
    }),
    enabled: query.length > 2,
  });

  return (
    <div className="flex items-center gap-2 p-3 border-b">
      <Search className="w-4 h-4 text-gray-400" />
      <input
        className="flex-1 outline-none text-sm"
        placeholder={mode === 'semantic'
          ? "Describe what you're looking for..."
          : "Search messages..."}
        value={query}
        onChange={e => setQuery(e.target.value)}
      />
      <select
        value={mode}
        onChange={e => setMode(e.target.value as 'text' | 'semantic')}
        className="text-xs border rounded px-2 py-1"
      >
        <option value="text">Text Match</option>
        <option value="semantic">Semantic</option>
      </select>
    </div>
  );
}
```

### 4.3 Admin Normalized Values CRUD

Each normalized table (categories, manufacturers, units, conditions) gets a standard CRUD admin page:

```tsx
function CategoriesAdmin() {
  const { data: categories } = useQuery(['categories'], api.normalize.listCategories);
  const createMutation = useMutation(api.normalize.createCategory);
  const updateMutation = useMutation(api.normalize.updateCategory);
  const deleteMutation = useMutation(api.normalize.deleteCategory);

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-lg font-semibold">Categories</h2>
        <Button onClick={() => setShowCreate(true)}>+ Add Category</Button>
      </div>

      <DataTable
        columns={[
          { key: 'name', label: 'Name', sortable: true },
          { key: 'parentName', label: 'Parent Category' },
          { key: 'listingCount', label: 'Listings', sortable: true },
          { key: 'isActive', label: 'Active', render: (v) => <Toggle checked={v} /> },
          { key: 'actions', label: '', render: (_, row) => (
            <div className="flex gap-2">
              <Button size="sm" variant="ghost" onClick={() => openEdit(row)}>Edit</Button>
              <Button size="sm" variant="ghost" className="text-red-500"
                      onClick={() => confirmDelete(row)}>Deactivate</Button>
            </div>
          )},
        ]}
        data={categories}
        searchable
      />

      {/* Same pattern for create/edit modals */}
    </div>
  );
}
```

### 4.4 Route Map

```tsx
<Routes>
  <Route path="/login" element={<LoginPage />} />

  {/* Authenticated routes */}
  <Route element={<AuthGuard />}>
    <Route element={<AppShell />}>
      <Route path="/" element={<Navigate to="/replay" />} />
      <Route path="/replay" element={<ReplayPage />} />
      <Route path="/chat" element={<ChatPage />} />
      <Route path="/listings" element={<ListingsPage />} />
      <Route path="/notifications" element={<NotificationsPage />} />
      <Route path="/costs" element={<CostPage />} />

      {/* Admin routes */}
      <Route element={<RoleGuard roles={['admin', 'uber_admin']} />}>
        <Route path="/review" element={<ReviewPage />} />
        <Route path="/admin/categories" element={<CategoriesAdmin />} />
        <Route path="/admin/manufacturers" element={<ManufacturersAdmin />} />
        <Route path="/admin/units" element={<UnitsAdmin />} />
        <Route path="/admin/conditions" element={<ConditionsAdmin />} />
        <Route path="/admin/jargon" element={<JargonAdmin />} />
      </Route>

      {/* Uber admin routes */}
      <Route element={<RoleGuard roles={['uber_admin']} />}>
        <Route path="/admin/users" element={<UserManagement />} />
        <Route path="/admin/chats" element={<AllChatsView />} />
        <Route path="/admin/costs" element={<CostReport />} />
        <Route path="/admin/audit" element={<AuditLog />} />
        <Route path="/admin/groups" element={<GroupManagement />} />
      </Route>
    </Route>
  </Route>
</Routes>
```

---

## 5. Navigation Sidebar

```
┌──────────────────────┐
│  📱 Trade Intel       │
│                       │
│  💬 Message Replay    │  ← WhatsApp-style viewer
│  🤖 Ask AI           │  ← Natural language queries
│  📋 Listings         │  ← Browse sell/want inventory
│  🔔 Notifications    │  ← Manage alert rules
│  💰 My Usage         │  ← Personal cost tracking
│                       │
│  ── Admin ──          │  (admin+ only)
│  📝 Review Queue     │
│  📂 Categories       │
│  🏭 Manufacturers    │
│  📏 Units            │
│  🔧 Conditions       │
│  📖 Jargon Dict      │
│                       │
│  ── Uber Admin ──     │  (uber_admin only)
│  👥 Users            │
│  💬 All Chats        │
│  💵 Cost Report      │
│  📜 Audit Log        │
│  📱 WhatsApp Groups  │
└──────────────────────┘
```

---

## 6. OpenAI Cost Model

```
Extraction Pipeline (per message):
  GPT-4o-mini: ~500 input tokens + ~300 output = ~$0.0002/msg

Embedding (per message + per listing):
  text-embedding-3-small: ~200 tokens = ~$0.000004/embed

User Chat:
  GPT-4o: ~2000 input + ~500 output = ~$0.01/exchange
  GPT-4o-mini (simple queries): ~$0.0005/exchange

Notification Rule Parsing (one-time per rule):
  GPT-4o-mini: ~$0.0003/rule

Cost tracking formula per chat message:
  input_cost  = input_tokens  * (model_rate_per_1M / 1_000_000)
  output_cost = output_tokens * (model_rate_per_1M / 1_000_000)
  total_cost  = input_cost + output_cost
```

---

## 7. application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tradeintel
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate          # Flyway manages schema
    properties:
      hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: openid, email, profile

app:
  openai:
    api-key: ${OPENAI_API_KEY}
    extraction-model: gpt-4o-mini
    chat-model: gpt-4o
    embedding-model: text-embedding-3-small
  whapi:
    api-key: ${WHAPI_API_KEY}
    webhook-secret: ${WHAPI_WEBHOOK_SECRET}
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: 86400000       # 24 hours
  processing:
    async-pool-size: 4
    confidence-auto-threshold: 0.8
    confidence-review-threshold: 0.5
    listing-expiry-days: 60
  cache:
    jargon-ttl-minutes: 10
    categories-ttl-minutes: 30
```

---

## 8. Implementation Phases

### Phase 1 — Foundation (Weeks 1-3)
- [ ] Spring Boot project scaffold + Flyway migrations
- [ ] Google SSO authentication flow + JWT
- [ ] Whapi webhook receiver + raw message archival
- [ ] React app scaffold + routing + auth context
- [ ] WhatsApp replay UI (group list + message thread + infinite scroll)

### Phase 2 — Admin & Normalized Data (Weeks 4-6)
- [ ] Admin CRUD for categories, manufacturers, units, conditions
- [ ] Jargon dictionary CRUD + seed data import
- [ ] Uber admin: user management, role assignment
- [ ] Group management UI (add/remove monitored WhatsApp groups)
- [ ] Audit log infrastructure
- [ ] Listing table + admin edit/delete with audit trail
- [ ] Populate normalized tables with real industry data

### Phase 3 — Intelligence (Weeks 7-9)
- [ ] OpenAI extraction pipeline (intent + normalization against admin-managed values)
- [ ] Embedding generation for messages + listings
- [ ] Jargon auto-learning (LLM discovers → queues for admin verification)
- [ ] Confidence routing + human review queue UI
- [ ] Listing creation from extraction pipeline
- [ ] Semantic + text search on messages and listings
- [ ] Listing browser with filters (category, manufacturer, condition, price)

### Phase 4 — User Features & Polish (Weeks 10-12)
- [ ] AI chat interface with function calling
- [ ] Tool implementations (search, stats, notifications)
- [ ] Notification rule engine (NL → parsed → match → email)
- [ ] Per-user cost tracking + cost dashboard
- [ ] Uber admin: view all chats, cost reports, CSV export
- [ ] Performance tuning + error handling + monitoring
