package raff.stein.platformcore.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(OllamaChatModelProperties.class)
public class OllamaChatModelConfiguration {

    @Bean
    public ChatLanguageModel chatLanguageModel(OllamaChatModelProperties properties) {
        String baseUrl = properties.baseUrl();
        String modelName = properties.modelName();

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("platform.ai.ollama.base-url must be configured");
        }
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalStateException("platform.ai.ollama.model-name must be configured");
        }

        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(60))
                .temperature(0.0)   // critical for deterministic responses in business use-cases
                .maxRetries(3)
                .build();
    }
}

