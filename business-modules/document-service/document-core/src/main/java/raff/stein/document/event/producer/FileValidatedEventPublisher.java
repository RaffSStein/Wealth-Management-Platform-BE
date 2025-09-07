package raff.stein.document.event.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import raff.stein.document.event.producer.mapper.FileToFileValidatedEventMapper;
import raff.stein.document.model.File;
import raff.stein.document.service.validation.FileValidationResult;
import raff.stein.platformcore.messaging.publisher.WMPBaseEventPublisher;
import raff.stein.platformcore.messaging.publisher.model.EventData;

@Slf4j
@Component
public class FileValidatedEventPublisher {

    private final WMPBaseEventPublisher wmpBaseEventPublisher;
    private final String fileValidatedTopic;
    private static final FileToFileValidatedEventMapper fileToFileValidatedEventMapper = FileToFileValidatedEventMapper.MAPPER;

    public FileValidatedEventPublisher(
            WMPBaseEventPublisher wmpBaseEventPublisher,
            @Value("${kafka.topics.document-service.file-validated.name}") String fileValidatedTopic) {
        this.wmpBaseEventPublisher = wmpBaseEventPublisher;
        this.fileValidatedTopic = fileValidatedTopic;
    }

    public void publishDocumentValidatedEvent(File file, FileValidationResult fileValidationResult) {
        var fileValidatedEvent = fileToFileValidatedEventMapper.toFileValidatedEvent(file, fileValidationResult);
        var eventData = new EventData(fileValidatedEvent);
        wmpBaseEventPublisher.publishCloudEvent(fileValidatedTopic, eventData);
    }


}
