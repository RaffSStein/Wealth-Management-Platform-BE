package raff.stein.document.model.entity;

import jakarta.persistence.*;
import lombok.*;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;

/**
 * Entity representing metadata associated with a document.
 * This entity allows for flexible storage of key-value pairs
 * that can be used to store additional information about a document,
 * such as author, creation date, or any other relevant metadata.
 */
@Entity
@Table(name = "document_metadata")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadataEntity extends BaseDateEntity<Long> {

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
     * Metadata key, e.g., "author", "created_at", etc.
     * This should be a short, descriptive string that identifies the type of metadata.
     */
    @Column(nullable = false)
    private String key;

    /**
     * Metadata value, which can be a longer string containing the actual data.
     */
    @Column(length = 1000, nullable = false)
    private String value;


}
