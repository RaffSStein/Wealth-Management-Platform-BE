-- Migration for EXISTING profiler databases (fresh ones only need populate_config_tables.sql).
-- Run by hand until the platform adopts Flyway/Liquibase, in this order:
--   1. this script:   docker compose exec -T postgres-profiler psql -U test -d profilerdb -v ON_ERROR_STOP=1 -f - < this-file
--   2. start profiler-service on the new build once (Hibernate creates role_feature_permission)
--   3. the seed:      ... -f - < ../populate_config_tables.sql
--
-- Replaces the old sample catalogue (application WMP_B2C, lowercase feature codes) with the one mirroring the FE, and
-- drops the per-user permission copy: permissions are now computed from role_feature_permission on each request.

BEGIN;

-- per-user permission rows (replaced by role_feature_permission) and the short-lived re-sync state
DROP TABLE IF EXISTS user_feature_permission;
DROP TABLE IF EXISTS permission_catalogue_state;

-- features and sections that are not part of the new catalogue
DELETE FROM features
WHERE section_id IN (
  SELECT id FROM sections WHERE section_code NOT IN ('ACCOUNT', 'INVESTMENTS', 'NEWS_MARKETS', 'WORKSPACE')
);
DELETE FROM sections
WHERE section_code NOT IN ('ACCOUNT', 'INVESTMENTS', 'NEWS_MARKETS', 'WORKSPACE');

COMMIT;
