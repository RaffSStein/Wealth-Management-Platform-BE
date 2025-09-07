package raff.stein.document.model.entity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import raff.stein.document.model.Document;
import raff.stein.document.model.entity.DocumentVersionEntity;

@Mapper(config = DocumentEntityCommonMapperConfig.class)
public interface DocumentVersionEntityToDocumentMapper {

    DocumentVersionEntityToDocumentMapper MAPPER = Mappers.getMapper(DocumentVersionEntityToDocumentMapper.class);

    Document toDocument(DocumentVersionEntity documentVersionEntity);

    @Mapping(target = "versionNumber", source = "activeVersion")
    @Mapping(target = "filePath", source = "fullFilePath")
    @Mapping(target = "document", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    DocumentVersionEntity toDocumentVersionEntity(Document document);
}
