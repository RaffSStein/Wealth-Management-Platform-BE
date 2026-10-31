package raff.stein.profiler.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import raff.stein.platformcore.model.audit.entity.BaseEntity;

/**
 * Entity representing a feature of the platform, such as a specific functionality or action
 * (e.g., EXPORT, EDIT_PROFILE). Each feature is associated with a section of the platform.
 * This entity is used for fine-grained permission management, allowing the platform to control
 * access to individual features within sections.
 */
@Entity
// a feature code is unique within its section: sitemaps resolve features by (section code, feature code)
@Table(name = "features", uniqueConstraints = @UniqueConstraint(
        name = "uk_feature_section_code", columnNames = {"section_id", "featureCode"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureEntity extends BaseEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String featureCode; // e.g.: "EXPORT", "EDIT_PROFILE"

    @Column(nullable = false)
    private String featureName;

    /**
     * Position within its section (FE side nav), set by the catalogue seed. Nullable only so the column can be added
     * to existing rows; lists put rows without an order last.
     */
    @Column(name = "display_order")
    private Integer displayOrder;

    @ManyToOne(optional = false)
    @JoinColumn(name = "section_id")
    private SectionEntity section;
}
