package raff.stein.customer.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import raff.stein.customer.ai.CustomerInsightAgent;
import raff.stein.customer.ai.tool.CustomerComplianceTools;

@Configuration
public class CustomerAiConfiguration {

    @Bean
    public CustomerInsightAgent customerInsightAgent(
            ChatLanguageModel chatLanguageModel,
            CustomerComplianceTools complianceTools) {
        return AiServices.builder(CustomerInsightAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .tools(complianceTools)
                .build();
    }
}

