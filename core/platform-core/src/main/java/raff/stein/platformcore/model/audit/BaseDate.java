package raff.stein.platformcore.model.audit;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Base class for audit fields (creation and update dates) for DTOs and models.
 * These fields are populated from the entity but should not be used to overwrite entity values on mapping.
 */
@Getter
@Setter
public abstract class BaseDate {
    //TODO: probably not useful in DTOs, consider removing
    /** Entity creation date. */
    private OffsetDateTime createdDate;
    /** Entity last update date. */
    private OffsetDateTime lastModifiedDate;
}
