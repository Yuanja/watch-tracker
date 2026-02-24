-- H2 type aliases so that Hibernate's PostgreSQL-specific columnDefinition
-- overrides (vector, jsonb, text[], uuid[]) do not cause DDL failures when
-- ddl-auto: create-drop generates the schema against H2.
CREATE DOMAIN IF NOT EXISTS VECTOR AS CLOB;
CREATE DOMAIN IF NOT EXISTS JSONB  AS CLOB;
