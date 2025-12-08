# Document Service

## Overview
The Document Service is a core microservice of the Wealth Management Platform responsible for managing documents and files
related to customers, products, and business processes. It provides secure storage, retrieval, and metadata management,
supporting multiple storage backends and event-driven integration with other services.


## Setup
- Use project standard Maven build (from repository root):
  ```powershell
  mvn clean install
  ```
- To run the service (from `document-core` directory):
  ```powershell
    mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.application.name=document-service"


### Profiles
The service supports multiple profiles for different storage providers:
- `local`: Stores files on the local filesystem.
- `aws`: Uses (dockerized) AWS S3 for storage.
- `gcs`: Uses (dockerized) Google Cloud Storage.

Configure the active profile via:
```
-Dspring.profiles.active=<profile>
```
and set the required credentials in `application.yaml` or environment variables.
Eg:
  ```powershell
    mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.application.name=document-service -Dspring.profiles.active=aws"
  ```


## Capabilities & APIs

### Main Capabilities
- **Upload Document**: Accepts file uploads, stores them, and creates metadata and version records.
- **Download Document**: Provides secure download of documents by UUID.
- **Versioning**: Manages multiple versions of a document.
- **Access Logging**: Tracks all access and actions on documents.
- **Metadata Management**: Allows custom metadata for each document.

## Topics & Events

The service is event-driven and interacts with the following Kafka topics:
- `document-uploaded` — Published when a document is uploaded
- `file-validated` — Published the validation result of a file before upload


Event payloads follow the schemas defined in `document-event-data`.
