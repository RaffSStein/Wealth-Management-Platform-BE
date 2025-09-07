package raff.stein.document.event.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import raff.stein.document.event.producer.mapper.DocumentToDocumentUploadedEventMapper;
import raff.stein.document.model.Document;
import raff.stein.platformcore.messaging.publisher.WMPBaseEventPublisher;
import raff.stein.platformcore.messaging.publisher.model.EventData;

@Slf4j
@Component
public class DocumentUploadedEventPublisher {

    private final WMPBaseEventPublisher wmpBaseEventPublisher;
    private final String documentUploadedTopic;
    private static final DocumentToDocumentUploadedEventMapper documentToDocumentUploadedEventMapper = DocumentToDocumentUploadedEventMapper.MAPPER;

    public DocumentUploadedEventPublisher(
            WMPBaseEventPublisher wmpBaseEventPublisher,
            @Value("${kafka.topics.document-service.document-uploaded.name}") String documentUploadedTopic) {
        this.wmpBaseEventPublisher = wmpBaseEventPublisher;
        this.documentUploadedTopic = documentUploadedTopic;
    }

    public void publishDocumentUploadedEvent(Document document) {
        var documentUploadedEvent = documentToDocumentUploadedEventMapper.toDocumentUploadedEvent(document);
        var eventData = new EventData(documentUploadedEvent);
        wmpBaseEventPublisher.publishCloudEvent(documentUploadedTopic, eventData);
    }
}
