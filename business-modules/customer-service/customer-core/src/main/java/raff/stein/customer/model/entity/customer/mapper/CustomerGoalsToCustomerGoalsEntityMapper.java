package raff.stein.customer.model.entity.customer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import raff.stein.customer.model.bo.customer.CustomerGoals;
import raff.stein.customer.model.entity.customer.CustomerFinancialGoalsEntity;
import raff.stein.customer.model.entity.goals.mapper.GoalTypeEntityToGoalTypeMapper;
import raff.stein.customer.model.entity.mapper.CustomerEntityCommonMapperConfig;

@Mapper(
        config = CustomerEntityCommonMapperConfig.class,
        uses = {GoalTypeEntityToGoalTypeMapper.class})
public interface CustomerGoalsToCustomerGoalsEntityMapper {

    CustomerGoalsToCustomerGoalsEntityMapper MAPPER = Mappers.getMapper(CustomerGoalsToCustomerGoalsEntityMapper.class);

    @Mapping(target = "goalType", ignore = true)
    @Mapping(target = "customer", ignore = true)
    CustomerFinancialGoalsEntity toCustomerGoalsEntity(CustomerGoals customerGoals);

    CustomerGoals toCustomerGoals(CustomerFinancialGoalsEntity customerGoalsEntity);
}
