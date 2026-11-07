package raff.stein.bank.repository.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raff.stein.bank.model.bo.BankSearchRequest;
import raff.stein.bank.model.entity.BankBranchEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Records the predicates the specification builds, as readable strings, in the order they are combined.
 */
class FindBankBranchesSpecificationTest {

    private Root<BankBranchEntity> root;
    private CriteriaBuilder cb;
    private List<String> combined;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        root = mock(Root.class);
        cb = mock(CriteriaBuilder.class);
        combined = new ArrayList<>();
        when(root.get(anyString())).thenAnswer(invocation -> mock(Path.class, invocation.<String>getArgument(0)));
        when(cb.lower(any())).thenAnswer(invocation ->
                mock(Expression.class, "lower(" + invocation.getArgument(0) + ")"));
        when(cb.equal(any(Expression.class), any(Object.class))).thenAnswer(invocation ->
                mock(Predicate.class, invocation.getArgument(0) + " = " + invocation.getArgument(1)));
        when(cb.like(any(), anyString())).thenAnswer(invocation ->
                mock(Predicate.class, invocation.getArgument(0) + " like " + invocation.getArgument(1)));
        when(cb.and(any(Predicate[].class))).thenAnswer(invocation -> {
            Arrays.stream(invocation.getArguments()).map(String::valueOf).forEach(combined::add);
            return mock(Predicate.class);
        });
    }

    @Test
    void shouldCombineEveryFilterInOrder() {
        predicatesFor(BankSearchRequest.builder()
                .bankCode("BANK_A").branchCode("BR_01").swiftCode("SWIFT").countryCode("IT").bankType("RETAIL")
                .status("ACTIVE").zipCode("20100").bankName("  Banca ROSSA ").branchCity("MILANO")
                .build());

        assertEquals(List.of(
                "bankCode = BANK_A",
                "branchCode = BR_01",
                "swiftCode = SWIFT",
                "countryCode = IT",
                "bankType = RETAIL",
                "status = ACTIVE",
                "zipCode = 20100",
                "lower(bankName) like %banca rossa%",
                "lower(branchCity) like %milano%"), combined);
    }

    @Test
    void shouldIgnoreNullAndBlankFilters() {
        predicatesFor(BankSearchRequest.builder().bankCode(" ").branchCode("").bankName(null).status("ACTIVE").build());

        assertEquals(List.of("status = ACTIVE"), combined);
    }

    @Test
    void shouldMatchEverythingWithoutFilters() {
        predicatesFor(BankSearchRequest.builder().build());

        assertEquals(List.of(), combined);
    }

    @SuppressWarnings("unchecked")
    private void predicatesFor(BankSearchRequest request) {
        FindBankBranchesSpecification.from(request).toPredicate(root, mock(CriteriaQuery.class), cb);
    }
}
