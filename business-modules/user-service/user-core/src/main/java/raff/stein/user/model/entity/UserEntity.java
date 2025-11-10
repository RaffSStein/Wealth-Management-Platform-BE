package raff.stein.user.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity representing a user of the platform. Each user has a unique email, first and last name.
 */
@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity extends BaseDateEntity<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relationships

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "settings_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private UserSettingsEntity userSettings;

    /**
     * List of associations between this user and bank branches (roles per branch).
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<BranchUserEntity> bankBranchUsers = new HashSet<>();

    // Fields

    @Column(nullable = false, unique = true)
    @Setter
    private String email;

    @Column(nullable = false)
    @Setter
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String phoneNumber;

    @Setter
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(length = 2)
    private String country; // es: "IT", "US"

    @Column(length = 100)
    private String address;

    @Column(length = 50)
    private String city;

    @Column
    private LocalDate birthDate;

    @Column(length = 10)
    private String gender; // es: "M", "F", "OTHER"

    @Column(length = 20)
    private String taxId; // Fiscal Code or equivalent, can be null if not applicable in the user's country


}
