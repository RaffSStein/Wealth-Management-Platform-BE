package raff.stein.customer.repository.goal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.customer.model.entity.goals.GoalTypeEntity;

import java.util.List;

@Repository
public interface GoalTypeRepository extends JpaRepository<GoalTypeEntity, Integer> {

    List<GoalTypeEntity> findAllByNameIn(List<String> names);

}
