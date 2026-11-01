package raff.stein.profiler.service.assignment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raff.stein.profiler.model.entity.KnownUserEntity;
import raff.stein.profiler.repository.KnownUserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KnownUserServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "client@bank.test";

    private KnownUserRepository repository;
    private KnownUserService service;

    @BeforeEach
    void setUp() {
        repository = mock(KnownUserRepository.class);
        service = new KnownUserService(repository);
    }

    @Test
    void shouldRecordThroughTheAtomicUpsertAndNeverAReadThenInsert() {
        final KnownUserEntity stored = KnownUserEntity.builder().userId(USER_ID).email(EMAIL).build();
        when(repository.findById(USER_ID)).thenReturn(Optional.of(stored));

        assertEquals(stored, service.recordUser(USER_ID, EMAIL));

        final var order = inOrder(repository);
        order.verify(repository).upsert(USER_ID, EMAIL);
        order.verify(repository).findById(USER_ID);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldFailWhenTheUpsertLeavesNothing() {
        when(repository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.recordUser(USER_ID, EMAIL));
    }
}
