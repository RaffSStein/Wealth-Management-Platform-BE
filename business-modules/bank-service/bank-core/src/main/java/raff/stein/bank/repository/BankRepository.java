package raff.stein.bank.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import raff.stein.bank.model.bo.Bank;
import org.springframework.stereotype.Repository;
import raff.stein.bank.model.entity.BankBranchEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankRepository  extends JpaRepository<BankBranchEntity, UUID>, JpaSpecificationExecutor<BankBranchEntity> {

    Optional<BankBranchEntity> findByBankCodeAndBranchCode(String bankCode, String branchCode);

    // one row per bank: name, country and type are the same on all its branches, min() just picks one
    @Query("""
            select new raff.stein.bank.model.bo.Bank(b.bankCode, min(b.bankName), min(b.countryCode), min(b.bankType))
            from BankBranchEntity b group by b.bankCode order by b.bankCode""")
    List<Bank> findAllBanks();

    @Query("""
            select new raff.stein.bank.model.bo.Bank(b.bankCode, min(b.bankName), min(b.countryCode), min(b.bankType))
            from BankBranchEntity b where b.bankCode = :bankCode group by b.bankCode""")
    List<Bank> findBank(@Param("bankCode") String bankCode);
}
