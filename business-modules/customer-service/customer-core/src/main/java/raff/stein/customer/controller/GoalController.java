package raff.stein.customer.controller;

import lombok.RequiredArgsConstructor;
import io.openapiprocessor.openapi.api.GoalApi;
import io.openapiprocessor.openapi.model.GoalTypeDto;
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
    public ResponseEntity<List<GoalTypeDto>> getAllGoalTypes() {
        final List<GoalType> goalTypes = goalsService.getAllGoalTypes();

        List<GoalTypeDto> goalTypeDTOS = goalTypes
                .stream()
                .map(goalTypeDTOToGoalTypeMapper::toGoalTypeDTO)
                .toList();
        return ResponseEntity.ok(goalTypeDTOS);
    }
}
