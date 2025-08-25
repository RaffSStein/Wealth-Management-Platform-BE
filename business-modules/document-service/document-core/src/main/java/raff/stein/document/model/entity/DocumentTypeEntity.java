package raff.stein.document.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;
import raff.stein.platformcore.model.converter.entity.StringSetConverter;

import java.util.List;
import java.util.Set;

/**
 * Entity representing a document type in the system.
 * This entity defines the properties and constraints for different types of documents that can be managed.
 * It includes fields for the type name, description, maximum file size, allowed MIME types,
 * and allowed operations.
 */
@Entity
@Table(name = "document_type")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTypeEntity extends BaseDateEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Relationships

    @OneToMany(mappedBy = "documentType")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<DocumentEntity> documents;

    // Fields

    /**
     * The name of the document type.
     * This field is mandatory and should be unique across all document types.
     */
    @Column(nullable = false, unique = true)
    private String typeName;

    /**
     * A brief description of the document type.
     * This field is optional and can be used to provide additional context or information about the document
     */
    @Column(length = 500)
    private String description;

    /**
     * The maximum file size allowed for documents of this type, in bytes.
     * This field is mandatory and should be set to a positive value.
     * It defines the upper limit for the size of files that can be uploaded for this document type.
     * For example, a value of 10485760 would represent a maximum file size of 10 MB.
     * This constraint helps to manage storage and performance by preventing excessively large files
     * from being uploaded, which could impact the system's performance or storage capacity.
     */
    @Column(nullable = false)
    private Long maxFileSize;

    /**
     * A set of allowed MIME types for documents of this type.
     * This field is mandatory and defines the types of files that can be uploaded for this document type.
     * It helps to ensure that only files of specific formats are accepted, enhancing security and compatibility.
     * For example, a set might include "application/pdf", "image/jpeg", etc.
     */
    @Column(nullable = false)
    @Convert(converter = StringSetConverter.class)
    private Set<String> allowedMimeTypes;

    /**
     * A set of allowed operations that can be performed on documents of this type.
     * This field is mandatory and defines the actions that can be taken with documents of this type,
     * such as "view", "edit", "delete", etc.
     * It helps to enforce security and access control by specifying which operations are permitted.
     */
    @Column(nullable = false)
    @Convert(converter = StringSetConverter.class)
    private Set<String> allowedOperations;


}
