package raff.stein.document.model.entity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import raff.stein.document.model.DocumentType;
import raff.stein.document.model.entity.DocumentTypeEntity;

@Mapper(config = DocumentEntityCommonMapperConfig.class)
public interface DocumentTypeEntityToDocumentTypeMapper {

    DocumentTypeEntityToDocumentTypeMapper MAPPER = Mappers.getMapper(DocumentTypeEntityToDocumentTypeMapper.class);

    DocumentTypeEntity toDocumentTypeEntity(DocumentType documentType);

    DocumentType toDocumentType(DocumentTypeEntity documentTypeEntity);
}
