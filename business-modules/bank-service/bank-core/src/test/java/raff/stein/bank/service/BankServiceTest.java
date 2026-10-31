package raff.stein.bank.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import raff.stein.bank.model.bo.Bank;
import raff.stein.bank.model.bo.BankSearchRequest;
import raff.stein.bank.model.entity.BankBranchEntity;
import raff.stein.bank.repository.BankRepository;
import raff.stein.bank.service.visibility.BankVisibilityPolicy;
import raff.stein.platformcore.exception.types.notfound.GenericObjectNotFoundException;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BankServiceTest {

    private BankRepository repository;
    private BankService service;

    @BeforeEach
    void setUp() {
        repository = mock(BankRepository.class);
        service = new BankService(repository, new BankVisibilityPolicy());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    @Test
    void platformAdminShouldListEveryBank() {
        givenCaller("PLATFORM_ADMIN", "PLATFORM");
        when(repository.findAllBanks()).thenReturn(List.of(bank("BANK_A"), bank("BANK_B")));

        assertEquals(List.of("BANK_A", "BANK_B"), service.listBanks().stream().map(Bank::bankCode).toList());
        verify(repository, never()).findBank(anyString());
    }

    @Test
    void bankAdminShouldListOnlyItsBank() {
        givenCaller("BANK_ADMIN", "BANK_A");
        when(repository.findBank("BANK_A")).thenReturn(List.of(bank("BANK_A")));

        assertEquals(List.of("BANK_A"), service.listBanks().stream().map(Bank::bankCode).toList());
        verify(repository, never()).findAllBanks();
    }

    @Test
    void aTokenWithoutBankShouldListNoBank() {
        givenCaller("CUSTOMER", null);

        assertTrue(service.listBanks().isEmpty());
        verify(repository, never()).findAllBanks();
    }

    @Test
    void anotherBanksBranchShouldLookNotFound() {
        givenCaller("ADVISOR", "BANK_A");
        when(repository.findByBankCodeAndBranchCode("BANK_B", "BR_09")).thenReturn(Optional.of(branch("BANK_B", "BR_09")));
        when(repository.findByBankCodeAndBranchCode("BANK_A", "BR_01")).thenReturn(Optional.of(branch("BANK_A", "BR_01")));

        assertThrows(GenericObjectNotFoundException.class, () -> service.getBankBranch("BANK_B", "BR_09"));
        assertEquals("BR_01", service.getBankBranch("BANK_A", "BR_01").getBranchCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchShouldCapThePageSize() {
        givenCaller("BANK_ADMIN", "BANK_A");
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.<BankBranchEntity>of(), invocation.getArgument(1), 0));

        final Page<?> page = service.findBankBranches(BankSearchRequest.builder()
                .pageable(PageRequest.of(0, 500)).status("ACTIVE").build());

        final ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageable.capture());
        assertEquals(BankService.MAX_PAGE_SIZE, pageable.getValue().getPageSize());
        assertEquals(0, page.getTotalElements());
    }

    private static void givenCaller(String role, String bankCode) {
        SecurityContextHolder.setContext(WMPContext.builder()
                .userId("u-1").roles(Set.of(role)).tenantId(bankCode).branchCode("BR_01").build());
    }

    private static Bank bank(String code) {
        return new Bank(code, code + " name", "IT", "RETAIL");
    }

    private static BankBranchEntity branch(String bankCode, String branchCode) {
        return BankBranchEntity.builder().bankCode(bankCode).branchCode(branchCode).bankName("n").branchName("b")
                .swiftCode("S").countryCode("IT").bankType("RETAIL").status("ACTIVE").build();
    }
}
