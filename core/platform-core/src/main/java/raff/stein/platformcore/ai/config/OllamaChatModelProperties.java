package raff.stein.platformcore.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.ai.ollama")
public record OllamaChatModelProperties(
        String baseUrl,
        String modelName
) {
}
