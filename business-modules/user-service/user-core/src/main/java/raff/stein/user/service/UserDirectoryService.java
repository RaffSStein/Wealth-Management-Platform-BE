package raff.stein.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.badrequest.RequestValidationException;
import raff.stein.platformcore.exception.types.forbidden.AccessDeniedException;
import raff.stein.platformcore.exception.types.notfound.GenericObjectNotFoundException;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.platformcore.security.role.WmpRole;
import raff.stein.user.model.BranchRole;
import raff.stein.user.model.entity.UserEntity;
import raff.stein.user.model.entity.mapper.BranchUserToBranchUserEntity;
import raff.stein.user.model.entity.mapper.UserToUserEntityMapper;
import raff.stein.user.model.user.User;
import raff.stein.user.model.user.UserSummary;
import raff.stein.user.model.user.UserSummaryPage;
import raff.stein.user.repository.UserRepository;
import raff.stein.user.service.visibility.UserVisibilityPolicy;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Finding users: exact lookup, admin search and reading a user by id, all limited by {@link UserVisibilityPolicy}.
 * Out of scope answers 404 like a missing user, so the existence of an account isn't revealed.
 * Searched emails and query texts are never logged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDirectoryService {

    static final int MIN_QUERY_LENGTH = 3;
    static final int MAX_PAGE_SIZE = 50;

    private final UserRepository userRepository;
    private final UserVisibilityPolicy visibilityPolicy;

    private static final UserToUserEntityMapper userMapper = UserToUserEntityMapper.MAPPER;
    private static final BranchUserToBranchUserEntity assignmentMapper = BranchUserToBranchUserEntity.MAPPER;

    @Transactional(readOnly = true)
    public UserSummary lookupByEmail(String email) {
        final WMPContext caller = SecurityContextHolder.getContextOrThrow();
        if (email == null || email.isBlank()) {
            throw invalid("email", email);
        }
        final UserEntity user = userRepository.findByEmailIgnoreCase(email.trim())
                .filter(found -> visibilityPolicy.canSee(caller, found))
                .orElseThrow(() -> {
                    log.info("User lookup by [{}]: no visible match", caller.getUserId());
                    return GenericObjectNotFoundException.with("email").get();
                });
        log.info("User lookup by [{}]: found [{}]", caller.getUserId(), user.getId());
        return toSummary(caller, user);
    }

    @Transactional(readOnly = true)
    public UserSummaryPage search(String query, Boolean linked, int page, int size) {
        final WMPContext caller = SecurityContextHolder.getContextOrThrow();
        final WmpRole role = visibilityPolicy.callerRole(caller);
        if (role != WmpRole.BANK_ADMIN && role != WmpRole.PLATFORM_ADMIN) {
            throw new AccessDeniedException(ErrorCode.ACCESS_DENIED,
                    String.format(ErrorCode.ACCESS_DENIED.getMessageTemplate(), caller.getUserId(), "user search"));
        }
        if (query == null || query.trim().length() < MIN_QUERY_LENGTH) {
            throw invalid("query", query);
        }
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw invalid("page/size", page + "/" + size);
        }
        final Page<UserEntity> result = userRepository.findAll(
                visibilityPolicy.scope(caller, linked).and(matching(query.trim())),
                PageRequest.of(page, size, Sort.by("email")));
        log.info("User search by [{}]: [{}] results", caller.getUserId(), result.getTotalElements());
        return UserSummaryPage.builder()
                .content(result.getContent().stream().map(user -> toSummary(caller, user)).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    /**
     * The full user, if the caller may see it; only the role assignments the caller may see are included.
     */
    @Transactional(readOnly = true)
    public User getVisibleUser(UUID id) {
        final WMPContext caller = SecurityContextHolder.getContextOrThrow();
        final UserEntity user = userRepository.findById(id)
                .filter(found -> visibilityPolicy.canSee(caller, found))
                .orElseThrow(GenericObjectNotFoundException.with(String.valueOf(id)));
        final User result = userMapper.toUser(user);
        result.setBranchRoles(visibleAssignments(caller, user));
        return result;
    }

    private UserSummary toSummary(WMPContext caller, UserEntity user) {
        return UserSummary.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roleAssignments(visibleAssignments(caller, user))
                .build();
    }

    private List<BranchRole> visibleAssignments(WMPContext caller, UserEntity user) {
        return user.getBankBranchUsers().stream()
                .filter(assignment -> visibilityPolicy.canSeeAssignment(caller, user, assignment))
                .map(assignmentMapper::toBranchRole)
                .toList();
    }

    // case-insensitive "contains" on email, first name, last name; LIKE wildcards in the input are literal
    private static Specification<UserEntity> matching(String query) {
        final String pattern = "%" + query.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, cq, cb) -> cb.or(
                cb.like(cb.lower(root.get("email")), pattern, '\\'),
                cb.like(cb.lower(root.get("firstName")), pattern, '\\'),
                cb.like(cb.lower(root.get("lastName")), pattern, '\\'));
    }

    private static RequestValidationException invalid(String field, Object value) {
        return new RequestValidationException(ErrorCode.REQUEST_VALIDATION_ERROR,
                String.format(ErrorCode.REQUEST_VALIDATION_ERROR.getMessageTemplate(), field, value));
    }
}
