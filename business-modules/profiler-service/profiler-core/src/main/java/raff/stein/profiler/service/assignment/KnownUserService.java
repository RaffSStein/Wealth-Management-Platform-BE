package raff.stein.profiler.service.assignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.profiler.model.entity.KnownUserEntity;
import raff.stein.profiler.repository.KnownUserRepository;

import java.util.UUID;

/**
 * Keeps the read model of users created by user-service, used to validate role assignment targets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnownUserService {

    private final KnownUserRepository knownUserRepository;

    /**
     * Upserts the user: replaying an event, or two events for the same new user at once, is harmless.
     */
    @Transactional
    public KnownUserEntity recordUser(UUID userId, String email) {
        knownUserRepository.upsert(userId, email);
        log.debug("Recorded known user [{}]", userId);
        return knownUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Known user just recorded is missing: " + userId));
    }
}
