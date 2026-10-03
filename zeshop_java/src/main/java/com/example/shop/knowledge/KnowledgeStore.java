package com.example.shop.knowledge;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

/** Policy documents are durable in production and deterministic in local-demo. */
@Component
public class KnowledgeStore {
    public record Document(String id, String storeId, String sourceType, String sourceId, String title, String content, String status, String createdAt) {}
    private final ObjectProvider<JdbcTemplate> jdbcProvider;
    private final Map<String, Document> local = new ConcurrentHashMap<>();
    private JdbcTemplate jdbc;

    public KnowledgeStore(ObjectProvider<JdbcTemplate> jdbcProvider) { this.jdbcProvider = jdbcProvider; }

    @PostConstruct
    void initialize() {
        jdbc = jdbcProvider.getIfAvailable();
        if (jdbc != null) {
            jdbc.execute("CREATE TABLE IF NOT EXISTS knowledge_documents (id VARCHAR(80) PRIMARY KEY, store_id VARCHAR(80) NOT NULL, source_type VARCHAR(30) NOT NULL, source_id VARCHAR(100) NOT NULL, title VARCHAR(240) NOT NULL, content TEXT NOT NULL, status VARCHAR(30) NOT NULL, created_at VARCHAR(40) NOT NULL)");
            if (Boolean.parseBoolean(System.getenv().getOrDefault("SHOP_SEED_DEMO", "false")) && jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_documents", Long.class) == 0) {
                add("demo-store", "POLICY", "policy-return", "退货政策", "未拆封商品在签收后七日内可申请退货，退款需人工审核。", "INDEXED");
                add("demo-store", "POLICY", "policy-shipping", "配送政策", "现货商品通常在一个工作日内发出，物流状态以订单查询接口为准。", "INDEXED");
            }
            return;
        }
        if (local.isEmpty()) {
            add("demo-store", "POLICY", "policy-return", "退货政策", "未拆封商品在签收后七日内可申请退货，退款需人工审核。", "INDEXED");
            add("demo-store", "POLICY", "policy-shipping", "配送政策", "现货商品通常在一个工作日内发出，物流状态以订单查询接口为准。", "INDEXED");
        }
    }

    public Document add(String storeId, String sourceType, String sourceId, String title, String content, String status) {
        Document document = new Document("doc-java-" + UUID.randomUUID().toString().substring(0, 12), storeId, sourceType, sourceId, title, content, status, Instant.now().toString());
        if (jdbc == null) local.put(document.id(), document);
        else jdbc.update("INSERT INTO knowledge_documents(id, store_id, source_type, source_id, title, content, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", document.id(), document.storeId(), document.sourceType(), document.sourceId(), document.title(), document.content(), document.status(), document.createdAt());
        return document;
    }

    public List<Document> policies(String storeId) {
        if (jdbc == null) return local.values().stream().filter(document -> document.storeId().equals(storeId) && document.sourceType().equals("POLICY")).sorted((a, b) -> b.createdAt().compareTo(a.createdAt())).toList();
        return jdbc.query("SELECT id, store_id, source_type, source_id, title, content, status, created_at FROM knowledge_documents WHERE store_id = ? AND source_type = 'POLICY' ORDER BY created_at DESC", (rs, row) -> new Document(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)), storeId);
    }

    public Document product(String storeId, String productId) {
        if (jdbc == null) return local.values().stream().filter(document -> document.storeId().equals(storeId) && document.sourceType().equals("PRODUCT") && document.sourceId().equals(productId)).findFirst().orElse(null);
        try { return jdbc.queryForObject("SELECT id, store_id, source_type, source_id, title, content, status, created_at FROM knowledge_documents WHERE store_id = ? AND source_type = 'PRODUCT' AND source_id = ? ORDER BY created_at DESC LIMIT 1", (rs, row) -> new Document(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)), storeId, productId); } catch (EmptyResultDataAccessException ignored) { return null; }
    }
}
