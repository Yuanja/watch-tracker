-- H2-compatible schema for integration tests.
-- Replaces Hibernate ddl-auto generated DDL to avoid PostgreSQL-specific
-- column types: vector(1536), text[], uuid[], jsonb.
-- All such columns are mapped to compatible H2 equivalents (CLOB or VARCHAR ARRAY).

-- Lookup / normalisation tables (no complex types) -------------------------

CREATE TABLE IF NOT EXISTS categories (
    id          UUID         NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    parent_id   UUID,
    sort_order  INT          DEFAULT 0,
    is_active   BOOLEAN      DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE,
    updated_at  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS conditions (
    id           UUID         NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
    name         VARCHAR(255) NOT NULL UNIQUE,
    abbreviation VARCHAR(255),
    sort_order   INT          DEFAULT 0,
    is_active    BOOLEAN      DEFAULT TRUE,
    created_at   TIMESTAMP WITH TIME ZONE
);

-- manufacturers: aliases stored as CLOB (replaces text[])
CREATE TABLE IF NOT EXISTS manufacturers (
    id         UUID         NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
    name       VARCHAR(255) NOT NULL UNIQUE,
    aliases    CLOB,
    website    VARCHAR(255),
    is_active  BOOLEAN      DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS units (
    id           UUID         NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
    name         VARCHAR(255) NOT NULL UNIQUE,
    abbreviation VARCHAR(255) NOT NULL UNIQUE,
    is_active    BOOLEAN      DEFAULT TRUE,
    created_at   TIMESTAMP WITH TIME ZONE
);

-- User management ----------------------------------------------------------

CREATE TABLE IF NOT EXISTS users (
    id           UUID         NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
    google_id    VARCHAR(255) NOT NULL UNIQUE,
    email        VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255),
    avatar_url   VARCHAR(255),
    role         VARCHAR(50)  NOT NULL DEFAULT 'user',
    is_active    BOOLEAN      DEFAULT TRUE,
    password_hash VARCHAR(255),
    created_at   TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE
);

-- WhatsApp groups ----------------------------------------------------------

CREATE TABLE IF NOT EXISTS whatsapp_groups (
    id             UUID         NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
    whapi_group_id VARCHAR(255) NOT NULL UNIQUE,
    group_name     VARCHAR(255) NOT NULL,
    description    CLOB,
    avatar_url     VARCHAR(255),
    is_active      BOOLEAN      DEFAULT TRUE,
    created_at     TIMESTAMP WITH TIME ZONE,
    updated_at     TIMESTAMP WITH TIME ZONE
);

-- Raw messages: embedding stored as CLOB (replaces vector(1536)) -----------

CREATE TABLE IF NOT EXISTS raw_messages (
    id              UUID         NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
    group_id        UUID         NOT NULL,
    whapi_msg_id    VARCHAR(255) NOT NULL UNIQUE,
    sender_phone    VARCHAR(255),
    sender_name     VARCHAR(255),
    sender_avatar   VARCHAR(255),
    message_body    CLOB,
    message_type    VARCHAR(50)  DEFAULT 'text',
    media_url       VARCHAR(255),
    media_mime_type VARCHAR(255),
    media_local_path VARCHAR(255),
    reply_to_msg_id VARCHAR(255),
    is_forwarded    BOOLEAN      DEFAULT FALSE,
    timestamp_wa    TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at     TIMESTAMP WITH TIME ZONE,
    processed       BOOLEAN      DEFAULT FALSE,
    processing_error CLOB,
    embedding       CLOB,
    raw_json        CLOB,
    CONSTRAINT fk_raw_msg_group FOREIGN KEY (group_id) REFERENCES whatsapp_groups(id)
);

-- Jargon dictionary --------------------------------------------------------

CREATE TABLE IF NOT EXISTS jargon_dictionary (
    id              UUID         NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
    acronym         VARCHAR(255) NOT NULL,
    expansion       VARCHAR(255) NOT NULL,
    industry        VARCHAR(255),
    context_example CLOB,
    source          VARCHAR(50)  DEFAULT 'llm',
    confidence      DOUBLE       DEFAULT 0.5,
    usage_count     INT          DEFAULT 1,
    verified        BOOLEAN      DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_jargon_acronym_expansion UNIQUE (acronym, expansion)
);

-- Listings: embedding stored as CLOB (replaces vector(1536)) ---------------

CREATE TABLE IF NOT EXISTS listings (
    id                 UUID          NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
    raw_message_id     UUID          NOT NULL,
    group_id           UUID          NOT NULL,
    intent             VARCHAR(50)   NOT NULL,
    confidence_score   DOUBLE        NOT NULL DEFAULT 0.0,
    item_description   CLOB          NOT NULL,
    item_category_id   UUID,
    manufacturer_id    UUID,
    part_number        VARCHAR(255),
    quantity           NUMERIC(19,4),
    unit_id            UUID,
    price              NUMERIC(19,4),
    price_currency     VARCHAR(10)   DEFAULT 'USD',
    condition_id       UUID,
    original_text      CLOB          NOT NULL,
    sender_name        VARCHAR(255),
    sender_phone       VARCHAR(255),
    status             VARCHAR(50)   DEFAULT 'active',
    needs_human_review BOOLEAN       DEFAULT FALSE,
    reviewed_by        UUID,
    reviewed_at        TIMESTAMP WITH TIME ZONE,
    embedding          CLOB,
    created_at         TIMESTAMP WITH TIME ZONE,
    updated_at         TIMESTAMP WITH TIME ZONE,
    expires_at         TIMESTAMP WITH TIME ZONE,
    deleted_at         TIMESTAMP WITH TIME ZONE,
    deleted_by         UUID,
    sold_at            TIMESTAMP WITH TIME ZONE,
    sold_message_id    VARCHAR(255),
    buyer_name         VARCHAR(255),
    CONSTRAINT fk_listing_raw_msg   FOREIGN KEY (raw_message_id)   REFERENCES raw_messages(id),
    CONSTRAINT fk_listing_group     FOREIGN KEY (group_id)         REFERENCES whatsapp_groups(id),
    CONSTRAINT fk_listing_category  FOREIGN KEY (item_category_id) REFERENCES categories(id),
    CONSTRAINT fk_listing_mfr       FOREIGN KEY (manufacturer_id)  REFERENCES manufacturers(id),
    CONSTRAINT fk_listing_unit      FOREIGN KEY (unit_id)          REFERENCES units(id),
    CONSTRAINT fk_listing_condition FOREIGN KEY (condition_id)     REFERENCES conditions(id),
    CONSTRAINT fk_listing_reviewer  FOREIGN KEY (reviewed_by)      REFERENCES users(id),
    CONSTRAINT fk_listing_deleter   FOREIGN KEY (deleted_by)       REFERENCES users(id)
);

-- Notification rules: arrays stored as CLOB (replaces text[], uuid[]) -----

CREATE TABLE IF NOT EXISTS notification_rules (
    id                   UUID          NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id              UUID          NOT NULL,
    nl_rule              CLOB          NOT NULL,
    parsed_intent        VARCHAR(50),
    parsed_keywords      CLOB,
    parsed_category_ids  CLOB,
    parsed_price_min     NUMERIC(19,4),
    parsed_price_max     NUMERIC(19,4),
    notify_channel       VARCHAR(50)   DEFAULT 'email',
    notify_email         VARCHAR(255),
    is_active            BOOLEAN       DEFAULT TRUE,
    last_triggered       TIMESTAMP WITH TIME ZONE,
    created_at           TIMESTAMP WITH TIME ZONE,
    updated_at           TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_notif_rule_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Review queue -------------------------------------------------------------

CREATE TABLE IF NOT EXISTS review_queue (
    id              UUID         NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
    listing_id      UUID,
    raw_message_id  UUID         NOT NULL,
    reason          CLOB         NOT NULL,
    llm_explanation CLOB,
    suggested_values CLOB,
    status          VARCHAR(50)  DEFAULT 'pending',
    resolved_by     UUID,
    resolution      CLOB,
    created_at      TIMESTAMP WITH TIME ZONE,
    resolved_at     TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_review_listing    FOREIGN KEY (listing_id)     REFERENCES listings(id),
    CONSTRAINT fk_review_raw_msg    FOREIGN KEY (raw_message_id) REFERENCES raw_messages(id),
    CONSTRAINT fk_review_resolver   FOREIGN KEY (resolved_by)    REFERENCES users(id)
);

-- Chat sessions and messages -----------------------------------------------

CREATE TABLE IF NOT EXISTS chat_sessions (
    id         UUID         NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id    UUID         NOT NULL,
    title      VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_chat_session_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id           UUID          NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
    session_id   UUID          NOT NULL,
    role         VARCHAR(50)   NOT NULL,
    content      CLOB          NOT NULL,
    model_used   VARCHAR(255),
    input_tokens INT           DEFAULT 0,
    output_tokens INT          DEFAULT 0,
    cost_usd     NUMERIC(10,6) DEFAULT 0,
    tool_calls   CLOB,
    created_at   TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_chat_msg_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
);

-- Usage ledger -------------------------------------------------------------

CREATE TABLE IF NOT EXISTS usage_ledger (
    id                   UUID          NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id              UUID          NOT NULL,
    period_date          DATE          NOT NULL,
    total_input_tokens   BIGINT        DEFAULT 0,
    total_output_tokens  BIGINT        DEFAULT 0,
    total_cost_usd       NUMERIC(12,6) DEFAULT 0,
    session_count        INT           DEFAULT 0,
    CONSTRAINT uq_usage_ledger_user_date UNIQUE (user_id, period_date),
    CONSTRAINT fk_usage_ledger_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Audit log ----------------------------------------------------------------

CREATE TABLE IF NOT EXISTS audit_log (
    id          UUID         NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
    actor_id    UUID,
    action      VARCHAR(255) NOT NULL,
    target_type VARCHAR(255),
    target_id   UUID,
    old_values  CLOB,
    new_values  CLOB,
    ip_address  VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id)
);
