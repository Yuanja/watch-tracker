ALTER TABLE listings ADD COLUMN model_name VARCHAR(255);
CREATE INDEX idx_listings_model_name ON listings(model_name) WHERE model_name IS NOT NULL AND deleted_at IS NULL;
