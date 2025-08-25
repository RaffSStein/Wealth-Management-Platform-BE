package raff.stein.document.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;

import java.util.List;
import java.util.UUID;

/**
 * Represents a document entity in the system.
 * This entity contains metadata about the document, its versions, access logs, and associated document type.
 * It is used to manage documents within the document service.
 */
@Entity
@Table(name = "document")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEntity extends BaseDateEntity<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relationships

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    @Setter
    private List<DocumentVersionEntity> versions;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    @Setter
    private List<DocumentAccessLogEntity> accessLogs;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    @Setter
    private List<DocumentMetadataEntity> metadata;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @Setter
    @JoinColumn(name = "document_type_id", nullable = false)
    private DocumentTypeEntity documentType;

    // Fields

    /**
     * The name of the document.
     */
    @Column(nullable = false)
    private String documentName;

    /**
     * The customer ID associated with the document.
     * This field is used to link the document to a specific customer in the system.
     * It is marked as non-nullable to ensure that every document is associated with a customer
     */
    @Column(nullable = false)
    private UUID customerId;

    /**
     * The initial version number of the document, generated before the file validation.
     * just a placeholder to track the initial version before the validated file is uploaded.
     */
    @Column(nullable = false)
    private UUID initialFileUuid;


}
