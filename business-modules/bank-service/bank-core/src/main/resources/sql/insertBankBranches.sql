INSERT INTO bank_branches (
  id, bank_code, branch_code, bank_name, branch_name, swift_code, country_code, bank_type, status,
  bank_description, branch_description, branch_city, address, zip_code, phone_number, email,
  created_by, creation_date, last_modified_by, last_modified_date, version
) VALUES (
  gen_random_uuid(), 'B001', '0001', 'International Bank Corporation', 'Main Branch', 'INTLUS33', 'US', 'RETAIL', 'ACTIVE',
  'Primary US retail banking entity', 'Flagship branch in Manhattan', 'New York', '350 5th Ave, New York, NY', '10001',
  '+1-212-555-0100', 'mainbranch@intlbank.com',
  'system', NOW(), NULL, NULL, 0
);

INSERT INTO bank_branches (
  id, bank_code, branch_code, bank_name, branch_name, swift_code, country_code, bank_type, status,
  bank_description, branch_description, branch_city, address, zip_code, phone_number, email,
  created_by, creation_date, last_modified_by, last_modified_date, version
) VALUES (
  gen_random_uuid(), 'B002', '0105', 'Euro Capital Bank AG', 'Milan Central', 'EUROITMM', 'IT', 'CORPORATE', 'ACTIVE',
  'European corporate and investment bank', 'Corporate services branch', 'Milano', 'Via Monte Napoleone 12, Milano', '20121',
  '+39-02-5555-2200', 'milanocentral@eurocapital.com',
  'system', NOW(), NULL, NULL, 0
);
