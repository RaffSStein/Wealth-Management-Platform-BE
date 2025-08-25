package raff.stein.document.service.validation.validators;

import org.springframework.web.multipart.MultipartFile;
import raff.stein.document.model.DocumentType;
import raff.stein.document.model.File;
import raff.stein.document.service.validation.FileValidationResult;
import raff.stein.document.service.validation.FileValidator;

import java.util.Set;

public class FileExtensionValidator extends FileValidator {

    @Override
    protected void doValidate(File file, FileValidationResult result, DocumentType configuration) {
        final MultipartFile multipartFile = file.getMultipartFile();
        final String fileMimeType = multipartFile.getContentType();
        if (fileMimeType != null && !fileMimeType.isEmpty()) {
            final Set<String> allowedMimeTypes = configuration.getAllowedMimeTypes();
            if (!allowedMimeTypes.contains(fileMimeType)) {
                result.addError(
                        FileExtensionValidator.class.getName(),
                        String.format("MultipartFile MIME type '%s' is not allowed. " +
                                "Allowed MIME types: %s",
                                fileMimeType,
                                allowedMimeTypes));
            }
        } else {
            result.addError(
                    FileExtensionValidator.class.getName(),
                    "MultipartFile MIME type cannot be null");
        }
    }
}
