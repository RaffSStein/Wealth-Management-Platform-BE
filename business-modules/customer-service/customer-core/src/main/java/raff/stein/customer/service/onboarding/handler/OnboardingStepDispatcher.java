package raff.stein.customer.service.onboarding.handler;

import lombok.NonNull;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OnboardingStepDispatcher {

    private final Map<OnboardingStep, OnboardingStepHandler> handlerMap;

    public OnboardingStepDispatcher(List<OnboardingStepHandler> stepHandlersList) {
        this.handlerMap = stepHandlersList.stream()
                .collect(Collectors.toMap(OnboardingStepHandler::getHandledStep, Function.identity()));
    }

    public void dispatch(OnboardingStep actualStep, @NonNull OnboardingStepContext context) {
        OnboardingStepHandler handler = handlerMap.get(actualStep);
        if (handler == null) {
            throw new IllegalStateException("No handler found for step: " + actualStep);
        }
        final UUID customerId = context.getCustomerId();
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID must not be null");
        }
        handler.handle(context);
    }
}
