package raff.stein.profiler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.profiler.model.entity.UserFeaturePermissionEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserFeaturePermissionRepository extends JpaRepository<UserFeaturePermissionEntity, UUID> {

    List<UserFeaturePermissionEntity> findAllByUserEmail(String userEmail);
    List<UserFeaturePermissionEntity> findAllByUserEmailAndBankCodeAndBranchCode(String userEmail, String bankCode, String branchCode);

    void deleteByUserEmailAndBankCodeAndBranchCode(String userEmail, String bankCode, String branchCode);
    List<UserFeaturePermissionEntity> findAllByFeature_Id(Integer featureId);
    List<UserFeaturePermissionEntity> findAllByUserEmailAndFeature_Id(String userEmail, Integer featureId);
}

