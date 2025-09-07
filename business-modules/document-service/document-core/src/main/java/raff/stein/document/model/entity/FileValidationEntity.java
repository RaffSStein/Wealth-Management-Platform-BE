package raff.stein.document.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "file_validation")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileValidationEntity  extends BaseDateEntity<Long> {

    /**
     * Unique identifier for the file validation.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique identifier of the file.
     */
    @Column(nullable = false)
    private UUID fileId;

    /**
     * Name of the validated file.
     */
    @Column(nullable = false)
    private String fileName;

    /**
     * Size of the file in bytes.
     */
    @Column(nullable = false)
    private Long fileSize;

    /**
     * MIME type of the file (e.g., application/pdf).
     */
    @Column(nullable = false)
    private String mimeType;

    /**
     * Indicates whether the file is valid or not.
     */
    @Column(nullable = false)
    private Boolean isValid;

    /**
     * List of validation errors, if any.
     */
    @ElementCollection
    @CollectionTable(name = "file_validation_errors", joinColumns = @JoinColumn(name = "file_validation_id"))
    @Column(name = "error")
    private List<String> errors = new ArrayList<>();

    /**
     * Timestamp of the validation event.
     */
    @Column(nullable = false)
    private OffsetDateTime validatedAt;
}
