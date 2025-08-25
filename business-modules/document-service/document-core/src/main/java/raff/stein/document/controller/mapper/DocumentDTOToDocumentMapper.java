package raff.stein.document.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.DocumentDTO;
import raff.stein.document.model.Document;

@Mapper(config = DocumentControllerCommonMapperConfig.class)
public interface DocumentDTOToDocumentMapper {

    DocumentDTOToDocumentMapper MAPPER = Mappers.getMapper(DocumentDTOToDocumentMapper.class);

    Document toDocument(DocumentDTO documentDTO);

    DocumentDTO toDocumentDTO(Document document);

}
