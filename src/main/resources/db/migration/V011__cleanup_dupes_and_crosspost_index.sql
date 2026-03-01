-- Soft-delete same-message exact duplicates (keep earliest per group)
UPDATE listings
SET    deleted_at = now(), status = 'deleted'
WHERE  id IN (
    SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER (
            PARTITION BY raw_message_id, COALESCE(part_number,''),
                         COALESCE(price::text,''), COALESCE(price_currency,'')
            ORDER BY created_at ASC
        ) AS rn
        FROM listings WHERE deleted_at IS NULL
    ) ranked WHERE rn > 1
);

-- Partial index to speed up cross-post detection queries
CREATE INDEX idx_listing_crosspost
    ON listings(part_number, price, price_currency, sender_name, sender_phone)
    WHERE part_number IS NOT NULL AND price IS NOT NULL AND deleted_at IS NULL;
