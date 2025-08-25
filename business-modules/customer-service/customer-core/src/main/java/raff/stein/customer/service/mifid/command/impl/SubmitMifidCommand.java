package raff.stein.customer.service.mifid.command.impl;

import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.service.mifid.command.MifidCommand;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SubmitMifidCommand implements MifidCommand {

    @Override
    public MifidActionType getActionType() {
        return MifidActionType.SUBMIT;
    }

    @Override
    public MifidFilling execute(@NonNull UUID customerId, @Nullable MifidFilling mifidFilling) {
        // answers are checked in pre hook of SubmitMifidWorkflow
        // mifid filling status is set in post hook of SubmitMifidWorkflow
        // just calculate customer risk profile here
        // TODO implement risk profile calculation
        return mifidFilling;
    }
}
