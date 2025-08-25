package raff.stein.document.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import raff.stein.document.event.producer.FileValidatedEventPublisher;
import raff.stein.document.model.DocumentType;
import raff.stein.document.model.File;
import raff.stein.document.model.entity.mapper.FileValidationResultToFileValidationEntity;
import raff.stein.document.repository.FileValidationRepository;
import raff.stein.document.service.validation.FileValidationResult;
import raff.stein.document.service.validation.FileValidatorProvider;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileValidationService {

    private final FileValidatedEventPublisher fileValidatedEventPublisher;
    private final FileValidationRepository fileValidationRepository;

    private static final FileValidationResultToFileValidationEntity fileValidationResultToFileValidationEntity =
            FileValidationResultToFileValidationEntity.MAPPER;

    // for validation only, without publishing an event
    public Boolean validate(File fileInput, DocumentType documentType) {
        // Validate the file against the document type configuration
        FileValidationResult fileValidationResult = getUploadedFileValidationResult(fileInput, documentType);
        return fileValidationResult.isValid();
    }

    // validate the file and publish an event to notify other services about the validation result
    public Boolean validateUploadedFileAndPublish(File fileInput, DocumentType documentType) {
        FileValidationResult fileValidationResult = getUploadedFileValidationResult(fileInput, documentType);
        // Save the validation result to the database
        fileValidationRepository.save(
                fileValidationResultToFileValidationEntity.toFileValidationEntity(fileValidationResult,fileInput));
        // publish an event to notify other services about the validation result
        fileValidatedEventPublisher.publishDocumentValidatedEvent(fileInput, fileValidationResult);
        return fileValidationResult.isValid();
    }

    private static FileValidationResult getUploadedFileValidationResult(File fileInput, DocumentType documentType) {
        FileValidationResult fileValidationResult = FileValidatorProvider.getUploadFileValidator().validate(
                fileInput,
                new FileValidationResult(),
                documentType);
        fileValidationResult.logErrors();
        fileValidationResult.finalizeValidation();
        return fileValidationResult;
    }
}
