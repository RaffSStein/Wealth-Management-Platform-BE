package raff.stein.bank.repository.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import raff.stein.bank.model.bo.BankSearchRequest;
import raff.stein.bank.model.entity.BankBranchEntity;

import java.util.ArrayList;
import java.util.List;

public final class FindBankBranchesSpecification {

    private FindBankBranchesSpecification() {
    }

    public static Specification<BankBranchEntity> from(BankSearchRequest request) {
        return (root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();
            addEqual(predicates, cb, root, "bankCode", request.getBankCode());
            addEqual(predicates, cb, root, "branchCode", request.getBranchCode());
            addEqual(predicates, cb, root, "swiftCode", request.getSwiftCode());
            addEqual(predicates, cb, root, "countryCode", request.getCountryCode());
            addEqual(predicates, cb, root, "bankType", request.getBankType());
            addEqual(predicates, cb, root, "status", request.getStatus());
            addEqual(predicates, cb, root, "zipCode", request.getZipCode());
            addLikeIgnoreCase(predicates, cb, root, "bankName", request.getBankName());
            addLikeIgnoreCase(predicates, cb, root, "branchCity", request.getBranchCity());
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // each filter is optional: a null or blank value adds no predicate
    private static void addEqual(
            List<Predicate> predicates,
            CriteriaBuilder cb,
            Root<BankBranchEntity> root,
            String attribute,
            String value) {
        if (hasText(value)) {
            predicates.add(cb.equal(root.get(attribute), value));
        }
    }

    private static void addLikeIgnoreCase(
            List<Predicate> predicates,
            CriteriaBuilder cb,
            Root<BankBranchEntity> root,
            String attribute,
            String value) {
        if (hasText(value)) {
            predicates.add(likeIgnoreCase(cb, root.get(attribute), value));
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static Predicate likeIgnoreCase(
            CriteriaBuilder cb,
            Path<String> path,
            String value) {
        String pattern = "%" + value.trim().toLowerCase() + "%";
        return cb.like(cb.lower(path), pattern);
    }
}
