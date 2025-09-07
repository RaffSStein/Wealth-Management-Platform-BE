# Document Service

## Overview
The Document Service is a core microservice of the Wealth Management Platform responsible for managing documents and files
related to customers, products, and business processes. It provides secure storage, retrieval, and metadata management,
supporting multiple storage backends and event-driven integration with other services.

---

## Setup

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker (for local development)
- Access to configured cloud storage (AWS S3, Google Cloud Storage, Azure Blob, etc.)

### Profiles
The service supports multiple profiles for different storage providers:
- `local`: Stores files on the local filesystem.
- `aws`: Uses AWS S3 for storage.
- `gcs`: Uses Google Cloud Storage.

Configure the active profile via:
```
-Dspring.profiles.active=<profile>
```
and set the required credentials in `application.yaml` or environment variables.

---

## Data Model
The main entities managed by the service include:
- **DocumentEntity**: Represents a document, with metadata, type, and access logs.
- **DocumentVersionEntity**: Tracks versions of a document.
- **DocumentAccessLogEntity**: Logs access and actions performed on documents.
- **DocumentTypeEntity**: Defines document types and allowed operations.
- **DocumentMetadataEntity**: Stores custom metadata as key-value pairs.

Refer to the domain documentation for detailed UML diagrams and entity relationships.

---

## Capabilities & APIs

### Main Capabilities
- **Upload Document**: Accepts file uploads, stores them, and creates metadata and version records.
- **Download Document**: Provides secure download of documents by UUID.
- **Versioning**: Manages multiple versions of a document.
- **Access Logging**: Tracks all access and actions on documents.
- **Metadata Management**: Allows custom metadata for each document.

### Main APIs
- `POST /document/upload` — Upload a new document
- `GET /document/downloadDocument/{uuid}` — Download a document by UUID
- `GET /document/{uuid}/metadata` — Get document metadata
- `GET /document/{uuid}/versions` — List all versions of a document
- `POST /document/{uuid}/metadata` — Add or update metadata

Refer to the OpenAPI specification in `document-api-data/document-api-data.yaml` for full details.

---

## Topics & Events

The service is event-driven and interacts with the following Kafka topics:
- `document-uploaded` — Published when a document is uploaded
- `file-validated` — Published the validation result of a file before upload


Event payloads follow the schemas defined in `document-event-data`.

---

## Key Processes

### Document Upload
1. The client calls `POST /document/uploadDocument` with a file and metadata.
2. The service validates the file, stores it in the configured backend, and creates a new DocumentEntity and DocumentVersionEntity.
3. An event is published to `document-uploaded`.

### Document Download
1. The client calls `GET /document/downloadDocument/{uuid}`.
2. The service retrieves the document, checks permissions, and streams the file.
3. 

### Versioning
- Each upload of a new file for an existing document creates a new DocumentVersionEntity and publishes an event.

---

## Miscellaneous
- All API responses follow standard error handling and include correlation IDs for traceability.
- The service supports audit logging and integrates with Splunk for observability.
- Security is enforced via JWT authentication and role-based access control.

---

## Work in Progress
This service is under active development. Features and APIs may change. For contributions, refer to the main repository README and module-specific documentation.

