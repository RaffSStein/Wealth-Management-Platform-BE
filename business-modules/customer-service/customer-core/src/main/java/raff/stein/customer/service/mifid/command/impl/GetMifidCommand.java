package raff.stein.customer.service.mifid.command.impl;

import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.model.entity.mifid.MifidFillingEntity;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;
import raff.stein.customer.model.entity.mifid.mapper.MifidFillingEntityToMifidFillingMapper;
import raff.stein.customer.repository.mifid.MifidFillingRepository;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;
import raff.stein.customer.service.mifid.command.MifidCommand;
import raff.stein.platformcore.exception.types.notfound.GenericObjectNotFoundException;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetMifidCommand implements MifidCommand {

    private final MifidFillingRepository mifidFillingRepository;

    private static final MifidFillingEntityToMifidFillingMapper mifidFillingEntityToMifidFillingMapper =
            MifidFillingEntityToMifidFillingMapper.MAPPER;

    @Override
    public MifidActionType getActionType() {
        return MifidActionType.GET;
    }

    @Override
    public MifidFilling execute(@NonNull UUID customerId, @Nullable MifidFilling mifidFilling) {
        // the filling identified by fillingId when provided, otherwise the latest non-deprecated one
        final Long fillingId = mifidFilling != null ? mifidFilling.getFillingId() : null;
        final Optional<MifidFillingEntity> optionalFilling = fillingId != null
                ? mifidFillingRepository.findByIdAndCustomerId(fillingId, customerId)
                : mifidFillingRepository.findTopByCustomerIdAndStatusNotOrderByIdDesc(customerId, MifidFillingStatus.DEPRECATED);

        return optionalFilling
                .map(mifidFillingEntityToMifidFillingMapper::toMifidFilling)
                .orElseThrow(GenericObjectNotFoundException.with(fillingId != null ? String.valueOf(fillingId) : customerId.toString()));
    }
}
