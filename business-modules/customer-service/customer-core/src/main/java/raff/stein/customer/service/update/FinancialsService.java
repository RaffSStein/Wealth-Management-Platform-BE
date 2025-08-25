package raff.stein.customer.service.update;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import raff.stein.customer.model.bo.financial.FinancialType;
import raff.stein.customer.model.entity.financial.mapper.FinancialTypeToFinancialTypeEntityMapper;
import raff.stein.customer.repository.financial.FinancialTypeRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialsService {

    private final FinancialTypeRepository financialTypeRepository;

    private static final FinancialTypeToFinancialTypeEntityMapper financialTypeToFinancialTypeEntityMapper = FinancialTypeToFinancialTypeEntityMapper.MAPPER;

    public List<FinancialType> getAllFinancialTypes() {
        log.info("Fetching all financial types");
        return financialTypeRepository.findAll()
                .stream()
                .map(financialTypeToFinancialTypeEntityMapper::toFinancialType)
                .toList();
    }


}
