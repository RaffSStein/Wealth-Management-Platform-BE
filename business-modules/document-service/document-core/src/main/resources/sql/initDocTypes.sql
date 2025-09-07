INSERT INTO document_type (type_name, description, max_file_size, allowed_mime_types, allowed_operations, created_by, creation_date, last_modified_by, last_modified_date, version)
VALUES (
    'IDENTITY_CARD',
    'Identity card document type',
    5242880, -- 5 MB
    'application/pdf,image/jpeg,image/png',
    'upload,view,download,delete,edit',
    'system',
    NOW(),
    NULL,
    NULL,
    0
);

INSERT INTO document_type (type_name, description, max_file_size, allowed_mime_types, allowed_operations, created_by, creation_date, last_modified_by, last_modified_date, version)
VALUES (
    'BANK_STATEMENT',
    'Bank statement document type',
    10485760, -- 10 MB
    'application/pdf',
    'view,download,delete',
    'system',
    NOW(),
    NULL,
    NULL,
    0
);
