package raff.stein.profiler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.profiler.model.entity.FeatureEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureRepository extends JpaRepository<FeatureEntity, Integer> {

    List<FeatureEntity> findAllBySectionId(Integer sectionId);

    List<FeatureEntity> findAllBySectionIdIn(List<Integer> sectionIds);

    Optional<FeatureEntity> findBySection_SectionCodeAndFeatureCode(String sectionCode, String featureCode);
}
