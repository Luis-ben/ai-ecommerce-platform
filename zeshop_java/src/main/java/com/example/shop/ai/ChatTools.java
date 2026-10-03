package com.example.shop.ai;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.example.shop.commerce.CatalogService;
import com.example.shop.commerce.JdbcCatalogStore;
import com.example.shop.knowledge.KnowledgeStore;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("!local-demo")
@Component
public class ChatTools {
    private static final String STORE_ID = "demo-store";
    private final JdbcCatalogStore catalog;
    private final KnowledgeStore knowledge;

    public ChatTools(JdbcCatalogStore catalog, KnowledgeStore knowledge) {
        this.catalog = catalog;
        this.knowledge = knowledge;
    }

    @Tool(name = "search_products", description = "Search active in-stock products for the current store. Never invent price or stock.")
    public List<CatalogService.Product> searchProducts(String query, Integer maxPrice) {
        return catalog.search(query, maxPrice == null ? null : BigDecimal.valueOf(maxPrice), null, null);
    }

    @Tool(name = "get_product_detail", description = "Get a product detail from the current store by product id.")
    public Map<String, Object> getProductDetail(String productId) {
        CatalogService.Product product = catalog.get(productId);
        if (product == null) return Map.of("product_id", productId, "store_id", STORE_ID, "message", "商品不存在");
        return Map.of("product_id", product.id(), "store_id", STORE_ID, "title", product.title(), "price", product.price(), "stock", product.stock());
    }

    @Tool(name = "compare_products", description = "Compare products already returned by the product search tool.")
    public Map<String, Object> compareProducts(List<String> productIds) {
        return Map.of("product_ids", productIds, "store_id", STORE_ID);
    }

    @Tool(name = "get_order_status", description = "Get the latest order status from the durable commerce store.")
    public Map<String, Object> getOrderStatus(String orderId) {
        CatalogService.Order order = catalog.allOrders().stream().filter(item -> item.id().equals(orderId)).findFirst().orElse(null);
        return order == null ? Map.of("order_id", orderId, "status", "NOT_FOUND") : Map.of("order_id", order.id(), "status", order.status(), "shipping_status", order.shippingStatus(), "total", order.total());
    }

    @Tool(name = "get_shipping_status", description = "Get shipping status from the durable commerce store.")
    public Map<String, Object> getShippingStatus(String orderId) {
        CatalogService.Order order = catalog.allOrders().stream().filter(item -> item.id().equals(orderId)).findFirst().orElse(null);
        return order == null ? Map.of("order_id", orderId, "status", "NOT_FOUND") : Map.of("order_id", order.id(), "status", order.shippingStatus());
    }

    @Tool(name = "search_policy", description = "Search return, delivery, payment, warranty and support policies from the indexed store knowledge base.")
    public Map<String, Object> searchPolicy(String query) {
        String needle = query == null ? "" : query.toLowerCase();
        KnowledgeStore.Document document = knowledge.policies(STORE_ID).stream()
                .filter(item -> needle.isBlank() || (item.title() + " " + item.content()).toLowerCase().contains(needle))
                .findFirst()
                .orElse(null);
        if (document == null) return Map.of("source_type", "POLICY", "title", "未找到政策", "content", "当前知识库没有匹配的政策，请转人工客服。", "store_id", STORE_ID);
        return Map.of("source_type", document.sourceType(), "title", document.title(), "content", document.content(), "store_id", document.storeId());
    }

    @Tool(name = "create_support_ticket", description = "Create a support ticket only after explicit confirmation. Never refund automatically.")
    public Map<String, Object> createSupportTicket(String intent, boolean confirmed) {
        if (!confirmed) return Map.of("confirmation_required", true, "intent", intent);
        return Map.of("status", "OPEN", "intent", intent, "message", "工单已提交人工客服审核");
    }
}
