package raff.stein.document.model;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class Document {

    private UUID documentId;
    private UUID customerId;
    private Integer activeVersion;
    private String documentType;
    private Set<Metadata> metadata;
    private OffsetDateTime uploadDate;
    private String uploadedBy;
    private String fileName;
    private String mimeType;
    private Long fileSize;
    private String fileContentBase64;
    private String fullFilePath;
    private UUID initialFileUuid;
}
