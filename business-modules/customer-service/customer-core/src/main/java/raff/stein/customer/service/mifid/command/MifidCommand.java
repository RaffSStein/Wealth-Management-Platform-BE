package raff.stein.customer.service.mifid.command;

import jakarta.annotation.Nullable;
import lombok.NonNull;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;

import java.util.UUID;

public interface MifidCommand {

    MifidActionType getActionType();

    MifidFilling execute(@NonNull UUID customerId, @Nullable MifidFilling mifidFilling);
}
