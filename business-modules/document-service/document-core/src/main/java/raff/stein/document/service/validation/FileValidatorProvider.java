package raff.stein.document.service.validation;

import raff.stein.document.service.validation.validators.FileBaseValidator;
import raff.stein.document.service.validation.validators.FileExtensionValidator;
import raff.stein.document.service.validation.validators.FileOperationValidator;
import raff.stein.document.service.validation.validators.FileSizeValidator;

public class FileValidatorProvider {

    private FileValidatorProvider() {
        // to prevent instantiation
    }

    public static FileValidator getUploadFileValidator() {
        // Create a chain of validators for file upload validation
        // Upload validations are: basic file checks, size, and extension validation
        return new FileBaseValidator()
                .linkWith(new FileSizeValidator())
                .linkWith(new FileExtensionValidator())
                .linkWith(new FileOperationValidator("upload"));
    }
}
