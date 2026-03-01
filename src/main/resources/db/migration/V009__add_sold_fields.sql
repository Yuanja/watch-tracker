ALTER TABLE listings ADD COLUMN sold_at TIMESTAMPTZ;
ALTER TABLE listings ADD COLUMN sold_message_id TEXT;
ALTER TABLE listings ADD COLUMN buyer_name TEXT;
