package raff.stein.user.service.visibility;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.platformcore.security.role.WmpRole;
import raff.stein.user.model.entity.BranchUserEntity;
import raff.stein.user.model.entity.UserEntity;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Which users a caller may see (lookup, search, GET /user/{id}). The rule exists twice, kept side by side: in memory
 * ({@link #canSee}, {@link #canSeeAssignment}) and as SQL ({@link #scope}) so search pages are scoped by the database.
 * <ul>
 *     <li>anyone: itself</li>
 *     <li>CUSTOMER: only itself</li>
 *     <li>ADVISOR: users with a role assignment in the bank and branch of its token</li>
 *     <li>BANK_ADMIN: users with a role assignment in its bank, plus users not linked to any bank</li>
 *     <li>PLATFORM_ADMIN: everyone</li>
 * </ul>
 * Out of scope is answered like "not found", so the existence of an account isn't revealed.
 */
@Component
public class UserVisibilityPolicy {

    /**
     * The most privileged role in the token (the active role assignment), CUSTOMER when none is known.
     */
    public WmpRole callerRole(WMPContext caller) {
        return Optional.ofNullable(caller.getRoles()).orElse(Set.of()).stream()
                .map(WmpRole::parse)
                .flatMap(Optional::stream)
                .max(Comparator.naturalOrder())
                .orElse(WmpRole.CUSTOMER);
    }

    public boolean canSee(WMPContext caller, UserEntity user) {
        if (isSelf(caller, user)) {
            return true;
        }
        return switch (callerRole(caller)) {
            case PLATFORM_ADMIN -> true;
            case BANK_ADMIN -> user.getBankBranchUsers().isEmpty()
                    || user.getBankBranchUsers().stream().anyMatch(a -> inBank(caller, a));
            case ADVISOR -> user.getBankBranchUsers().stream().anyMatch(a -> inBranch(caller, a));
            case CUSTOMER -> false;
        };
    }

    /**
     * Whether the caller may see this role assignment of a user it can see.
     */
    public boolean canSeeAssignment(WMPContext caller, UserEntity user, BranchUserEntity assignment) {
        if (isSelf(caller, user)) {
            return true;
        }
        return switch (callerRole(caller)) {
            case PLATFORM_ADMIN -> true;
            case BANK_ADMIN -> inBank(caller, assignment);
            case ADVISOR -> inBranch(caller, assignment);
            case CUSTOMER -> false;
        };
    }

    /**
     * The same rule as {@link #canSee}, as a query restriction. {@code linked}: null for everyone in scope, false for
     * users with no role assignment at all, true for users with one the caller may see.
     */
    public Specification<UserEntity> scope(WMPContext caller, Boolean linked) {
        final WmpRole role = callerRole(caller);
        return (root, query, cb) -> {
            final Predicate anyAssignment = cb.exists(assignments(root, query, cb, null, null));
            final Predicate visibleAssignment = switch (role) {
                case PLATFORM_ADMIN -> anyAssignment;
                case BANK_ADMIN -> cb.exists(assignments(root, query, cb, caller.getTenantId(), null));
                case ADVISOR -> cb.exists(assignments(root, query, cb, caller.getTenantId(), caller.getBranchCode()));
                case CUSTOMER -> cb.disjunction();
            };
            final Predicate inScope = switch (role) {
                case PLATFORM_ADMIN -> cb.conjunction();
                case BANK_ADMIN -> cb.or(visibleAssignment, cb.not(anyAssignment));
                case ADVISOR -> visibleAssignment;
                case CUSTOMER -> cb.disjunction();
            };
            if (linked == null) {
                return inScope;
            }
            return linked ? cb.and(inScope, visibleAssignment) : cb.and(inScope, cb.not(anyAssignment));
        };
    }

    // role assignments of the user, optionally restricted to a bank (and branch)
    private static Subquery<Long> assignments(
            Root<UserEntity> root, CriteriaQuery<?> query,
            CriteriaBuilder cb, String bankCode, String branchCode) {
        final Subquery<Long> subquery = query.subquery(Long.class);
        final Root<BranchUserEntity> assignment = subquery.from(BranchUserEntity.class);
        Predicate where = cb.equal(assignment.get("user"), root);
        if (bankCode != null) {
            where = cb.and(where, cb.equal(assignment.get("bankCode"), bankCode));
        }
        if (branchCode != null) {
            where = cb.and(where, cb.equal(assignment.get("branchCode"), branchCode));
        }
        return subquery.select(assignment.get("id")).where(where);
    }

    private static boolean inBank(WMPContext caller, BranchUserEntity assignment) {
        return caller.getTenantId() != null && caller.getTenantId().equals(assignment.getBankCode());
    }

    private static boolean inBranch(WMPContext caller, BranchUserEntity assignment) {
        return inBank(caller, assignment) && Objects.equals(caller.getBranchCode(), assignment.getBranchCode());
    }

    private static boolean isSelf(WMPContext caller, UserEntity user) {
        return user.getId() != null && Objects.equals(caller.getUserId(), user.getId().toString());
    }
}
