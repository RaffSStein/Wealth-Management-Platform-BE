package raff.stein.bank.controller;

import io.openapiprocessor.openapi.api.BankApi;
import io.openapiprocessor.openapi.model.BankBranchDto;
import io.openapiprocessor.openapi.model.BankDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import raff.stein.bank.controller.mapper.BankBranchDtoToBankBranchMapper;
import raff.stein.bank.model.bo.BankSearchRequest;
import raff.stein.bank.service.BankService;

@RestController
@RequiredArgsConstructor
public class BankController implements BankApi {

    private final BankService bankService;

    private static final BankBranchDtoToBankBranchMapper bankBranchDtoToBankBranchMapper = BankBranchDtoToBankBranchMapper.MAPPER;
    //TODO implement methods

    @Override
    public ResponseEntity<BankBranchDto> createBankBranch(BankBranchDto body) {
        return null;
    }

    @Override
    public ResponseEntity<BankDto[]> getBanks() {
        return ResponseEntity.ok(bankService.listBanks().stream()
                .map(bankBranchDtoToBankBranchMapper::toBankDto)
                .toArray(BankDto[]::new));
    }

    @Override
    public ResponseEntity<Page<BankBranchDto>> getBankBranches(
            String status,
            String bankCode,
            String bankName,
            String branchCode,
            String swiftCode,
            String countryCode,
            String bankType,
            String branchCity,
            String zipCode,
            Pageable pageable) {
        final BankSearchRequest bankSearchRequest = bankBranchDtoToBankBranchMapper.toBankSearchRequest(
                pageable,
                bankCode,
                bankName,
                branchCode,
                swiftCode,
                countryCode,
                bankType,
                branchCity,
                zipCode,
                status);
        final Page<BankBranchDto> bankBranchDtos = bankService.findBankBranches(bankSearchRequest)
                .map(bankBranchDtoToBankBranchMapper::toBankBranchDto);
        return ResponseEntity.ok(bankBranchDtos);
    }

    @Override
    public ResponseEntity<BankBranchDto> getBankBranchOfBank(String bankCode, String branchCode) {
        return ResponseEntity.ok(bankBranchDtoToBankBranchMapper.toBankBranchDto(
                bankService.getBankBranch(bankCode, branchCode)));
    }

    @Override
    public ResponseEntity<BankBranchDto> getBankBranchByCode(String branchCode) {
        return null;
    }

    @Override
    public ResponseEntity<BankBranchDto> updateBankBranch(String branchCode, BankBranchDto body) {
        return null;
    }

    @Override
    public ResponseEntity<Void> deleteBankBranch(String branchCode) {
        return null;
    }
}
