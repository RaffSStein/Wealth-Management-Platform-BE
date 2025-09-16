package raff.stein.bank.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import raff.stein.bank.model.entity.BankBranchEntity;

import java.util.UUID;

@Repository
public interface BankRepository  extends JpaRepository<BankBranchEntity, UUID>, JpaSpecificationExecutor<BankBranchEntity> {
}
