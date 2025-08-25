package raff.stein.document.model.entity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import raff.stein.document.model.Document;
import raff.stein.document.model.entity.DocumentAccessLogEntity;

@Mapper(config = DocumentEntityCommonMapperConfig.class)
public interface DocumentAccessLogEntityToDocumentMapper {

    DocumentAccessLogEntityToDocumentMapper MAPPER = Mappers.getMapper(DocumentAccessLogEntityToDocumentMapper.class);

    @Mapping(target = "actionType", constant = "UPLOAD")
    @Mapping(target = "actionDetails", constant = "First upload of the document")
    @Mapping(target = "document", ignore = true)
    DocumentAccessLogEntity toUploadAction(Document document);

}
