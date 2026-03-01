-- Populate condition abbreviation column (match what LLM commonly outputs)
UPDATE conditions SET abbreviation = 'BNIB'      WHERE name = 'BNIB (Brand New In Box)';
UPDATE conditions SET abbreviation = 'NOS'       WHERE name = 'NOS (New Old Stock)';
UPDATE conditions SET abbreviation = 'new'       WHERE name = 'New / Unworn';
UPDATE conditions SET abbreviation = 'pre-owned' WHERE name = 'Pre-Owned';
UPDATE conditions SET abbreviation = 'like new'  WHERE name = 'Like New';

-- Manufacturer alias mappings
UPDATE manufacturers SET aliases = ARRAY['AP']                  WHERE name = 'Audemars Piguet';
UPDATE manufacturers SET aliases = ARRAY['PP', 'Patek']         WHERE name = 'Patek Philippe';
UPDATE manufacturers SET aliases = ARRAY['VC']                  WHERE name = 'Vacheron Constantin';
UPDATE manufacturers SET aliases = ARRAY['JLC', 'Jaeger']       WHERE name = 'Jaeger-LeCoultre';
UPDATE manufacturers SET aliases = ARRAY['IWC Schaffhausen']    WHERE name = 'IWC';
UPDATE manufacturers SET aliases = ARRAY['Tag', 'TAG']          WHERE name = 'TAG Heuer';
UPDATE manufacturers SET aliases = ARRAY['Lange', 'ALS']        WHERE name = 'A. Lange & Sohne';
UPDATE manufacturers SET aliases = ARRAY['RM']                  WHERE name = 'Richard Mille';
