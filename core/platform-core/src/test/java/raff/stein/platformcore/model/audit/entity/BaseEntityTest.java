package raff.stein.platformcore.model.audit.entity;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseEntityTest {

    private static class SampleEntity extends BaseEntity<Long> {
        @Override
        public Long getId() {
            return 1L;
        }
    }

    @Test
    void shouldBeNewUntilLoadedOrPersisted() {
        final SampleEntity entity = new SampleEntity();
        assertTrue(entity.isNew());

        entity.markNotNew();

        assertFalse(entity.isNew());
    }

    @Test
    void shouldRegisterTheCallbackForLoadAndPersist() throws NoSuchMethodException {
        final Method callback = BaseEntity.class.getDeclaredMethod("markNotNew");

        assertTrue(callback.isAnnotationPresent(PostLoad.class));
        assertTrue(callback.isAnnotationPresent(PostPersist.class));
    }
}
