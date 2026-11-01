package raff.stein.customer.repository.customer;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.model.entity.customer.enumeration.CustomerStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@UtilityClass
public class CustomerSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    /**
     * The customer list: a deleted record is never listed, even when asked for. Filters are combined with AND.
     */
    public static Specification<CustomerEntity> listed(Optional<String> branchCode, String query, List<CustomerStatus> statuses) {
        return (root, criteriaQuery, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("customerStatus"), CustomerStatus.DELETED));
            branchCode.ifPresent(branch -> predicates.add(cb.equal(root.get("branchCode"), branch)));
            if (!statuses.isEmpty()) {
                predicates.add(root.get("customerStatus").in(statuses));
            }
            if (StringUtils.hasText(query)) {
                predicates.add(matchesQuery(root, cb, query.trim()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate matchesQuery(Root<CustomerEntity> root, CriteriaBuilder cb, String query) {
        final String pattern = "%" + escapeLike(query.toLowerCase(Locale.ROOT)) + "%";
        // first name + last name, so a search for the full name also matches
        final Expression<String> fullName = cb.concat(
                cb.concat(cb.coalesce(root.<String>get("firstName"), ""), " "),
                cb.coalesce(root.<String>get("lastName"), ""));
        return cb.or(
                cb.like(cb.lower(fullName), pattern, LIKE_ESCAPE),
                cb.like(cb.lower(root.get("companyName")), pattern, LIKE_ESCAPE),
                cb.like(cb.lower(root.get("taxId")), pattern, LIKE_ESCAPE));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
