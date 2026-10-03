package com.example.shop.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Profile("!local-demo")
@Service
public class AgentService {
    private final ChatClient chatClient;
    private final ChatTools tools;
    private final VectorStore vectorStore;

    public AgentService(ChatClient.Builder builder, ChatTools tools, VectorStore vectorStore) {
        this.tools = tools;
        this.vectorStore = vectorStore;
        this.chatClient = builder
                .defaultSystem("You are Zeshop support. Use tools for live price, stock, order and shipping facts. Never invent business facts. Return policy answers must come from RAG. Never create a ticket without explicit confirmation.")
                .build();
    }

    public Flux<String> stream(String message) {
        return chatClient.prompt()
                .user(message)
                .tools(tools)
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .stream()
                .content();
    }

    public String answer(String message) {
        return chatClient.prompt()
                .user(message)
                .tools(tools)
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .call()
                .content();
    }

    public boolean providerConfigured() {
        String baseUrl = System.getenv("MAIN_MODEL_BASE_URL");
        String apiKey = System.getenv("MAIN_MODEL_API_KEY");
        return baseUrl != null && !baseUrl.isBlank() && apiKey != null && !apiKey.isBlank() && !"unset".equalsIgnoreCase(apiKey);
    }

    public String model() {
        String model = System.getenv("MAIN_MODEL");
        return model == null || model.isBlank() ? "main-model" : model;
    }
}
