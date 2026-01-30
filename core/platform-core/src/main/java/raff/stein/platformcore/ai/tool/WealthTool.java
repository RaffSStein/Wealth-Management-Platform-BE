package raff.stein.platformcore.ai.tool;

import java.lang.annotation.*;

/**
 * Marks a tool method as "financially audited".
 * <p>
 * Use together with LangChain4j {@code @Tool} on the same method.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WealthTool {

    /**
     * Auditing classification label (example: "MIFID", "KYC", "PORTFOLIO").
     */
    String auditTag() default "";
}
