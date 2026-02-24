-- =====================================================================
-- V001__initial_schema.sql
-- Full initial schema for the WhatsApp Trade Intelligence Platform
-- Tables are ordered by dependency (referenced tables created first)
-- =====================================================================

-- =====================================================================
-- EXTENSIONS
-- =====================================================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "vector";       -- pgvector for semantic search
CREATE EXTENSION IF NOT EXISTS "pg_trgm";      -- trigram for fuzzy text search

-- =====================================================================
-- ENUMS
-- =====================================================================
CREATE TYPE intent_type AS ENUM ('sell', 'want', 'unknown');
CREATE TYPE listing_status AS ENUM ('active', 'expired', 'deleted', 'pending_review');
CREATE TYPE user_role AS ENUM ('user', 'admin', 'uber_admin');

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
-- RAW MESSAGES (the full archive -- WhatsApp replay source)
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
-- USERS & AUTH (must precede listings, chat, notifications, etc.)
-- =====================================================================
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
-- NORMALIZED VALUE TABLES (Admin-managed CRUD)
-- Must precede listings which references these tables
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
-- EXTRACTED LISTINGS
-- Depends on: whatsapp_groups, raw_messages, users, categories,
--             manufacturers, units, conditions
-- =====================================================================
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
-- CHAT SYSTEM (AI queries about data)
-- Depends on: users
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
-- Depends on: users
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
-- Depends on: users
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
-- Depends on: listings, raw_messages, users
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
-- Depends on: users
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
