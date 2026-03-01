-- Clean up industrial surplus seed data and replace with watch industry metadata

-- Clear existing data
DELETE FROM categories;
DELETE FROM manufacturers;
DELETE FROM units;
DELETE FROM conditions;

-- =========================================================================
-- Categories (watch types/complications)
-- =========================================================================
INSERT INTO categories (id, name, created_at) VALUES
  (gen_random_uuid(), 'Dress Watch', now()),
  (gen_random_uuid(), 'Sports Watch', now()),
  (gen_random_uuid(), 'Dive Watch', now()),
  (gen_random_uuid(), 'Pilot Watch', now()),
  (gen_random_uuid(), 'Chronograph', now()),
  (gen_random_uuid(), 'GMT / Dual Time', now()),
  (gen_random_uuid(), 'Perpetual Calendar', now()),
  (gen_random_uuid(), 'Minute Repeater', now()),
  (gen_random_uuid(), 'Tourbillon', now()),
  (gen_random_uuid(), 'Skeleton Watch', now()),
  (gen_random_uuid(), 'Moon Phase', now()),
  (gen_random_uuid(), 'World Timer', now()),
  (gen_random_uuid(), 'Annual Calendar', now()),
  (gen_random_uuid(), 'Digital Watch', now()),
  (gen_random_uuid(), 'Smart Watch', now()),
  (gen_random_uuid(), 'Field Watch', now()),
  (gen_random_uuid(), 'Racing Watch', now()),
  (gen_random_uuid(), 'Jewelry Watch', now());

-- =========================================================================
-- Manufacturers (watch brands)
-- =========================================================================
INSERT INTO manufacturers (id, name, created_at) VALUES
  -- Holy Trinity
  (gen_random_uuid(), 'Patek Philippe', now()),
  (gen_random_uuid(), 'Audemars Piguet', now()),
  (gen_random_uuid(), 'Vacheron Constantin', now()),
  -- Rolex & Tudor
  (gen_random_uuid(), 'Rolex', now()),
  (gen_random_uuid(), 'Tudor', now()),
  -- Richemont Group
  (gen_random_uuid(), 'Cartier', now()),
  (gen_random_uuid(), 'IWC', now()),
  (gen_random_uuid(), 'Jaeger-LeCoultre', now()),
  (gen_random_uuid(), 'Panerai', now()),
  (gen_random_uuid(), 'Piaget', now()),
  (gen_random_uuid(), 'Roger Dubuis', now()),
  (gen_random_uuid(), 'A. Lange & Söhne', now()),
  -- Swatch Group
  (gen_random_uuid(), 'Omega', now()),
  (gen_random_uuid(), 'Breguet', now()),
  (gen_random_uuid(), 'Blancpain', now()),
  (gen_random_uuid(), 'Longines', now()),
  (gen_random_uuid(), 'Tissot', now()),
  -- LVMH
  (gen_random_uuid(), 'TAG Heuer', now()),
  (gen_random_uuid(), 'Hublot', now()),
  (gen_random_uuid(), 'Zenith', now()),
  (gen_random_uuid(), 'Bulgari', now()),
  -- Independent / Other Major
  (gen_random_uuid(), 'Richard Mille', now()),
  (gen_random_uuid(), 'F.P. Journe', now()),
  (gen_random_uuid(), 'Chopard', now()),
  (gen_random_uuid(), 'Girard-Perregaux', now()),
  (gen_random_uuid(), 'Ulysse Nardin', now()),
  (gen_random_uuid(), 'Breitling', now()),
  (gen_random_uuid(), 'Bell & Ross', now()),
  (gen_random_uuid(), 'Nomos', now()),
  (gen_random_uuid(), 'Grand Seiko', now()),
  (gen_random_uuid(), 'Seiko', now()),
  (gen_random_uuid(), 'Casio', now()),
  (gen_random_uuid(), 'G-Shock', now()),
  (gen_random_uuid(), 'Hermès', now()),
  (gen_random_uuid(), 'Chanel', now()),
  (gen_random_uuid(), 'Franck Muller', now()),
  (gen_random_uuid(), 'Jacob & Co', now()),
  (gen_random_uuid(), 'MB&F', now()),
  (gen_random_uuid(), 'H. Moser & Cie', now()),
  (gen_random_uuid(), 'Laurent Ferrier', now()),
  (gen_random_uuid(), 'De Bethune', now()),
  (gen_random_uuid(), 'Greubel Forsey', now()),
  (gen_random_uuid(), 'Oris', now()),
  (gen_random_uuid(), 'Sinn', now()),
  (gen_random_uuid(), 'Junghans', now()),
  (gen_random_uuid(), 'Montblanc', now()),
  (gen_random_uuid(), 'Baume & Mercier', now()),
  (gen_random_uuid(), 'Corum', now()),
  (gen_random_uuid(), 'Glashutte Original', now()),
  (gen_random_uuid(), 'Jaquet Droz', now()),
  (gen_random_uuid(), 'Parmigiani Fleurier', now()),
  (gen_random_uuid(), 'Czapek', now()),
  (gen_random_uuid(), 'Moser', now());

-- =========================================================================
-- Units (pricing / measurement)
-- =========================================================================
INSERT INTO units (id, name, abbreviation, created_at) VALUES
  (gen_random_uuid(), 'US Dollar', 'USD', now()),
  (gen_random_uuid(), 'Hong Kong Dollar', 'HKD', now()),
  (gen_random_uuid(), 'Euro', 'EUR', now()),
  (gen_random_uuid(), 'British Pound', 'GBP', now()),
  (gen_random_uuid(), 'Swiss Franc', 'CHF', now()),
  (gen_random_uuid(), 'Japanese Yen', 'JPY', now()),
  (gen_random_uuid(), 'Singapore Dollar', 'SGD', now()),
  (gen_random_uuid(), 'Chinese Yuan', 'CNY', now()),
  (gen_random_uuid(), 'Australian Dollar', 'AUD', now()),
  (gen_random_uuid(), 'Canadian Dollar', 'CAD', now()),
  (gen_random_uuid(), 'Malaysian Ringgit', 'MYR', now()),
  (gen_random_uuid(), 'Thai Baht', 'THB', now()),
  (gen_random_uuid(), 'UAE Dirham', 'AED', now()),
  (gen_random_uuid(), 'Millimeters', 'mm', now()),
  (gen_random_uuid(), 'Piece', 'pc', now());

-- =========================================================================
-- Conditions (watch condition grading)
-- =========================================================================
INSERT INTO conditions (id, name, created_at) VALUES
  (gen_random_uuid(), 'BNIB (Brand New In Box)', now()),
  (gen_random_uuid(), 'New / Unworn', now()),
  (gen_random_uuid(), 'Like New', now()),
  (gen_random_uuid(), 'Excellent', now()),
  (gen_random_uuid(), 'Very Good', now()),
  (gen_random_uuid(), 'Good', now()),
  (gen_random_uuid(), 'Fair', now()),
  (gen_random_uuid(), 'Poor', now()),
  (gen_random_uuid(), 'NOS (New Old Stock)', now()),
  (gen_random_uuid(), 'Watch Only', now()),
  (gen_random_uuid(), 'Full Set', now()),
  (gen_random_uuid(), 'Double Box', now()),
  (gen_random_uuid(), 'No Box / No Papers', now()),
  (gen_random_uuid(), 'Box Only', now()),
  (gen_random_uuid(), 'Papers Only', now()),
  (gen_random_uuid(), 'Service Required', now()),
  (gen_random_uuid(), 'Recently Serviced', now()),
  (gen_random_uuid(), 'Polished', now()),
  (gen_random_uuid(), 'Unpolished', now()),
  (gen_random_uuid(), 'Aftermarket Parts', now());
