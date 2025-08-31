package raff.stein.customer.service.aml.pipeline.step.ue.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import raff.stein.customer.client.eu.sanctions.SanctionClient;
import raff.stein.customer.client.eu.sanctions.model.screen.ScreenRequest;
import raff.stein.customer.client.eu.sanctions.model.screen.ScreenResponse;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.service.aml.pipeline.step.AmlContext;
import raff.stein.customer.service.aml.pipeline.step.DefaultAmlStep;
import raff.stein.customer.service.aml.pipeline.step.ue.impl.mapper.SanctionRequestMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SanctionScreeningStep extends DefaultAmlStep {

    private final SanctionClient sanctionClient;
    private static final SanctionRequestMapper sanctionRequestMapper = SanctionRequestMapper.MAPPER;

    @Override
    public String name() {
        return "ue-sanctions-screening-step";
    }

    @Override
    public StepOutcome doExecute(AmlContext amlContext) {
        // IMPORTANT: This is a mock implementation. Replace with real sanction screening logic.
        final ScreenRequest screenRequest = sanctionRequestMapper.toScreenRequest(amlContext.getCustomer());
        final ScreenResponse response = sanctionClient.screenClient(screenRequest);
        final List<ScreenResponse.Match> matches = response.getMatches();
        final int scoreSum = matches.stream().mapToInt(ScreenResponse.Match::getScore).sum();
        final Map<String, Object> details = new HashMap<>();
        details.put("response", response);
        details.put("matchesCount", matches.size());
        details.put("scoreSum", scoreSum);
        // return outcome based on sum of scores
        if (scoreSum > 0  && scoreSum <= 50) {
            return StepOutcome.review("Score sum is in 'review' threshold", details);
        }
        if (scoreSum > 50 && scoreSum <= 100) {
            return StepOutcome.fail("Score sum is in critical threshold", details);
        }
        return StepOutcome.pass("Score sum is acceptable", details);
    }

    @Override
    public void preValidate(AmlContext amlContext) {
        final Customer customer = amlContext.getCustomer();
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required for sanction screening");
        }
        requireText(customer.getFirstName(), "first name");
        requireText(customer.getLastName(), "last name");
        requireText(customer.getTaxId(), "tax ID");
        requireNonNull(customer.getDateOfBirth(), "date of birth");
        requireText(customer.getNationality(), "nationality");
        requireText(customer.getGender(), "gender");
        requireText(customer.getAddressLine1(), "address line 1");
        requireText(customer.getAddressLine2(), "address line 2");
        requireText(customer.getCity(), "city");
    }

    private static void requireText(String value, String fieldLabel) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Customer " + fieldLabel + " is required for sanction screening");
        }
    }

    private static void requireNonNull(Object value, String fieldLabel) {
        if (value == null) {
            throw new IllegalArgumentException("Customer " + fieldLabel + " is required for sanction screening");
        }
    }

    @Override
    public void postProcess(AmlContext amlContext, StepOutcome outcome) {
        //TODO: implement persistence for audit/logging purposes (step focused)

    }
}
