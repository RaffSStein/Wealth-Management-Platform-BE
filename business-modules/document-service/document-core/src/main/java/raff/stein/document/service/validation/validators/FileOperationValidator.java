package raff.stein.document.service.validation.validators;

import raff.stein.document.model.DocumentType;
import raff.stein.document.model.File;
import raff.stein.document.service.validation.FileValidationResult;
import raff.stein.document.service.validation.FileValidator;

public class FileOperationValidator extends FileValidator {

    private final String operation;

    public FileOperationValidator(String operation) {
        super();
        this.operation = operation;
    }

    @Override
    protected void doValidate(File file, FileValidationResult result, DocumentType configuration) {
        // Validate the operation against the allowed operations in the document type configuration
        boolean isNotValidOperation = configuration.getAllowedOperations()
                .stream()
                .noneMatch(op -> op.equalsIgnoreCase(operation));
        if (isNotValidOperation) {
            result.addError(
                    FileOperationValidator.class.getName(),
                    String.format("Operation '%s' is not allowed for document type '%s'",
                            operation,
                            configuration.getTypeName()));
        }
    }
}
