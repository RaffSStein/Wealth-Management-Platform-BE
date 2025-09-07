package raff.stein.customer.controller;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.FinancialApi;
import org.openapitools.model.FinancialTypeDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import raff.stein.customer.controller.mapper.financial.FinancialTypeDTOToFinancialType;
import raff.stein.customer.model.bo.financial.FinancialType;
import raff.stein.customer.service.update.FinancialsService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FinancialController implements FinancialApi {

    private final FinancialsService financialsService;

    private static final FinancialTypeDTOToFinancialType financialTypeDTOToFinancialType  = FinancialTypeDTOToFinancialType.MAPPER;

    @Override
    public ResponseEntity<List<FinancialTypeDTO>> getAllFinancialTypes() {
        List<FinancialType> financialTypes = financialsService.getAllFinancialTypes();
        List<FinancialTypeDTO> financialTypeDTOs = financialTypes.stream()
                .map(financialTypeDTOToFinancialType::toFinancialTypeDTO)
                .toList();
        return ResponseEntity.ok(financialTypeDTOs);
    }
}
