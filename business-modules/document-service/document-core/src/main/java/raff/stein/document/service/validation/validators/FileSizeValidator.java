package raff.stein.document.service.validation.validators;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import raff.stein.document.model.DocumentType;
import raff.stein.document.model.File;
import raff.stein.document.service.validation.FileValidationResult;
import raff.stein.document.service.validation.FileValidator;

@Slf4j
public class FileSizeValidator extends FileValidator {

    @Override
    protected void doValidate(File file, FileValidationResult result, DocumentType configuration) {
        final MultipartFile multipartFile = file.getMultipartFile();
        if(multipartFile.getSize() != 0) {
            final long fileSizeInBytes = multipartFile.getSize();
            if(fileSizeInBytes > configuration.getMaxFileSize()) {
                result.addError(
                        FileSizeValidator.class.getName(),
                        String.format("MultipartFile size is too large: %d bytes, maximum allowed: %d bytes",
                                fileSizeInBytes,
                                configuration.getMaxFileSize()));
            }
        } else {
            result.addError(FileSizeValidator.class.getName(), "MultipartFile size cannot be zero");
        }
    }
}
