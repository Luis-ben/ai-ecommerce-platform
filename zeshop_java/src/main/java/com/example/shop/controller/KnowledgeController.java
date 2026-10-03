package com.example.shop.controller;

import com.example.shop.commerce.CatalogService;
import com.example.shop.commerce.JdbcCatalogStore;
import com.example.shop.knowledge.KnowledgeService;
import com.example.shop.knowledge.KnowledgeStore;
import com.example.shop.security.AuthService;
import org.springframework.beans.factory.ObjectProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final CatalogService catalog;
    private final JdbcCatalogStore jdbcCatalog;
    private final AuthService authService;
    private final KnowledgeStore store;
    private final ObjectProvider<KnowledgeService> knowledgeService;

    public KnowledgeController(CatalogService catalog, JdbcCatalogStore jdbcCatalog, AuthService authService, KnowledgeStore store, ObjectProvider<KnowledgeService> knowledgeService) { this.catalog = catalog; this.jdbcCatalog = jdbcCatalog; this.authService = authService; this.store = store; this.knowledgeService = knowledgeService; }
    public record PolicyRequest(@NotBlank String title, @NotBlank String content) {}

    @GetMapping("/policies")
    public Map<String, Object> policies(@RequestHeader("Authorization") String authorization) {
        requireStaff(authorization);
        var items = store.policies("demo-store").stream().map(document -> Map.<String, Object>of("id", document.id(), "title", document.title(), "content", document.content(), "status", document.status(), "created_at", document.createdAt())).toList();
        return Map.of("items", items, "total", items.size());
    }

    @PostMapping("/policies")
    public Map<String, Object> addPolicy(@RequestHeader("Authorization") String authorization, @Valid @RequestBody PolicyRequest request) {
        requireStaff(authorization);
        KnowledgeStore.Document document = store.add("demo-store", "POLICY", "policy-" + request.title(), request.title(), request.content(), "INDEXED");
        return Map.of("id", document.id(), "title", document.title(), "status", document.status(), "chunks", index(document));
    }

    @PostMapping("/products/{productId}/sync")
    public Map<String, Object> syncProduct(@RequestHeader("Authorization") String authorization, @PathVariable String productId) {
        requireStaff(authorization);
        CatalogService.Product product = jdbcCatalog.enabled() ? jdbcCatalog.get(productId) : catalog.get(productId);
        if (product == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在");
        KnowledgeStore.Document document = store.add("demo-store", "PRODUCT", product.id(), product.title(), product.title() + " " + product.brand() + " " + product.category() + " " + product.description(), "INDEXED");
        return Map.of("id", document.id(), "product_id", product.id(), "status", document.status(), "chunks", index(document));
    }

    private int index(KnowledgeStore.Document document) {
        KnowledgeService service = knowledgeService.getIfAvailable();
        return service == null ? 1 : service.sync(document.sourceType(), document.sourceId(), document.storeId(), document.title(), document.content());
    }

    private void requireStaff(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "需要登录");
        CatalogService.User user = authService.authenticate(authorization.substring("Bearer ".length()));
        if (user == null || !("merchant".equals(user.role()) || "agent".equals(user.role()))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权执行此操作");
    }
}
