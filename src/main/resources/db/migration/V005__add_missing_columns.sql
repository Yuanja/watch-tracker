-- Add missing parsed_intent column to notification_rules
ALTER TABLE notification_rules ADD COLUMN IF NOT EXISTS parsed_intent VARCHAR(20);
