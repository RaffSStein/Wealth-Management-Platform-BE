package raff.stein.customer.model.entity.goals.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import raff.stein.customer.model.bo.goals.GoalType;
import raff.stein.customer.model.entity.goals.GoalTypeEntity;
import raff.stein.customer.model.entity.mapper.CustomerEntityCommonMapperConfig;

@Mapper(config = CustomerEntityCommonMapperConfig.class)
public interface GoalTypeEntityToGoalTypeMapper {

    GoalTypeEntityToGoalTypeMapper MAPPER = Mappers.getMapper(GoalTypeEntityToGoalTypeMapper.class);

    GoalTypeEntity toFinancialGoalTypeEntity(GoalType goalType);

    GoalType toGoalType(GoalTypeEntity goalTypeEntity);
}
