package com.example.shop.controller;

import com.example.shop.ai.AgentService;
import com.example.shop.commerce.CatalogService.User;
import com.example.shop.commerce.SupportStore;
import com.example.shop.security.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
public class ChatController {
    private final ObjectProvider<AgentService> agentProvider;
    private final SupportStore supportStore;
    private final AuthService authService;

    public ChatController(ObjectProvider<AgentService> agentProvider, SupportStore supportStore, AuthService authService) {
        this.agentProvider = agentProvider;
        this.supportStore = supportStore;
        this.authService = authService;
    }

    public record ChatRequest(@NotBlank String message, @com.fasterxml.jackson.annotation.JsonProperty("conversation_id") String conversationId, boolean confirmed) {}

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@Valid @RequestBody ChatRequest request, @RequestHeader(name = "Authorization", required = false) String authorization) {
        String customerId = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            User user = authService.authenticate(authorization.substring("Bearer ".length()));
            if (user != null) {
                customerId = user.id();
            }
        }
        SupportStore.Conversation conv = supportStore.getOrCreateConversation("demo-store", customerId, request.conversationId());
        supportStore.addConversationMessage(conv.id(), "user", request.message());

        AgentService agent = agentProvider.getIfAvailable();
        if (agent != null && agent.providerConfigured()) {
            StringBuilder full = new StringBuilder();
            return Flux.just(event("message_start", "{\"conversation_id\":\"" + conv.id() + "\"}"))
                    .concatWith(agent.stream(request.message())
                            .doOnNext(full::append)
                            .map(content -> event("text_delta", "{\"content\":\"" + escape(content) + "\"}")))
                    .concatWith(Flux.defer(() -> {
                        supportStore.addConversationMessage(conv.id(), "assistant", full.toString());
                        return Flux.just(event("done", "{\"status\":\"ok\"}"));
                    }));
        }

        String fallbackReply = "演示 AI 已收到你的问题。当前使用本地商品、订单与政策数据；配置 MAIN_MODEL_BASE_URL 和 MAIN_MODEL_API_KEY 后会切换到真实模型。";
        supportStore.addConversationMessage(conv.id(), "assistant", fallbackReply);
        return Flux.just(
                event("message_start", "{\"conversation_id\":\"" + conv.id() + "\"}"),
                event("text_delta", "{\"content\":\"" + escape(fallbackReply) + "\"}"),
                event("done", "{\"status\":\"ok\"}"));
    }

    private String event(String name, String data) { return "event: " + name + "\ndata: " + data + "\n\n"; }
    private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"); }
}
