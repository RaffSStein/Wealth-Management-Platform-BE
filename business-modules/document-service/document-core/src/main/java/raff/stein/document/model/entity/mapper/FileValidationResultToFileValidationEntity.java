package raff.stein.document.model.entity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import raff.stein.document.model.File;
import raff.stein.document.model.entity.FileValidationEntity;
import raff.stein.document.service.validation.FileValidationResult;

import java.util.Collections;
import java.util.List;

@Mapper(config = DocumentEntityCommonMapperConfig.class)
public interface FileValidationResultToFileValidationEntity {

    FileValidationResultToFileValidationEntity MAPPER = Mappers.getMapper(FileValidationResultToFileValidationEntity.class);

    @Mapping(target = "id", ignore = true) // id is auto-generated
    @Mapping(target = "fileId", source = "file.id")
    @Mapping(target = "fileSize", source = "file.multipartFile.size")
    @Mapping(target = "mimeType", source = "file.multipartFile.contentType")
    @Mapping(target = "fileName", source = "file.multipartFile.originalFilename")
    @Mapping(target = "errors", source = "fileValidationResult.errors", qualifiedByName = "fileValidationErrorsToEventErrors")
    @Mapping(target = "isValid", expression = "java(fileValidationResult.isValid())")
    @Mapping(target = "validatedAt", source = "fileValidationResult.validatedAt")
    FileValidationEntity toFileValidationEntity(FileValidationResult fileValidationResult, File file);

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
