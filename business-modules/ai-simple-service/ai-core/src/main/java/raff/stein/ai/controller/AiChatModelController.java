package raff.stein.ai.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


@RestController
@RequestMapping("/model")
public class AiChatModelController {

    private static final String DEFAULT_PROMPT = "Ciao, presentati in modo creativo.";

    private final ChatModel chatModel;


    public AiChatModelController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }


    @GetMapping("/simple/chat")
    public String simpleChat() {

        return chatModel.call(new Prompt(DEFAULT_PROMPT)).getResult().getOutput().getText();
    }

    @GetMapping("/stream/chat")
    public Flux<String> streamChat(HttpServletResponse response) {

        response.setCharacterEncoding("UTF-8");

        Flux<ChatResponse> stream = chatModel.stream(new Prompt(DEFAULT_PROMPT));
        return stream.map(resp -> resp.getResult().getOutput().getText());
    }


    @GetMapping("/custom/chat")
    public String customChat() {

        OllamaChatOptions customOptions = OllamaChatOptions.builder()
                .topP(0.7)
                .model("llama3")
                .temperature(0.8)
                .build();

        return chatModel.call(new Prompt(DEFAULT_PROMPT, customOptions)).getResult().getOutput().getText();
    }
}
