package raff.stein.document.model.entity;

import jakarta.persistence.*;
import lombok.*;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;

/**
 * Entity representing a log of actions performed on a document.
 * This includes actions like viewing, downloading, or editing the document.
 * It is used for tracking user interactions with documents for auditing and analytics purposes.
 */
@Entity
@Table(name = "document_access_log")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAccessLogEntity extends BaseDateEntity<Long> {

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
     * Type of action performed on the document (e.g., VIEW, DOWNLOAD, EDIT).
     */
    @Column(length = 50, nullable = false)
    private String actionType;

    /**
     * Additional details about the action (e.g., IP address, device info).
     */
    @Column(length = 1000)
    private String actionDetails;


}
