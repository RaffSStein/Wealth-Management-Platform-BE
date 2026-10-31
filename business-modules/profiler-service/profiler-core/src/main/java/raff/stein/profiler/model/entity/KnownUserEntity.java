package raff.stein.profiler.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Read model of the users created by user-service (from user-created events): lets profiler check that an
 * assignment target exists and know its email, without calling user-service.
 */
@Entity
@Table(name = "known_users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnownUserEntity {

    @Id
    private UUID userId;

    @Setter
    @Column(nullable = false)
    private String email;
}
