package raff.stein.customer.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import raff.stein.customer.ai.tool.projection.ProductProjectionStore;
import raff.stein.customer.model.bo.mifid.filling.CustomerRiskProfile;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.service.mifid.MifidService;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;
import raff.stein.platformcore.ai.tool.WealthTool;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CustomerComplianceTools {

    private final MifidService mifidService;
    private final ProductProjectionStore productProjectionStore;

    public record MifidSummary(CustomerRiskProfile profile, boolean valid, LocalDate date) {}

    @Tool("Retrieve MiFID profile and risk profile for a given customer")
    @WealthTool(auditTag = "MIFID")
    public MifidSummary getMifidProfile(@P("Customer's id") String customerId) {
        UUID customerUuid;
        try {
            customerUuid = UUID.fromString(customerId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid customerId, expected UUID format: " + customerId, ex);
        }

        MifidFilling filling = mifidService.handleFilling(MifidActionType.GET, customerUuid, null);

        return new MifidSummary(
                filling.getCustomerRiskProfile(),
                filling.isValid(),
                filling.getFillingDate()
        );
    }


    @Tool("Retrieve technical details and risk level of a financial product")
    @WealthTool(auditTag = "PRODUCT")
    public ProductProjectionStore.ProductSummary getProductDetails(@P("The ISIN code") String isin) {
        Optional<ProductProjectionStore.ProductSummary> snapshot = productProjectionStore.findByIsin(isin);
        return snapshot.orElseThrow(() -> new IllegalArgumentException("Product with ISIN " + isin + " not found"));
    }
}

