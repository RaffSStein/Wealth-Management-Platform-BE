package raff.stein.document.service.validation.validators;

import raff.stein.document.model.DocumentType;
import raff.stein.document.model.File;
import raff.stein.document.service.validation.FileValidationResult;
import raff.stein.document.service.validation.FileValidator;

public class FileBaseValidator extends FileValidator {

    @Override
    protected void doValidate(File file, FileValidationResult result, DocumentType configuration) {
        // Base validation checks for file and configuration
        // File checks
        if (file == null) {
            result.addError(FileBaseValidator.class.getName(), "File object was null");
        } else {
            if(file.getMultipartFile() == null || file.getMultipartFile().isEmpty()) {
                result.addError(FileBaseValidator.class.getName(), "MultipartFile was null or empty");
            }
        }
        // configuration checks
        if (configuration == null) {
            result.addError(FileBaseValidator.class.getName(), "Document type configuration was null");
        } else {
            fileSizeNullCheck(result, configuration);
            allowedMimeTypesNullCheck(result, configuration);
            allowedOperationsNullCheck(result, configuration);
        }
    }

    private static void allowedOperationsNullCheck(FileValidationResult result, DocumentType configuration) {
        if (configuration.getAllowedOperations() == null || configuration.getAllowedOperations().isEmpty()) {
            result.addError(
                    FileBaseValidator.class.getName(),
                    "Document type configuration allowed operations was null or empty");
        }
    }

    private static void allowedMimeTypesNullCheck(FileValidationResult result, DocumentType configuration) {
        if (configuration.getAllowedMimeTypes() == null || configuration.getAllowedMimeTypes().isEmpty()) {
            result.addError(
                    FileBaseValidator.class.getName(),
                    "Document type configuration allowed MIME types was null or empty");
        }
    }

    private static void fileSizeNullCheck(FileValidationResult result, DocumentType configuration) {
        if (configuration.getMaxFileSize() == null || configuration.getMaxFileSize() <= 0) {
            result.addError(
                    FileBaseValidator.class.getName(),
                    "Document type configuration max file size must be a non null digit greater than zero");
        }
    }
}
