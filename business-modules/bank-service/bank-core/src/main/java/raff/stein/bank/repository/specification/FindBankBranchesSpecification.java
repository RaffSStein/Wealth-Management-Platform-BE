package raff.stein.bank.repository.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
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
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(request.getBankCode())) {
                predicates.add(cb.equal(root.get("bankCode"), request.getBankCode()));
            }
            if (hasText(request.getBranchCode())) {
                predicates.add(cb.equal(root.get("branchCode"), request.getBranchCode()));
            }
            if (hasText(request.getSwiftCode())) {
                predicates.add(cb.equal(root.get("swiftCode"), request.getSwiftCode()));
            }
            if (hasText(request.getCountryCode())) {
                predicates.add(cb.equal(root.get("countryCode"), request.getCountryCode()));
            }
            if (hasText(request.getBankType())) {
                predicates.add(cb.equal(root.get("bankType"), request.getBankType()));
            }
            if (hasText(request.getZipCode())) {
                predicates.add(cb.equal(root.get("zipCode"), request.getZipCode()));
            }
            if (hasText(request.getBankName())) {
                predicates.add(likeIgnoreCase(cb, root.get("bankName"), request.getBankName()));
            }
            if (hasText(request.getBranchCity())) {
                predicates.add(likeIgnoreCase(cb, root.get("branchCity"), request.getBranchCity()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
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
