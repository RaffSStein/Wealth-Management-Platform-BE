package raff.stein.customer.service.aml.pipeline.step.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import raff.stein.customer.client.eu.sanctions.SanctionClient;
import raff.stein.customer.client.eu.sanctions.model.screen.ScreenResponse;
import raff.stein.customer.service.aml.pipeline.step.AmlContext;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SanctionScreeningStep extends DefaultAmlStep {

    private final SanctionClient sanctionClient;

    @Override
    public String name() {
        return "sanctions-screen";
    }

    @Override
    protected StepOutcome doExecute(AmlContext amlContext) {
        final ScreenResponse response = sanctionClient.screenClient(null);
        Map<String,Object> details = new HashMap<>();
//        details.put("matches", resp.matches());
//        details.put("providerRef", resp.referenceId());
//
//        if (resp.hardMatches() > 0) {
//            return StepOutcome.fail("Hard sanctions match", details);
//        }
//        if (resp.fuzzyMatches() > 0) {
//            return StepOutcome.review("Fuzzy sanctions match requires manual review", details);
//        }
        return StepOutcome.pass("No sanctions match", details);
    }

    @Override
    protected void preValidate(AmlContext ctx) {

    }

    @Override
    protected void postProcess(AmlContext ctx, StepOutcome outcome) {

    }
}
