package raff.stein.bank.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import raff.stein.bank.model.bo.BankBranch;
import raff.stein.bank.model.bo.BankSearchRequest;
import raff.stein.bank.model.entity.BankBranchEntity;
import raff.stein.bank.model.entity.mapper.BankBranchToBranchEntityMapper;
import raff.stein.bank.repository.BankRepository;
import raff.stein.bank.repository.specification.FindBankBranchesSpecification;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankService {

    private final BankRepository bankRepository;

    private static final BankBranchToBranchEntityMapper bankBranchToBranchEntityMapper = BankBranchToBranchEntityMapper.MAPPER;

    public Page<BankBranch> findBankBranches(BankSearchRequest bankSearchRequest) {
        log.info("Searching bank branches with criteria: {}", bankSearchRequest);
        Pageable pageable = bankSearchRequest.getPageable() != null ?
                bankSearchRequest.getPageable() :
                PageRequest.of(0, 20);
        Specification<BankBranchEntity> spec = FindBankBranchesSpecification.from(bankSearchRequest);
        Page<BankBranchEntity> page = bankRepository.findAll(spec, pageable);
        return page.map(bankBranchToBranchEntityMapper::toModel);
    }
}
