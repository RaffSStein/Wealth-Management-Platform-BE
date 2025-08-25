package raff.stein.document.model;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class DocumentType {

    private Integer id;
    private String typeName;
    private String description;
    private Long maxFileSize;               // Maximum file size in bytes
    private Set<String> allowedMimeTypes;
    private Set<String> allowedOperations;
}
