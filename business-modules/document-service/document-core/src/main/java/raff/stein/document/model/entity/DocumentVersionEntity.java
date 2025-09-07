package raff.stein.document.model.entity;

import jakarta.persistence.*;
import lombok.*;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;

import java.time.OffsetDateTime;

/**
 * Represents a version of a document in the system.
 * Each version contains metadata about the file, such as its path, size, upload date,
 * and the user who uploaded it.
 * This entity is used to track changes and maintain a history of document versions.
 */
@Entity
@Table(name = "document_version")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersionEntity extends BaseDateEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationships

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    @Setter
    private DocumentEntity document;

    // Fields

    /**
     * Version number of the document (e.g., 1, 2, 3).
     */
    @Column(nullable = false)
    private Integer versionNumber;

    /**
     * Path to the file in the storage system.
     */
    @Column(length = 500, nullable = false)
    private String filePath;

    /**
     * Size of the file in bytes.
     */
    @Column(nullable = false)
    private Long fileSize;

    /**
     * Date and time when the file was uploaded.
     */
    @Column(nullable = false)
    private OffsetDateTime uploadDate;

    /**
     * Email or identifier of the user who uploaded the file.
     */
    @Column(nullable = false)
    private String uploadedBy;

    /**
     * Hash of the file content for integrity check.
     */
    @Column(length = 128)
    private String contentHash;

    /**
     * MIME type of the file (e.g., application/pdf, image/png).
     */
    @Column(length = 100, nullable = false)
    private String mimeType;

    /**
     * Indicates whether the version is currently active.
     * Only one version can be active at a time for a given document.
     */
    @Column(nullable = false)
    private boolean isActive;


}
