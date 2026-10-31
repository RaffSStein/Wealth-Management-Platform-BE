package raff.stein.bank.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.bank.model.bo.Bank;
import raff.stein.bank.model.bo.BankBranch;
import raff.stein.bank.model.bo.BankSearchRequest;
import raff.stein.bank.model.entity.BankBranchEntity;
import raff.stein.bank.model.entity.mapper.BankBranchToBranchEntityMapper;
import raff.stein.bank.repository.BankRepository;
import raff.stein.bank.repository.specification.FindBankBranchesSpecification;
import raff.stein.bank.service.visibility.BankScope;
import raff.stein.bank.service.visibility.BankVisibilityPolicy;
import raff.stein.platformcore.exception.types.notfound.GenericObjectNotFoundException;
import raff.stein.platformcore.security.context.SecurityContextHolder;

import java.util.List;

/**
 * Banks and branches, limited by {@link BankVisibilityPolicy}: a PLATFORM_ADMIN reads every bank, any other caller
 * only the bank of its token.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankService {

    static final int MAX_PAGE_SIZE = 100;

    private final BankRepository bankRepository;
    private final BankVisibilityPolicy visibilityPolicy;

    private static final BankBranchToBranchEntityMapper bankBranchToBranchEntityMapper = BankBranchToBranchEntityMapper.MAPPER;

    @Transactional(readOnly = true)
    public List<Bank> listBanks() {
        final BankScope scope = visibilityPolicy.scopeOf(SecurityContextHolder.getContext());
        if (scope.isNone()) {
            return List.of();
        }
        return scope.all() ? bankRepository.findAllBanks() : bankRepository.findBank(scope.bankCode());
    }

    @Transactional(readOnly = true)
    public Page<BankBranch> findBankBranches(BankSearchRequest bankSearchRequest) {
        log.info("Searching bank branches with criteria: {}", bankSearchRequest);
        final BankScope scope = visibilityPolicy.scopeOf(SecurityContextHolder.getContext());
        final Pageable pageable = limit(bankSearchRequest.getPageable());
        final Specification<BankBranchEntity> spec = FindBankBranchesSpecification.from(bankSearchRequest)
                .and(inScope(scope));
        return bankRepository.findAll(spec, pageable).map(bankBranchToBranchEntityMapper::toModel);
    }

    @Transactional(readOnly = true)
    public BankBranch getBankBranch(String bankCode, String branchCode) {
        final BankScope scope = visibilityPolicy.scopeOf(SecurityContextHolder.getContext());
        return bankRepository.findByBankCodeAndBranchCode(bankCode, branchCode)
                .filter(branch -> scope.includes(branch.getBankCode()))
                .map(bankBranchToBranchEntityMapper::toModel)
                .orElseThrow(GenericObjectNotFoundException.with(bankCode + "/" + branchCode));
    }

    // a caller limited to one bank gets that bank's rows only, whatever bankCode filter it sent
    private static Specification<BankBranchEntity> inScope(BankScope scope) {
        return (root, query, cb) -> scope.all()
                ? cb.conjunction()
                : scope.isNone() ? cb.disjunction() : cb.equal(root.get("bankCode"), scope.bankCode());
    }

    private static Pageable limit(Pageable requested) {
        if (requested == null || requested.isUnpaged()) {
            return PageRequest.of(0, 20);
        }
        return requested.getPageSize() > MAX_PAGE_SIZE
                ? PageRequest.of(requested.getPageNumber(), MAX_PAGE_SIZE, requested.getSort())
                : requested;
    }
}
