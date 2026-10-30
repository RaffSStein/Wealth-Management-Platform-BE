package raff.stein.customer.model.entity.aml;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import raff.stein.platformcore.model.tenant.TenantScopedEntity;

@Entity
@Table(
        name = "aml_document",
        uniqueConstraints = @UniqueConstraint(name = "uk_aml_document_uuid", columnNames = {"document_uuid", "aml_verification_id"})
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmlDocumentEntity extends TenantScopedEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationships

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aml_verification_id", nullable = false)
    private AmlVerificationEntity amlVerification;

    // Fields

    @Column(name = "document_uuid", nullable = false, unique = true)
    private String documentUuid;



}
