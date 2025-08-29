package raff.stein.customer.service.aml.pipeline.step;

public interface AmlStep {

    String name();

    AmlStepResult execute(AmlContext context);
}
