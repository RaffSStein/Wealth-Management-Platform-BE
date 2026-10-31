package raff.stein.platformcore.model.audit.entity;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;

@MappedSuperclass
public abstract class BaseEntity<I> implements Serializable, Persistable<I> {

    @Transient
    protected boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    // Spring Data asks isNew() to choose persist vs merge and to skip deleting "new" entities: an entity read from
    // or written to the database is not new, otherwise repository.delete(entity) silently does nothing
    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
