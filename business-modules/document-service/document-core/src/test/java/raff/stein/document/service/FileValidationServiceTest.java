package raff.stein.document.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.document.event.producer.FileValidatedEventPublisher;
import raff.stein.document.model.DocumentType;
import raff.stein.document.model.File;
import raff.stein.document.model.entity.FileValidationEntity;
import raff.stein.document.repository.FileValidationRepository;
import raff.stein.document.service.validation.FileValidationResult;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class FileValidationServiceTest {

    private FileValidatedEventPublisher fileValidatedEventPublisher;
    private FileValidationRepository fileValidationRepository;
    private FileValidationService service;

    @BeforeEach
    void setUp() {
        fileValidatedEventPublisher = mock(FileValidatedEventPublisher.class);
        fileValidationRepository = mock(FileValidationRepository.class);
        service = new FileValidationService(fileValidatedEventPublisher, fileValidationRepository);
    }

    @Test
    void shouldStoreTheResultAndThePublishedEventForAValidFile() {
        final File file = fileWithMimeType("application/pdf");

        assertTrue(service.validateUploadedFileAndPublish(file, documentType()));

        final var inOrder = inOrder(fileValidationRepository, fileValidatedEventPublisher);
        inOrder.verify(fileValidationRepository).save(any(FileValidationEntity.class));
        inOrder.verify(fileValidatedEventPublisher).publishDocumentValidatedEvent(any(File.class), any(FileValidationResult.class));
    }

    @Test
    void shouldStillReportAnInvalidFile() {
        //TODO: FileValidator.linkWith overwrites "next", so only the last validator runs after the base check; fix it and use a bad MIME type here
        final File file = fileWithMimeType("application/pdf");
        final DocumentType downloadOnly = documentType();
        downloadOnly.setAllowedOperations(Set.of("download"));

        assertFalse(service.validateUploadedFileAndPublish(file, downloadOnly));

        // the caller rejects the upload, but the result and the event are kept
        final var inOrder = inOrder(fileValidationRepository, fileValidatedEventPublisher);
        inOrder.verify(fileValidationRepository).save(any(FileValidationEntity.class));
        inOrder.verify(fileValidatedEventPublisher).publishDocumentValidatedEvent(any(File.class), any(FileValidationResult.class));
    }

    @Test
    void shouldRunInATransactionSoTheEventIsStoredWithTheResult() throws NoSuchMethodException {
        // the outbox row is written by the publisher call: without the transaction it would not be atomic with the result
        assertNotNull(FileValidationService.class
                .getMethod("validateUploadedFileAndPublish", File.class, DocumentType.class)
                .getAnnotation(Transactional.class));
    }

    private static File fileWithMimeType(final String mimeType) {
        return File.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .documentType("ID_CARD")
                .multipartFile(new MockMultipartFile("file", "id.pdf", mimeType, new byte[]{1, 2, 3}))
                .build();
    }

    private static DocumentType documentType() {
        return DocumentType.builder()
                .id(1)
                .typeName("ID_CARD")
                .maxFileSize(1024L)
                .allowedMimeTypes(Set.of("application/pdf"))
                .allowedOperations(Set.of("upload"))
                .build();
    }
}
