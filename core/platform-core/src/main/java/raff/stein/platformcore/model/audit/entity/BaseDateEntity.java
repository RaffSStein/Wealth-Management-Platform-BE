package raff.stein.platformcore.model.audit.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Base entity class for audit information.
 * <p>
 * This abstract class provides common audit fields for all entities, such as creation and modification metadata,
 * and supports optimistic locking. It is intended to be extended by all entities that require audit tracking.
 * </p>
 *
 * @param <I> the type of the entity identifier
 */
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
@Getter
public abstract class BaseDateEntity<I> extends BaseEntity<I> {

    //TODO:
    // history table for changes
    // tracing: correlationId, traceId, spanId ?

    /**
     * The username or identifier of the user who created the entity.
     * Automatically populated by Spring Data JPA auditing.
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false, nullable = false)
    @JsonIgnore
    private String createdBy;

    /**
     * The timestamp when the entity was created.
     * Automatically populated by Spring Data JPA auditing.
     */
    @CreatedDate
    @Column(name = "creation_date", updatable = false, nullable = false)
    @JsonIgnore
    private Instant createdDate;

    /**
     * The username or identifier of the user who last modified the entity.
     * Automatically populated by Spring Data JPA auditing on update.
     */
    @LastModifiedBy
    @Column(name = "last_modified_by")
    @JsonIgnore
    private String lastModifiedBy;

    /**
     * The timestamp when the entity was last modified.
     * Automatically populated by Spring Data JPA auditing on update.
     */
    @LastModifiedDate
    @Column(name = "last_modified_date")
    @JsonIgnore
    private Instant lastModifiedDate;

    /**
     * Version field for optimistic locking, managed by Hibernate.
     * Used to prevent concurrent update conflicts.
     */
    @Version
    private Long version;


}
