package raff.stein.customer.service.update;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import raff.stein.customer.model.bo.goals.GoalType;
import raff.stein.customer.model.entity.goals.mapper.GoalTypeEntityToGoalTypeMapper;
import raff.stein.customer.repository.goal.GoalTypeRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalsService {

    private final GoalTypeRepository goalTypeRepository;

    private static final GoalTypeEntityToGoalTypeMapper goalTypeEntityToGoalTypeMapper = GoalTypeEntityToGoalTypeMapper.MAPPER;

    public List<GoalType> getAllGoalTypes() {
        log.debug("Retrieving all goal types");
        return goalTypeRepository.findAll()
                .stream()
                .map(goalTypeEntityToGoalTypeMapper::toGoalType)
                .toList();
    }
}
