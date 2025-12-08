package raff.stein.ai.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/client")
public class AiChatClientController {

    private static final String DEFAULT_PROMPT = "Ciao, presentati in modo creativo.";

    private final ChatClient chatClient;

    public AiChatClientController(ChatModel chatModel) {

        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()
                )
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .topP(0.7)
                                .model("llama3")
                                .build()
                )
                .build();
    }


    @GetMapping("/simple/chat")
    public String simpleChat() {

        return chatClient.prompt(DEFAULT_PROMPT).call().content();
    }


    @GetMapping("/stream/chat")
    public Flux<String> streamChat(HttpServletResponse response) {

        response.setCharacterEncoding("UTF-8");
        return chatClient.prompt(DEFAULT_PROMPT).stream().content();
    }
}
