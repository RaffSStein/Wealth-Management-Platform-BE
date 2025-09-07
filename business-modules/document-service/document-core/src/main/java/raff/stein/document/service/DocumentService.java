package raff.stein.document.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.document.event.producer.DocumentUploadedEventPublisher;
import raff.stein.document.exception.FileValidationException;
import raff.stein.document.model.Document;
import raff.stein.document.model.File;
import raff.stein.document.model.entity.*;
import raff.stein.document.model.entity.mapper.*;
import raff.stein.document.repository.DocumentAccessLogRepository;
import raff.stein.document.repository.DocumentMetadataRepository;
import raff.stein.document.repository.DocumentRepository;
import raff.stein.document.repository.DocumentVersionRepository;
import raff.stein.document.service.storage.CloudStorageService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentTypeService documentTypeService;
    private final FileValidationService fileValidationService;
    private final CloudStorageService cloudStorageService;

    private final DocumentUploadedEventPublisher documentUploadedEventPublisher;

    private final DocumentRepository documentRepository;
    private final DocumentAccessLogRepository documentAccessLogRepository;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final DocumentVersionRepository documentVersionRepository;

    private static final DocumentEntityToDocumentMapper documentEntityToDocumentMapper = DocumentEntityToDocumentMapper.MAPPER;
    private static final DocumentVersionEntityToDocumentMapper documentVersionEntityToDocumentMapper = DocumentVersionEntityToDocumentMapper.MAPPER;
    private static final DocumentMetadataEntityToMetadataMapper documentMetadataEntityToMetadataMapper = DocumentMetadataEntityToMetadataMapper.MAPPER;
    private static final DocumentAccessLogEntityToDocumentMapper documentAccessLogEntityToDocumentMapper = DocumentAccessLogEntityToDocumentMapper.MAPPER;
    private static final DocumentTypeEntityToDocumentTypeMapper documentTypeEntityToDocumentTypeMapper = DocumentTypeEntityToDocumentTypeMapper.MAPPER;

    public Document uploadDocument(File fileInput) {
        // get the document type (configuration) from the input file
        DocumentTypeEntity documentTypeEntity = documentTypeService.getDocumentTypeEntity(fileInput.getDocumentType());
        // validate the file against the document type configuration
        // publish an event to notify other services about the validation result
        Boolean isValid = fileValidationService.validateUploadedFileAndPublish(
                fileInput,
                documentTypeEntityToDocumentTypeMapper.toDocumentType(documentTypeEntity));
        if(Boolean.TRUE.equals(isValid)) {
            // upload file to storage (e.g., S3, local file system, etc.)
            final Document uploadedDocument = cloudStorageService.uploadFile(fileInput);
            // save document and related metadata to the database
            final DocumentEntity savedDocument = buildDocumentEntityAndRelatedEntities(uploadedDocument, documentTypeEntity);
            // publish an event to notify other services for upload and validation
            final Document uploadedDocumentWithMetadata = documentEntityToDocumentMapper.toDocument(savedDocument);
            documentUploadedEventPublisher.publishDocumentUploadedEvent(uploadedDocumentWithMetadata);
            // return the uploaded document with metadata
            return uploadedDocumentWithMetadata;
        } else {
            throw FileValidationException.with(fileInput.getCustomerId().toString()).get();
        }
    }

    @Transactional(readOnly = true)
    public Document downloadDocument(UUID documentId) {
        //TODO: add security checks to ensure the user has access to the document
        // find the document by ID
        DocumentEntity documentEntity = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));
        // convert the DocumentEntity to Document
        Document document = documentEntityToDocumentMapper.toDocument(documentEntity);
        String fileContentBase64 = cloudStorageService.downloadFile(document.getFullFilePath());
        document.setFileContentBase64(fileContentBase64);
        return document;
    }

    private DocumentEntity buildDocumentEntityAndRelatedEntities(Document document, DocumentTypeEntity documentTypeEntity) {
        // create a new DocumentVersionEntity from the Document
        DocumentVersionEntity documentVersionEntity = documentVersionEntityToDocumentMapper.toDocumentVersionEntity(document);
        // create a new DocumentMetadataEntity from the Document
        List<DocumentMetadataEntity> documentMetadataEntities = document.getMetadata().stream()
                .map(documentMetadataEntityToMetadataMapper::toDocumentMetadataEntity)
                .toList();
        // create a new DocumentAccessLogEntity from the Document
        DocumentAccessLogEntity documentAccessLogEntity = documentAccessLogEntityToDocumentMapper.toUploadAction(document);
        // create a new DocumentEntity from the Document
        DocumentEntity documentEntity = documentEntityToDocumentMapper.toDocumentEntity(document);
        documentEntity.setDocumentType(documentTypeEntity);
        DocumentEntity savedDocumentEntity = documentRepository.save(documentEntity);
        documentMetadataEntities.forEach(e -> e.setDocument(savedDocumentEntity));
        documentMetadataRepository.saveAll(documentMetadataEntities);
        documentVersionEntity.setDocument(savedDocumentEntity);
        documentVersionRepository.save(documentVersionEntity);
        documentAccessLogEntity.setDocument(savedDocumentEntity);
        documentAccessLogRepository.save(documentAccessLogEntity);
        savedDocumentEntity.setVersions(List.of(documentVersionEntity));
        savedDocumentEntity.setAccessLogs(List.of(documentAccessLogEntity));
        savedDocumentEntity.setMetadata(documentMetadataEntities);
        return savedDocumentEntity;
    }


}
