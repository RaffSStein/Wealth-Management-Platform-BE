package raff.stein.customer.controller;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.GoalApi;
import org.openapitools.model.GoalTypeDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import raff.stein.customer.controller.mapper.goal.GoalTypeDTOToGoalTypeMapper;
import raff.stein.customer.model.bo.goals.GoalType;
import raff.stein.customer.service.update.GoalsService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GoalController implements GoalApi {

    private final GoalsService goalsService;

    private static final GoalTypeDTOToGoalTypeMapper goalTypeDTOToGoalTypeMapper = GoalTypeDTOToGoalTypeMapper.MAPPER;

    @Override
    public ResponseEntity<List<GoalTypeDTO>> getAllGoalTypes() {
        final List<GoalType> goalTypes = goalsService.getAllGoalTypes();

        List<GoalTypeDTO> goalTypeDTOS = goalTypes
                .stream()
                .map(goalTypeDTOToGoalTypeMapper::toGoalTypeDTO)
                .toList();
        return ResponseEntity.ok(goalTypeDTOS);
    }
}
