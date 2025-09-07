package raff.stein.document.model.entity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import raff.stein.document.model.Document;
import raff.stein.document.model.entity.DocumentEntity;
import raff.stein.document.model.entity.DocumentVersionEntity;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper(config = DocumentEntityCommonMapperConfig.class)
public interface DocumentEntityToDocumentMapper {

    DocumentEntityToDocumentMapper MAPPER = Mappers.getMapper(DocumentEntityToDocumentMapper.class);

    @Mapping(target = "documentName", source = "fileName")
    @Mapping(target = "documentType", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "accessLogs", ignore = true)
    @Mapping(target = "versions", ignore = true)
    DocumentEntity toDocumentEntity(Document document);

    @Mapping(target = "documentId", source = "documentEntity.id")
    @Mapping(target = "documentType", source = "documentEntity.documentType.typeName")
    @Mapping(target = "activeVersion", source = "documentEntity.versions",
             qualifiedByName = "getActiveVersionNumber")
    @Mapping(target = "uploadDate", source = "documentEntity.versions",
            qualifiedByName = "getActiveUploadDate")
    @Mapping(target = "fileName", source = "documentEntity.documentName")
    @Mapping(target = "mimeType", source = "documentEntity.versions",
            qualifiedByName = "getActiveMimeType")
    @Mapping(target = "fileSize", source = "documentEntity.versions",
            qualifiedByName = "getActiveVersionSize")
    @Mapping(target = "fullFilePath", source = "documentEntity.versions",
            qualifiedByName = "getActiveFullFilePath")
    @Mapping(target = "uploadedBy", source = "documentEntity.versions",
            qualifiedByName = "getActiveUploadedBy")
    Document toDocument(DocumentEntity documentEntity);

    @Named("getActiveUploadedBy")
    static String getActiveUploadedBy(List<DocumentVersionEntity> versions) {
        return getActiveVersion(versions).getUploadedBy();
    }

    @Named("getActiveFullFilePath")
    static String getActiveFullFilePath(List<DocumentVersionEntity> versions) {
        return getActiveVersion(versions).getFilePath();
    }

    @Named("getActiveVersionSize")
    static Long getActiveVersionSize(List<DocumentVersionEntity> versions) {
        return getActiveVersion(versions).getFileSize();
    }

    @Named("getActiveMimeType")
    static String getActiveMimeType(List<DocumentVersionEntity> versions) {
        return getActiveVersion(versions).getMimeType();
    }

    @Named("getActiveVersionNumber")
    static Integer getActiveVersionNumber(List<DocumentVersionEntity> versions) {
        return getActiveVersion(versions).getVersionNumber();
    }

    @Named("getActiveUploadDate")
    static OffsetDateTime getActiveUploadDate(List<DocumentVersionEntity> versions) {
        return getActiveVersion(versions).getUploadDate();
    }

    private static DocumentVersionEntity getActiveVersion(List<DocumentVersionEntity> versions) {
        return versions.stream()
                .filter(DocumentVersionEntity::isActive)
                .findFirst()
                .orElseThrow(() -> {
                    String documentId = !versions.isEmpty() && versions.get(0) != null
                            ? String.valueOf(versions.get(0).getDocument().getId())
                            : "unknown";
                    return new IllegalStateException("No active version found for document with ID: " + documentId);
                });
    }
}
