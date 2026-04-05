package raff.stein.customer.ai;

import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.UUID;

public interface CustomerInsightAgent {

    record EligibilityResult(
            @Description("The step-by-step reasoning used to reach the decision. MANDATORY.")
            String reason,

            @Description("The final decision: true for ELIGIBLE, false for NOT ELIGIBLE.")
            boolean isEligible,

            @Description("The risk level extracted from the customer MiFID")
            String customerRisk,

            @Description("The risk level extracted from the product snapshot")
            String productRisk
    ) {
    }

    @SystemMessage("""
            You are an expert Wealth Management Advisor.
            Your task is to compare the Customer Risk Profile with the Product Risk Level.
            
            BUSINESS RULES:
            - ELIGIBLE: Customer Risk >= Product Risk.
            - Risk Scale: LOW (1) < MEDIUM (2) < HIGH (3).
            
            COMPARISON LOGIC:
                - You must treat Risk Levels as numeric values: LOW=1, MEDIUM=2, HIGH=3.
                - ELIGIBILITY RULE: A customer can buy any product where (Customer Level >= Product Level).
            
            EXAMPLES FOR YOUR REASONING:
                - Customer HIGH (3) vs Product MEDIUM (2): ELIGIBLE (because 3 is greater than 2).
                - Customer MEDIUM (2) vs Product HIGH (3): NOT ELIGIBLE (because 2 is less than 3).
            
            MANDATORY OUTPUT REQUIREMENTS:
            - You MUST populate the 'reason' field first.
            - The 'reason' MUST contain the internal monologue: "The customer has X risk, the product has Y risk, therefore the decision is Z because...".
            - Do not return empty strings for any field.
            """)
    @UserMessage("""
            Process the investment request for customer {{customerId}} and product {{isin}}.
            Use the following execution plan:
            1. Call the tools to get MiFID and Product data.
            2. In the 'reason' field, justify the match between customer and product risks.
            3. Set 'isEligible' based on the rules provided in the system instructions.
            """)
    EligibilityResult analyzeEligibility(
            @MemoryId UUID customerId,
            @V("isin") String isin);
}
