package raff.stein.document.event.producer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.DocumentUploadedEvent;
import raff.stein.document.event.mapper.config.DocumentEventMapperConfig;
import raff.stein.document.model.Document;

@Mapper(config = DocumentEventMapperConfig.class)
public interface DocumentToDocumentUploadedEventMapper {

    DocumentToDocumentUploadedEventMapper MAPPER = Mappers.getMapper(DocumentToDocumentUploadedEventMapper.class);

    DocumentUploadedEvent toDocumentUploadedEvent(Document document);

    Document toDocument(DocumentUploadedEvent documentUploadedEvent);
}
