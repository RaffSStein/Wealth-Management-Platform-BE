package raff.stein.document.event.producer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.FileValidatedEvent;
import raff.stein.document.event.mapper.config.DocumentEventMapperConfig;
import raff.stein.document.model.File;
import raff.stein.document.service.validation.FileValidationResult;

import java.util.Collections;
import java.util.List;

@Mapper(config = DocumentEventMapperConfig.class)
public interface FileToFileValidatedEventMapper {

    FileToFileValidatedEventMapper MAPPER = Mappers.getMapper(FileToFileValidatedEventMapper.class);

    @Mapping(target = "fileId", source = "file.id")
    @Mapping(target = "fileSize", source = "file.multipartFile.size")
    @Mapping(target = "mimeType", source = "file.multipartFile.contentType")
    @Mapping(target = "fileName", source = "file.multipartFile.originalFilename")
    @Mapping(target = "errors", source = "fileValidationResult.errors", qualifiedByName = "fileValidationErrorsToEventErrors")
    @Mapping(target = "isValid", expression = "java(fileValidationResult.isValid())")
    @Mapping(target = "validatedAt", source = "fileValidationResult.validatedAt")
    FileValidatedEvent toFileValidatedEvent(File file, FileValidationResult fileValidationResult);

    @Named("fileValidationErrorsToEventErrors")
    static List<String> fileValidationErrorsToEventErrors(List<FileValidationResult.ValidationError> errors) {
        if (errors == null || errors.isEmpty()) {
            return Collections.emptyList();
        }
        // Convert the list of validation errors object to a list of error messages
        return errors.stream()
                .map(FileValidationResult.ValidationError::message)
                .toList();
    }
}
