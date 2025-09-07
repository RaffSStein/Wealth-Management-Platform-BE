package raff.stein.document.service.validation;

import lombok.extern.slf4j.Slf4j;
import raff.stein.document.model.DocumentType;
import raff.stein.document.model.File;
import raff.stein.document.service.validation.validators.FileBaseValidator;

@Slf4j
public abstract class FileValidator {

    private FileValidator next;

    public FileValidator linkWith(FileValidator next) {
        this.next = next;
        return this;
    }

    public FileValidationResult validate(
            File file,
            FileValidationResult result,
            DocumentType configuration) {
        doValidate(file, result, configuration);
        // if basic validation fails, do not continue with the next validators
        if (this instanceof FileBaseValidator && !result.isValid()) {
            return result;
        }
        if (next != null) {
            next.validate(file, result, configuration);
        }
        return result;
    }

    protected abstract void doValidate(
            File file,
            FileValidationResult result,
            DocumentType configuration);

}
