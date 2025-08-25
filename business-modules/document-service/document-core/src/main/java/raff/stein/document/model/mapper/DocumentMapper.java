package raff.stein.document.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.springframework.web.multipart.MultipartFile;
import raff.stein.document.model.Document;
import raff.stein.document.model.File;
import raff.stein.document.model.entity.mapper.DocumentEntityCommonMapperConfig;
import raff.stein.platformcore.security.context.SecurityContextHolder;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Base64;

@Mapper(config = DocumentEntityCommonMapperConfig.class)
public interface DocumentMapper {

    DocumentMapper MAPPER = Mappers.getMapper(DocumentMapper.class);

    // pass on the fileId for future reference
    @Mapping(target = "initialFileUuid", source = "file.id")
    // default active version to 0
    @Mapping(target = "activeVersion", constant = "0")
    @Mapping(target = "uploadDate", source = "uploadTime")
    // get the email from the security context
    @Mapping(target = "uploadedBy", expression = "java(getEmailFromContext())")
    @Mapping(target = "fileName", source = "file.multipartFile.originalFilename")
    @Mapping(target = "mimeType", source = "file.multipartFile.contentType")
    @Mapping(target = "fileSize", source = "file.multipartFile.size")
    // convert file bytes to Base64 string
    @Mapping(target = "fileContentBase64", source = "file.multipartFile", qualifiedByName = "convertFileBytesToBase64")
    @Mapping(target = "fullFilePath", source = "fullFilePath")
    Document fileToNewDocument(
            File file,
            OffsetDateTime uploadTime,
            String fullFilePath);

    default String getEmailFromContext() {
        return SecurityContextHolder.getContext().getEmail();
    }

    @Named("convertFileBytesToBase64")
    static String convertFileBytesToBase64(MultipartFile multipartFile) {
        try {
            byte[] fileBytes = multipartFile.getBytes();
            if (fileBytes.length == 0) {
                return null;
            }
            // Convert byte array to Base64 string
            return Base64.getEncoder().encodeToString(fileBytes);
        } catch (IOException e) {
            // Handle the exception, e.g., log it or rethrow it
            throw new RuntimeException("Failed to read file bytes while parsing File to Document", e);
        }

    }
}
