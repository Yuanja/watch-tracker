-- Add password_hash column for simple username/password login
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

-- Seed default uber_admin account: admin / password88
INSERT INTO users (id, google_id, email, display_name, role, is_active, password_hash, created_at)
VALUES (
    gen_random_uuid(),
    'local-admin',
    'admin@tradeintel.local',
    'Admin',
    'uber_admin',
    true,
    '$2b$12$9WdKgQWnqmb6WzQ9ye7pBe6X5463gzjzmTxIKUtufPLBFb3YBUiXC',
    now()
)
ON CONFLICT (email) DO NOTHING;
