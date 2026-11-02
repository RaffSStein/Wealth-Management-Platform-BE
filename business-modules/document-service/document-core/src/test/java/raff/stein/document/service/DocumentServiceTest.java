package raff.stein.document.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import raff.stein.document.event.producer.DocumentUploadedEventPublisher;
import raff.stein.document.exception.FileValidationException;
import raff.stein.document.model.Document;
import raff.stein.document.model.DocumentType;
import raff.stein.document.model.File;
import raff.stein.document.model.Metadata;
import raff.stein.document.model.entity.DocumentEntity;
import raff.stein.document.model.entity.DocumentTypeEntity;
import raff.stein.document.repository.DocumentAccessLogRepository;
import raff.stein.document.repository.DocumentMetadataRepository;
import raff.stein.document.repository.DocumentRepository;
import raff.stein.document.repository.DocumentVersionRepository;
import raff.stein.document.service.storage.CloudStorageService;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DocumentServiceTest {

    private DocumentTypeService documentTypeService;
    private FileValidationService fileValidationService;
    private CloudStorageService cloudStorageService;
    private DocumentUploadedEventPublisher documentUploadedEventPublisher;
    private DocumentRepository documentRepository;
    private PlatformTransactionManager transactionManager;
    private DocumentService service;
    private File file;

    @BeforeEach
    void setUp() {
        documentTypeService = mock(DocumentTypeService.class);
        fileValidationService = mock(FileValidationService.class);
        cloudStorageService = mock(CloudStorageService.class);
        documentUploadedEventPublisher = mock(DocumentUploadedEventPublisher.class);
        documentRepository = mock(DocumentRepository.class);
        transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new DocumentService(
                documentTypeService,
                fileValidationService,
                cloudStorageService,
                documentUploadedEventPublisher,
                new TransactionTemplate(transactionManager),
                documentRepository,
                mock(DocumentAccessLogRepository.class),
                mock(DocumentMetadataRepository.class),
                mock(DocumentVersionRepository.class));
        file = File.builder().id(UUID.randomUUID()).customerId(UUID.randomUUID()).documentType("ID_CARD").build();
        when(documentTypeService.getDocumentTypeEntity("ID_CARD"))
                .thenReturn(DocumentTypeEntity.builder().typeName("ID_CARD").build());
    }

    private void givenValidation(final boolean valid) {
        when(fileValidationService.validateUploadedFileAndPublish(any(File.class), any(DocumentType.class))).thenReturn(valid);
    }

    private void givenStorageReturnsADocument() {
        when(cloudStorageService.uploadFile(file)).thenReturn(Document.builder()
                .customerId(file.getCustomerId())
                .documentType("ID_CARD")
                .fileName("id.pdf")
                .mimeType("application/pdf")
                .fileSize(3L)
                .fullFilePath("customer-documents/id.pdf")
                .initialFileUuid(UUID.randomUUID())
                .uploadedBy("advisor@bank.test")
                .uploadDate(OffsetDateTime.now())
                .metadata(Set.of(Metadata.builder().key("k").value("v").build()))
                .build());
        when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void shouldStoreTheRowsAndTheEventInOneTransactionAfterTheUpload() {
        givenValidation(true);
        givenStorageReturnsADocument();

        service.uploadDocument(file);

        final InOrder inOrder = inOrder(cloudStorageService, transactionManager, documentRepository, documentUploadedEventPublisher);
        // the storage can't join the transaction, so it runs before it opens
        inOrder.verify(cloudStorageService).uploadFile(file);
        inOrder.verify(transactionManager).getTransaction(any());
        inOrder.verify(documentRepository).save(any(DocumentEntity.class));
        inOrder.verify(documentUploadedEventPublisher).publishDocumentUploadedEvent(any(Document.class));
        inOrder.verify(transactionManager).commit(any());
    }

    @Test
    void shouldRollBackTheRowsWhenTheEventCannotBeStored() {
        givenValidation(true);
        givenStorageReturnsADocument();
        doThrow(new IllegalStateException("outbox write failed"))
                .when(documentUploadedEventPublisher).publishDocumentUploadedEvent(any(Document.class));

        assertThrows(IllegalStateException.class, () -> service.uploadDocument(file));

        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void shouldNotUploadNorOpenATransactionWhenTheFileIsInvalid() {
        givenValidation(false);

        assertThrows(FileValidationException.class, () -> service.uploadDocument(file));

        verifyNoInteractions(cloudStorageService, documentUploadedEventPublisher);
        verify(transactionManager, never()).getTransaction(any());
    }
}
