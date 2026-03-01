-- Convert PostgreSQL enum columns to VARCHAR for Hibernate compatibility.
-- JPA uses @Enumerated(EnumType.STRING) which maps to varchar.

-- users.role
ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(20) USING role::text;
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'user';

-- listings.status
ALTER TABLE listings ALTER COLUMN status TYPE VARCHAR(20) USING status::text;
ALTER TABLE listings ALTER COLUMN status SET DEFAULT 'active';

-- listings.intent
ALTER TABLE listings ALTER COLUMN intent TYPE VARCHAR(20) USING intent::text;

-- Drop the enum types (no longer needed)
DROP TYPE IF EXISTS user_role CASCADE;
DROP TYPE IF EXISTS listing_status CASCADE;
DROP TYPE IF EXISTS intent_type CASCADE;
