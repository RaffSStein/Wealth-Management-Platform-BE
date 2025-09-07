package raff.stein.document.model.entity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import raff.stein.document.model.Metadata;
import raff.stein.document.model.entity.DocumentMetadataEntity;

@Mapper(config = DocumentEntityCommonMapperConfig.class)
public interface DocumentMetadataEntityToMetadataMapper {

    DocumentMetadataEntityToMetadataMapper MAPPER = Mappers.getMapper(DocumentMetadataEntityToMetadataMapper.class);

    Metadata toDocument(DocumentMetadataEntity documentMetadataEntity);

    DocumentMetadataEntity toDocumentMetadataEntity(Metadata document);
}
