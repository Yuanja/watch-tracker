-- =====================================================================
-- V002__seed_data.sql
-- Initial seed data for normalized value tables
-- =====================================================================

-- =====================================================================
-- CATEGORIES
-- =====================================================================
INSERT INTO categories (name, sort_order) VALUES
    ('Pipe Fittings',     1),
    ('Valves',            2),
    ('Electrical',        3),
    ('Instrumentation',   4),
    ('Pumps',             5),
    ('Motors',            6),
    ('Heat Exchangers',   7);

-- =====================================================================
-- MANUFACTURERS
-- =====================================================================
INSERT INTO manufacturers (name, aliases) VALUES
    ('Parker Hannifin',  ARRAY['Parker', 'PH']),
    ('Swagelok',         ARRAY['Swage']),
    ('Emerson',          ARRAY['Emerson Electric', 'Emerson Process']),
    ('Honeywell',        ARRAY['HON']),
    ('Siemens',          ARRAY['Siemens AG']);

-- =====================================================================
-- UNITS
-- =====================================================================
INSERT INTO units (name, abbreviation) VALUES
    ('each',    'ea'),
    ('feet',    'ft'),
    ('lot',     'lot'),
    ('pounds',  'lbs'),
    ('meters',  'm'),
    ('inches',  'in');

-- =====================================================================
-- CONDITIONS
-- =====================================================================
INSERT INTO conditions (name, abbreviation, sort_order) VALUES
    ('New',            NULL,   1),
    ('Used',           NULL,   2),
    ('Surplus',        NULL,   3),
    ('New Old Stock',  'NOS',  4),
    ('Refurbished',    NULL,   5);
