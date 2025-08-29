package raff.stein.customer.service.aml;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.service.aml.pipeline.AmlPipelineExecutor;
import raff.stein.customer.service.aml.pipeline.step.AmlContext;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmlService {

    private final AmlPipelineExecutor amlPipelineExecutor;

    public void triggerAmlCheck(@NonNull Customer customer) {
        log.info("Triggering AML check for customer {} ", customer.getId());
        //TODO: determine jurisdiction based on customer data
        AmlContext context = AmlContext.builder()
                .customer(customer)
                .jurisdiction(null)
                .amlCaseId(UUID.randomUUID())
                .build();

        amlPipelineExecutor.execute(context);
    }


}
