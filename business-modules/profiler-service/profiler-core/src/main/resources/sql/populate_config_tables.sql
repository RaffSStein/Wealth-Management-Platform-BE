-- Permission catalogue and role permissions.
-- Sections and features mirror the FE navigation (Wealth-Management-Platform-FE src/app/layout/navigation.ts):
-- a section is a top-bar section, a feature a side-nav item. role_feature_permission says what each role may do;
-- users get the permissions of the role in their token, computed on read (nothing is stored per user).
-- Run after profiler-service has started once (Hibernate creates the tables). Idempotent: safe to run again.
-- Changes apply immediately, no restart needed. Docs: docs/domain/process/security/roles-and-permissions.md

-- Permission codes
INSERT INTO permissions (permission_code, permission_name) VALUES
  ('VIEW', 'VIEW_PERMISSION'),
  ('USE', 'USE_PERMISSION'),
  ('EDIT', 'EDIT_PERMISSION'),
  ('DOWNLOAD', 'DOWNLOAD_PERMISSION')
ON CONFLICT (permission_code) DO NOTHING;

-- Sections (FE top-bar sections, in navigation order); application WMP.
-- display_order: position in the top bar; re-running updates order and names of existing rows.
INSERT INTO sections (section_code, section_name, application, display_order)
SELECT v.section_code, v.section_name, 'WMP', v.display_order
FROM (VALUES
  ('ACCOUNT', 'Account', 1),
  ('INVESTMENTS', 'Investments', 2),
  ('NEWS_MARKETS', 'Markets', 3),
  ('WORKSPACE', 'Workspace', 4)
) AS v(section_code, section_name, display_order)
ON CONFLICT (section_code) DO UPDATE
  SET section_name = EXCLUDED.section_name, display_order = EXCLUDED.display_order;

-- Features (FE side-nav items), unique per section; display_order: position in the section's side nav
INSERT INTO features (feature_code, feature_name, section_id, display_order)
SELECT v.feature_code, v.feature_name, s.id, v.display_order
FROM (VALUES
  -- Account (/app/home, /app/investor-profile, ...)
  ('ACCOUNT', 'HOME', 'Home', 1),
  ('ACCOUNT', 'INVESTOR_PROFILE', 'Investor profile', 2),
  ('ACCOUNT', 'TRANSACTIONS', 'Transactions', 3),
  ('ACCOUNT', 'ASSETS', 'Assets', 4),
  ('ACCOUNT', 'BANK_TRANSFERS', 'Bank transfers', 5),
  ('ACCOUNT', 'TAX_CREDITS', 'Tax credits', 6),
  ('ACCOUNT', 'SECURITIES_TRANSFER', 'Securities transfer', 7),
  -- Investments (/app/investments/...)
  ('INVESTMENTS', 'OVERVIEW', 'Overview', 1),
  ('INVESTMENTS', 'POSITIONS', 'Positions', 2),
  ('INVESTMENTS', 'ORDERS', 'Orders', 3),
  ('INVESTMENTS', 'PERFORMANCE', 'Performance', 4),
  ('INVESTMENTS', 'ALLOCATION', 'Allocation', 5),
  -- Markets (/app/news-markets/...)
  ('NEWS_MARKETS', 'OVERVIEW', 'Overview', 1),
  ('NEWS_MARKETS', 'MARKET_NEWS', 'Market news', 2),
  ('NEWS_MARKETS', 'INDICES', 'Indices', 3),
  ('NEWS_MARKETS', 'CURRENCIES', 'Currencies', 4),
  ('NEWS_MARKETS', 'COMMODITIES', 'Commodities', 5),
  -- Advisor workspace (/app/advisor/...)
  ('WORKSPACE', 'OVERVIEW', 'Overview', 1),
  ('WORKSPACE', 'CLIENTS', 'Clients', 2),
  ('WORKSPACE', 'MIFID_REVIEWS', 'MiFID reviews', 3),
  ('WORKSPACE', 'PROPOSALS', 'Proposals', 4),
  ('WORKSPACE', 'ORDERS', 'Orders', 5)
) AS v(section_code, feature_code, feature_name, display_order)
JOIN sections s ON s.section_code = v.section_code
ON CONFLICT ON CONSTRAINT uk_feature_section_code DO UPDATE
  SET feature_name = EXCLUDED.feature_name, display_order = EXCLUDED.display_order;

-- Role permissions: (role, section, feature, permission code)
CREATE TEMP TABLE seed_role_grants (role varchar(30), section_code varchar(255), feature_code varchar(255), permission_code varchar(255));

-- CUSTOMER, and ADVISOR too: an advisor can act on behalf of a client and then sees the client sections
INSERT INTO seed_role_grants
SELECT r.role, g.section_code, g.feature_code, g.permission_code
FROM (VALUES ('CUSTOMER'), ('ADVISOR')) AS r(role)
CROSS JOIN (VALUES
  ('ACCOUNT', 'HOME', 'VIEW'),
  ('ACCOUNT', 'INVESTOR_PROFILE', 'VIEW'),
  ('ACCOUNT', 'INVESTOR_PROFILE', 'EDIT'),
  ('ACCOUNT', 'TRANSACTIONS', 'VIEW'),
  ('ACCOUNT', 'ASSETS', 'VIEW'),
  ('ACCOUNT', 'BANK_TRANSFERS', 'VIEW'),
  ('ACCOUNT', 'BANK_TRANSFERS', 'USE'),
  ('ACCOUNT', 'TAX_CREDITS', 'VIEW'),
  ('ACCOUNT', 'SECURITIES_TRANSFER', 'VIEW'),
  ('ACCOUNT', 'SECURITIES_TRANSFER', 'USE'),
  ('INVESTMENTS', 'OVERVIEW', 'VIEW'),
  ('INVESTMENTS', 'POSITIONS', 'VIEW'),
  ('INVESTMENTS', 'ORDERS', 'VIEW'),
  ('INVESTMENTS', 'ORDERS', 'USE'),
  ('INVESTMENTS', 'PERFORMANCE', 'VIEW'),
  ('INVESTMENTS', 'ALLOCATION', 'VIEW'),
  ('NEWS_MARKETS', 'OVERVIEW', 'VIEW'),
  ('NEWS_MARKETS', 'MARKET_NEWS', 'VIEW'),
  ('NEWS_MARKETS', 'INDICES', 'VIEW'),
  ('NEWS_MARKETS', 'CURRENCIES', 'VIEW'),
  ('NEWS_MARKETS', 'COMMODITIES', 'VIEW')
) AS g(section_code, feature_code, permission_code);

-- ADVISOR workspace
INSERT INTO seed_role_grants VALUES
  ('ADVISOR', 'WORKSPACE', 'OVERVIEW', 'VIEW'),
  ('ADVISOR', 'WORKSPACE', 'CLIENTS', 'VIEW'),
  ('ADVISOR', 'WORKSPACE', 'CLIENTS', 'USE'),
  ('ADVISOR', 'WORKSPACE', 'MIFID_REVIEWS', 'VIEW'),
  ('ADVISOR', 'WORKSPACE', 'MIFID_REVIEWS', 'EDIT'),
  ('ADVISOR', 'WORKSPACE', 'PROPOSALS', 'VIEW'),
  ('ADVISOR', 'WORKSPACE', 'PROPOSALS', 'EDIT'),
  ('ADVISOR', 'WORKSPACE', 'ORDERS', 'VIEW'),
  ('ADVISOR', 'WORKSPACE', 'ORDERS', 'USE');

-- BANK_ADMIN, PLATFORM_ADMIN: no feature permissions yet (their rights come from the role in the token)

-- a grant naming an unknown section, feature or permission code fails the script instead of being silently skipped
DO $$
DECLARE unknown text;
BEGIN
  SELECT string_agg(g.role || ' ' || g.section_code || '/' || g.feature_code || ' ' || g.permission_code, ', ')
  INTO unknown
  FROM seed_role_grants g
  LEFT JOIN sections s ON s.section_code = g.section_code
  LEFT JOIN features f ON f.section_id = s.id AND f.feature_code = g.feature_code
  LEFT JOIN permissions p ON p.permission_code = g.permission_code
  WHERE f.id IS NULL OR p.id IS NULL;
  IF unknown IS NOT NULL THEN
    RAISE EXCEPTION 'Role grants reference unknown catalogue entries: %', unknown;
  END IF;
END $$;

INSERT INTO role_feature_permission (role, feature_id, permission_id)
SELECT g.role, f.id, p.id
FROM seed_role_grants g
JOIN sections s ON s.section_code = g.section_code
JOIN features f ON f.section_id = s.id AND f.feature_code = g.feature_code
JOIN permissions p ON p.permission_code = g.permission_code
ON CONFLICT ON CONSTRAINT uk_role_feature_permission DO NOTHING;

DROP TABLE seed_role_grants;
