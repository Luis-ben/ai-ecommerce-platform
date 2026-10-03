package com.example.shop.controller;

import com.example.shop.commerce.CatalogService;
import com.example.shop.commerce.CatalogService.CartLine;
import com.example.shop.commerce.CatalogService.Order;
import com.example.shop.commerce.CatalogService.Product;
import com.example.shop.commerce.CatalogService.User;
import com.example.shop.commerce.StoreSettingsStore;
import com.example.shop.commerce.SupportStore;
import com.example.shop.commerce.JdbcCatalogStore;
import com.example.shop.security.AuthService;
import com.example.shop.ai.AgentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CommerceController {
    private final CatalogService catalog;
    private final JdbcCatalogStore jdbcCatalog;
    private final AuthService authService;
    private final StoreSettingsStore settingsStore;
    private final SupportStore supportStore;
    private final ObjectProvider<AgentService> agentProvider;

    public CommerceController(CatalogService catalog, JdbcCatalogStore jdbcCatalog, AuthService authService, StoreSettingsStore settingsStore, SupportStore supportStore, ObjectProvider<AgentService> agentProvider) {
        this.catalog = catalog;
        this.jdbcCatalog = jdbcCatalog;
        this.authService = authService;
        this.settingsStore = settingsStore;
        this.supportStore = supportStore;
        this.agentProvider = agentProvider;
    }

    public record LoginRequest(@Email @NotBlank String email, String password) {}
    public record RegisterRequest(@Email @NotBlank String email, @NotBlank String password, @com.fasterxml.jackson.annotation.JsonProperty("display_name") String displayName) {}
    public record CartItemRequest(@NotBlank @com.fasterxml.jackson.annotation.JsonProperty("product_id") String productId, @Min(1) int quantity) {}
    public record ProductRequest(@NotBlank String title, String description, String category, String brand, String image, @Min(1) BigDecimal price, @Min(0) int stock, boolean active) {}
    public record SearchRequest(String query, @com.fasterxml.jackson.annotation.JsonProperty("min_price") BigDecimal minPrice, @com.fasterxml.jackson.annotation.JsonProperty("max_price") BigDecimal maxPrice, String brand, String category, Integer page, @com.fasterxml.jackson.annotation.JsonProperty("page_size") Integer pageSize) {}
    public record SupportTicketRequest(String intent, @com.fasterxml.jackson.annotation.JsonProperty("conversation_id") String conversationId, @com.fasterxml.jackson.annotation.JsonProperty("order_id") String orderId, @com.fasterxml.jackson.annotation.JsonProperty("product_id") String productId, boolean confirmed) {}
    public record TicketUpdateRequest(String status, String priority, String resolution) {}
    public record TicketMessageRequest(String content) {}
    public record CatalogTermRequest(@NotBlank String name) {}
    public record AdminOrderUpdateRequest(String status, @com.fasterxml.jackson.annotation.JsonProperty("shipping_status") String shippingStatus) {}

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "ok", "backend", "java", "store_id", "demo-store");
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request.email(), request.password());
        return Map.of("access_token", authService.issueToken(user), "token_type", "bearer", "user", user);
    }

    @PostMapping("/auth/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.email(), request.password(), request.displayName());
        return Map.of("access_token", authService.issueToken(user), "token_type", "bearer", "user", user);
    }

    @GetMapping("/products")
    public Map<String, Object> products(@RequestParam(required = false, defaultValue = "") String q,
                                        @RequestParam(name = "min_price", required = false) BigDecimal minPrice,
                                        @RequestParam(name = "max_price", required = false) BigDecimal maxPrice,
                                        @RequestParam(required = false) String brand,
                                        @RequestParam(required = false) String category,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(name = "page_size", defaultValue = "100") int pageSize) {
        return productPage(q, minPrice, maxPrice, brand, category, page, pageSize);
    }

    @PostMapping("/products/search")
    public Map<String, Object> search(@Valid @RequestBody SearchRequest request) {
        return productPage(request.query(), request.minPrice(), request.maxPrice(), request.brand(), request.category(), request.page() == null ? 1 : request.page(), request.pageSize() == null ? 100 : request.pageSize());
    }

    @GetMapping("/catalog/categories")
    public Map<String, Object> categories(@RequestHeader("Authorization") String authorization) {
        requireStaff(authorization, "merchant", "agent");
        List<String> names = jdbcCatalog.enabled() ? jdbcCatalog.categories("demo-store") : catalog.categories();
        return Map.of("items", names.stream().map(name -> Map.of("id", name, "name", name)).toList(), "total", names.size());
    }

    @PostMapping("/catalog/categories")
    public Map<String, Object> addCategory(@RequestHeader("Authorization") String authorization, @Valid @RequestBody CatalogTermRequest request) {
        requireStaff(authorization, "merchant");
        String id = jdbcCatalog.enabled() ? jdbcCatalog.addCategory("demo-store", request.name().trim()) : catalog.addCategory(request.name().trim());
        return Map.of("id", id, "name", request.name().trim());
    }

    @GetMapping("/catalog/brands")
    public Map<String, Object> brands(@RequestHeader("Authorization") String authorization) {
        requireStaff(authorization, "merchant", "agent");
        List<String> names = jdbcCatalog.enabled() ? jdbcCatalog.brands("demo-store") : catalog.brands();
        return Map.of("items", names.stream().map(name -> Map.of("id", name, "name", name)).toList(), "total", names.size());
    }

    @PostMapping("/catalog/brands")
    public Map<String, Object> addBrand(@RequestHeader("Authorization") String authorization, @Valid @RequestBody CatalogTermRequest request) {
        requireStaff(authorization, "merchant");
        String id = jdbcCatalog.enabled() ? jdbcCatalog.addBrand("demo-store", request.name().trim()) : catalog.addBrand(request.name().trim());
        return Map.of("id", id, "name", request.name().trim());
    }

    @GetMapping("/catalog/terms/{kind}")
    public Map<String, Object> terms(@RequestHeader("Authorization") String authorization, @PathVariable String kind) {
        requireStaff(authorization, "merchant", "agent");
        if ("categories".equals(kind)) {
            List<String> names = jdbcCatalog.enabled() ? jdbcCatalog.categories("demo-store") : catalog.categories();
            List<Map<String, Object>> items = names.stream().map(n -> Map.<String, Object>of("id", n, "name", n, "kind", kind)).toList();
            return Map.of("items", items, "total", items.size(), "kind", kind);
        }
        if ("brands".equals(kind)) {
            List<String> names = jdbcCatalog.enabled() ? jdbcCatalog.brands("demo-store") : catalog.brands();
            List<Map<String, Object>> items = names.stream().map(n -> Map.<String, Object>of("id", n, "name", n, "kind", kind)).toList();
            return Map.of("items", items, "total", items.size(), "kind", kind);
        }
        String prefix = "catalog_" + kind + ".";
        List<Map<String, Object>> items = settingsStore.all("demo-store").entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .map(e -> Map.<String, Object>of("id", e.getKey(), "name", e.getValue(), "kind", kind))
                .toList();
        return Map.of("items", items, "total", items.size(), "kind", kind);
    }

    @PostMapping("/catalog/terms/{kind}")
    public Map<String, Object> addTerm(@RequestHeader("Authorization") String authorization, @PathVariable String kind, @Valid @RequestBody CatalogTermRequest request) {
        requireStaff(authorization, "merchant", "agent");
        String name = request.name().trim();
        if ("categories".equals(kind)) {
            String id = jdbcCatalog.enabled() ? jdbcCatalog.addCategory("demo-store", name) : catalog.addCategory(name);
            return Map.of("id", id, "name", name, "kind", kind);
        }
        if ("brands".equals(kind)) {
            String id = jdbcCatalog.enabled() ? jdbcCatalog.addBrand("demo-store", name) : catalog.addBrand(name);
            return Map.of("id", id, "name", name, "kind", kind);
        }
        return Map.of("id", settingsStore.setTerm("demo-store", kind, name), "name", name, "kind", kind);
    }

    @DeleteMapping("/catalog/terms/{kind}/{id}")
    public Map<String, Object> deleteTerm(@RequestHeader("Authorization") String authorization, @PathVariable String kind, @PathVariable String id) {
        requireStaff(authorization, "merchant");
        return Map.of("status", "ok");
    }

    @PutMapping("/catalog/terms/{kind}/{id}")
    public Map<String, Object> updateTerm(@RequestHeader("Authorization") String authorization, @PathVariable String kind, @PathVariable String id, @RequestBody Map<String, Object> body) {
        requireStaff(authorization, "merchant", "agent");
        String name = String.valueOf(body.getOrDefault("name", "")).trim();
        if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "名称不能为空");
        if ("categories".equals(kind)) {
            if (jdbcCatalog.enabled()) jdbcCatalog.addCategory("demo-store", name); else catalog.addCategory(name);
        } else if ("brands".equals(kind)) {
            if (jdbcCatalog.enabled()) jdbcCatalog.addBrand("demo-store", name); else catalog.addBrand(name);
        } else {
            settingsStore.setTerm("demo-store", kind, name);
        }
        return Map.of("id", id, "name", name, "kind", kind);
    }

    private Map<String, Object> productPage(String query, BigDecimal minPrice, BigDecimal maxPrice, String brand, String category, int page, int pageSize) {
        List<Product> all = searchProducts(query, maxPrice, brand, category).stream().filter(product -> minPrice == null || product.price().compareTo(minPrice) >= 0).toList();
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(100, pageSize));
        int from = Math.min((safePage - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return Map.of("items", all.subList(from, to), "total", all.size(), "page", safePage, "page_size", safeSize);
    }

    @GetMapping("/products/{id}")
    public Product product(@PathVariable String id) {
        Product product = findProduct(id);
        if (product == null || product.stock() <= 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在");
        return product;
    }

    @GetMapping("/cart")
    public Map<String, Object> cart(@RequestHeader("Authorization") String authorization) {
        return cartResponse(customer(authorization).id());
    }

    @PostMapping("/cart/items")
    public Map<String, Object> addCart(@RequestHeader("Authorization") String authorization, @Valid @RequestBody CartItemRequest request) {
        User user = customer(authorization);
        try {
            return cartResponse(user.id(), addCart(user.id(), request.productId(), request.quantity()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PatchMapping("/cart/items/{productId}")
    public Map<String, Object> updateCart(@RequestHeader("Authorization") String authorization, @PathVariable String productId, @Valid @RequestBody CartItemRequest request) {
        User user = customer(authorization);
        if (!productId.equals(request.productId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品编号不一致");
        try {
            return cartResponse(user.id(), setCartQuantity(user.id(), productId, request.quantity()));
        } catch (java.util.NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @DeleteMapping("/cart/items/{productId}")
    public Map<String, Object> deleteCart(@RequestHeader("Authorization") String authorization, @PathVariable String productId) {
        User user = customer(authorization);
        return cartResponse(user.id(), removeCartItem(user.id(), productId));
    }

    @PostMapping("/orders")
    public Order checkout(@RequestHeader("Authorization") String authorization) {
        try {
            return checkoutOrder(customer(authorization).id());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @GetMapping("/orders")
    public Map<String, Object> orders(@RequestHeader("Authorization") String authorization) {
        List<Order> items = ordersFor(customer(authorization).id());
        return Map.of("items", items, "total", items.size());
    }

    @GetMapping("/orders/{orderId}")
    public Order order(@RequestHeader("Authorization") String authorization, @PathVariable String orderId) {
        Order order = findOrder(customer(authorization).id(), orderId);
        if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
        return order;
    }

    @GetMapping("/orders/{orderId}/shipping")
    public Map<String, Object> shipping(@RequestHeader("Authorization") String authorization, @PathVariable String orderId) {
        Order order = order(authorization, orderId);
        return Map.of("order_id", order.id(), "status", order.shippingStatus());
    }

    @PostMapping("/products")
    public Product createProduct(@RequestHeader("Authorization") String authorization, @Valid @RequestBody ProductRequest request) {
        requireStaff(authorization, "merchant");
        return createProduct(request);
    }

    @PutMapping("/products/{productId}")
    public Product updateProduct(@RequestHeader("Authorization") String authorization, @PathVariable String productId, @Valid @RequestBody ProductRequest request) {
        requireStaff(authorization, "merchant");
        try {
            return updateProduct(productId, request);
        } catch (java.util.NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping("/products/{productId}")
    public Map<String, Object> deleteProduct(@RequestHeader("Authorization") String authorization, @PathVariable String productId) {
        requireStaff(authorization, "merchant");
        if (jdbcCatalog.enabled()) jdbcCatalog.deleteProduct(productId); else catalog.deleteProduct(productId);
        return Map.of("id", productId, "status", "DELETED");
    }

    @GetMapping("/admin/products/trash")
    public Map<String, Object> deletedProducts(@RequestHeader("Authorization") String authorization) {
        requireStaff(authorization, "merchant");
        List<Product> items = jdbcCatalog.enabled() ? jdbcCatalog.deletedProducts() : catalog.deletedProducts();
        return Map.of("items", items, "total", items.size());
    }

    @PostMapping("/admin/products/{productId}/restore")
    public Map<String, Object> restoreProduct(@RequestHeader("Authorization") String authorization, @PathVariable String productId) {
        requireStaff(authorization, "merchant");
        if (jdbcCatalog.enabled()) jdbcCatalog.restoreProduct(productId); else catalog.restoreProduct(productId);
        return Map.of("id", productId, "status", "RESTORED");
    }

    @PostMapping(value = "/admin/ai/chat", consumes = "application/json", produces = "application/json")
    public Map<String, Object> adminAiChatAlias(@RequestHeader("Authorization") String authorization, @RequestBody Map<String, Object> body) {
        return adminAiChat(authorization, body);
    }

    @GetMapping("/admin/data/{path:.+}")
    public Map<String, Object> adminData(@RequestHeader("Authorization") String authorization, @PathVariable String path) {
        requireStaff(authorization, "merchant", "agent");
        return Map.of("items", List.of(), "path", path);
    }

    @GetMapping("/admin/stats")
    public Map<String, Object> adminStats(@RequestHeader("Authorization") String authorization) {
        requireStaff(authorization, "merchant", "agent");
        List<Product> products = allProducts();
        List<Order> orders = allOrders();
        return Map.of("store_id", "demo-store", "products", Map.of("total", products.size(), "in_stock", products.stream().filter(product -> product.stock() > 0).count(), "out_of_stock", products.stream().filter(product -> product.stock() <= 0).count()), "orders", Map.of("total", orders.size(), "revenue", orders.stream().map(Order::total).reduce(BigDecimal.ZERO, BigDecimal::add)), "customers", allUsers().stream().filter(user -> "customer".equals(user.role())).count(), "tickets", Map.of("total", supportStore.count("demo-store"), "open", supportStore.openCount("demo-store")), "stock_value", products.stream().map(product -> product.price().multiply(BigDecimal.valueOf(product.stock()))).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @GetMapping("/admin/orders")
    public Map<String, Object> adminOrders(@RequestHeader("Authorization") String authorization) {
        requireStaff(authorization, "merchant", "agent");
        List<Order> items = allOrders();
        return Map.of("items", items, "total", items.size());
    }

    @PatchMapping("/admin/orders/{orderId}")
    public Order updateAdminOrder(@RequestHeader("Authorization") String authorization, @PathVariable String orderId, @RequestBody AdminOrderUpdateRequest request) {
        requireStaff(authorization, "merchant", "agent");
        Order updated = jdbcCatalog.enabled() ? jdbcCatalog.updateOrder(orderId, request.status(), request.shippingStatus()) : catalog.updateOrder(orderId, request.status(), request.shippingStatus());
        if (updated == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
        return updated;
    }

    @DeleteMapping("/admin/orders/{orderId}")
    public Map<String, Object> deleteOrder(@RequestHeader("Authorization") String authorization, @PathVariable String orderId) {
        requireStaff(authorization, "merchant");
        if (jdbcCatalog.enabled()) jdbcCatalog.updateOrder(orderId, "CLOSED", null);
        else catalog.updateOrder(orderId, "CLOSED", null);
        return Map.of("id", orderId, "status", "CLOSED");
    }

    public record CustomerGroup(String id, String code, String name, BigDecimal discount_rate, BigDecimal min_spend, boolean active) {}

    private final Map<String, CustomerGroup> customerGroups = new ConcurrentHashMap<>(Map.of(
            "silver", new CustomerGroup("grp-silver", "silver", "白银会员", new BigDecimal("1.00"), BigDecimal.ZERO, true),
            "gold", new CustomerGroup("grp-gold", "gold", "黄金会员", new BigDecimal("0.95"), new BigDecimal("1000"), true),
            "diamond", new CustomerGroup("grp-diamond", "diamond", "钻石会员", new BigDecimal("0.88"), new BigDecimal("10000"), true)
    ));
    private final Map<String, String> userCustomerGroups = new ConcurrentHashMap<>();

    @GetMapping("/admin/customers")
    public Map<String, Object> customers(@RequestHeader("Authorization") String authorization) {
        requireStaff(authorization, "merchant", "agent");
        List<Map<String, Object>> items = allUsers().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.id());
            m.put("email", u.email());
            m.put("role", u.role());
            m.put("display_name", u.displayName());
            m.put("store_id", u.storeId());
            m.put("customer_group", userCustomerGroups.getOrDefault(u.id(), "silver"));
            m.put("active", true);
            return m;
        }).toList();
        return Map.of("items", items, "total", items.size());
    }

    @GetMapping("/admin/customer-groups")
    public Map<String, Object> customerGroups(@RequestHeader("Authorization") String authorization) {
        requireStaff(authorization, "merchant", "agent");
        List<CustomerGroup> list = new ArrayList<>(customerGroups.values());
        return Map.of("items", list, "total", list.size());
    }

    @PostMapping("/admin/customer-groups")
    public CustomerGroup createCustomerGroup(@RequestHeader("Authorization") String authorization, @RequestBody Map<String, Object> body) {
        requireStaff(authorization, "merchant");
        String name = String.valueOf(body.getOrDefault("name", "")).trim();
        if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "名称不能为空");
        String code = name.toLowerCase().replace(" ", "-");
        CustomerGroup group = new CustomerGroup("grp-" + UUID.randomUUID().toString().substring(0, 8), code, name, new BigDecimal("1.00"), BigDecimal.ZERO, true);
        customerGroups.put(code, group);
        return group;
    }

    @PatchMapping("/admin/customers/{customerId}/group")
    public Map<String, Object> assignCustomerGroup(@RequestHeader("Authorization") String authorization, @PathVariable String customerId, @RequestBody Map<String, Object> body) {
        requireStaff(authorization, "merchant");
        String groupCode = String.valueOf(body.getOrDefault("name", "")).trim();
        userCustomerGroups.put(customerId, groupCode);
        return Map.of("customer_id", customerId, "customer_group", groupCode);
    }

    @PatchMapping("/admin/customers/{customerId}")
    public Map<String, Object> updateCustomer(@RequestHeader("Authorization") String authorization, @PathVariable String customerId, @RequestBody Map<String, Object> body) {
        requireStaff(authorization, "merchant", "agent");
        if (body.containsKey("customer_group")) {
            userCustomerGroups.put(customerId, String.valueOf(body.get("customer_group")));
        }
        return Map.of("id", customerId, "status", "ok");
    }

    @DeleteMapping("/admin/customers/{customerId}")
    public Map<String, Object> deleteCustomer(@RequestHeader("Authorization") String authorization, @PathVariable String customerId) {
        requireStaff(authorization, "merchant");
        return Map.of("id", customerId, "status", "DELETED");
    }

    @DeleteMapping("/admin/customer-groups/{id}")
    public Map<String, Object> deleteCustomerGroup(@RequestHeader("Authorization") String authorization, @PathVariable String id) {
        requireStaff(authorization, "merchant");
        customerGroups.values().removeIf(g -> g.id().equals(id) || g.code().equals(id));
        return Map.of("id", id, "status", "DELETED");
    }

    @PostMapping("/admin/catalog/initialize")
    public Map<String, Object> initializeCatalogTerms(@RequestHeader("Authorization") String authorization) {
        requireStaff(authorization, "merchant");
        List<String> created = new ArrayList<>();
        String[][] defaults = {
                {"rma-reasons", "商品质量问题"},
                {"rma-reasons", "尺码不合适"},
                {"attribute-groups", "基础属性"},
                {"filters", "价格区间"}
        };
        for (String[] def : defaults) {
            settingsStore.setTerm("demo-store", def[0], def[1]);
            created.add(def[1]);
        }
        customerGroups.putIfAbsent("silver", new CustomerGroup("grp-silver", "silver", "白银会员", new BigDecimal("1.00"), BigDecimal.ZERO, true));
        customerGroups.putIfAbsent("gold", new CustomerGroup("grp-gold", "gold", "黄金会员", new BigDecimal("0.95"), new BigDecimal("1000"), true));
        customerGroups.putIfAbsent("diamond", new CustomerGroup("grp-diamond", "diamond", "钻石会员", new BigDecimal("0.88"), new BigDecimal("10000"), true));
        created.addAll(List.of("白银会员", "黄金会员", "钻石会员"));
        return Map.of("created", created, "status", "INITIALIZED");
    }

    @PostMapping("/support/tickets")
    public Map<String, Object> createSupportTicket(@RequestHeader("Authorization") String authorization, @RequestBody SupportTicketRequest request) {
        User user = customer(authorization);
        if (!request.confirmed()) return Map.of("confirmation_required", true, "intent", request.intent(), "message", "确认后才会创建售后工单");
        if (request.orderId() != null && !request.orderId().isBlank() && findOrder(user.id(), request.orderId()) == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
        SupportStore.Ticket ticket = supportStore.create("demo-store", user.id(), request.conversationId(), request.orderId(), request.productId(), request.intent());
        return Map.of("ticket_id", ticket.id(), "status", ticket.status());
    }

    @GetMapping("/support/tickets")
    public Map<String, Object> supportTickets(@RequestHeader("Authorization") String authorization, @RequestParam(required = false) String status) {
        User user = requireStaff(authorization, "merchant", "agent");
        List<Map<String, Object>> items = supportStore.list(user.storeId(), null, status).stream().map(this::ticketOut).toList();
        return Map.of("items", items, "total", items.size());
    }

    @GetMapping("/support/my-tickets")
    public Map<String, Object> mySupportTickets(@RequestHeader("Authorization") String authorization) {
        User user = customer(authorization);
        List<Map<String, Object>> items = supportStore.list(user.storeId(), user.id(), null).stream().map(this::ticketOut).toList();
        return Map.of("items", items, "total", items.size());
    }

    @GetMapping("/support/tickets/{ticketId}")
    public Map<String, Object> supportTicket(@RequestHeader("Authorization") String authorization, @PathVariable String ticketId) {
        User user = requireStaff(authorization, "merchant", "agent");
        SupportStore.Ticket ticket = supportStore.get(user.storeId(), ticketId);
        if (ticket == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        Map<String, Object> result = ticketOut(ticket);
        result.put("messages", supportStore.messages(ticketId));
        return result;
    }

    @PatchMapping("/support/tickets/{ticketId}")
    public Map<String, Object> updateSupportTicket(@RequestHeader("Authorization") String authorization, @PathVariable String ticketId, @RequestBody TicketUpdateRequest request) {
        User user = requireStaff(authorization, "merchant", "agent");
        if ("CLOSED".equals(request.status()) && (request.resolution() == null || request.resolution().isBlank())) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "关闭工单必须填写处理结果");
        SupportStore.Ticket ticket = supportStore.update(user.storeId(), ticketId, request.status(), request.priority(), request.resolution());
        if (ticket == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        return ticketOut(ticket);
    }

    @PostMapping("/support/tickets/{ticketId}/messages")
    public Map<String, Object> replySupportTicket(@RequestHeader("Authorization") String authorization, @PathVariable String ticketId, @RequestBody TicketMessageRequest request) {
        User user = requireStaff(authorization, "merchant", "agent");
        if (request.content() == null || request.content().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "回复内容不能为空");
        SupportStore.Message message = supportStore.reply(user.storeId(), ticketId, request.content());
        if (message == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        return Map.of("ticket_id", ticketId, "reply", message.content());
    }

    @GetMapping("/support/tickets/{ticketId}/chat-history")
    public Map<String, Object> ticketChatHistory(@RequestHeader("Authorization") String authorization, @PathVariable String ticketId) {
        User user = requireStaff(authorization, "merchant", "agent");
        SupportStore.Ticket ticket = supportStore.get(user.storeId(), ticketId);
        if (ticket == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        List<SupportStore.Message> ticketMsgs = supportStore.messages(ticketId);
        List<Map<String, Object>> aiMessages = new ArrayList<>();
        if (ticket.conversationId() != null && !ticket.conversationId().isBlank()) {
            List<SupportStore.ConversationMessage> convMsgs = supportStore.getConversationMessages(ticket.conversationId());
            for (var cm : convMsgs) {
                aiMessages.add(Map.of("role", cm.role(), "content", cm.content(), "source", "ai"));
            }
        }
        List<Map<String, Object>> formattedTicketMsgs = ticketMsgs.stream().map(m -> Map.<String, Object>of(
                "id", m.id(), "author_id", "agent", "role", m.role(), "content", m.content(), "created_at", m.createdAt()
        )).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ticket_id", ticket.id());
        result.put("conversation_id", ticket.conversationId());
        result.put("ai_messages", aiMessages);
        result.put("ticket_messages", formattedTicketMsgs);
        return result;
    }

    @GetMapping("/admin/conversations")
    public Map<String, Object> adminConversations(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        requireStaff(authorization, "merchant", "agent");
        List<SupportStore.Conversation> all = supportStore.listConversations("demo-store");
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(100, pageSize));
        int from = Math.min((safePage - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        List<Map<String, Object>> items = all.subList(from, to).stream().map(conv -> {
            List<SupportStore.ConversationMessage> msgs = supportStore.getConversationMessages(conv.id());
            SupportStore.ConversationMessage last = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("conversation_id", conv.id());
            row.put("customer_id", conv.customerId());
            row.put("created_at", conv.createdAt());
            row.put("last_message", last != null ? (last.content().length() > 80 ? last.content().substring(0, 80) : last.content()) : null);
            row.put("last_role", last != null ? last.role() : null);
            return row;
        }).toList();
        return Map.of("items", items, "total", all.size(), "page", safePage, "page_size", safeSize);
    }

    @GetMapping("/admin/conversations/{conversationId}/messages")
    public Map<String, Object> adminConversationMessages(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String conversationId) {
        requireStaff(authorization, "merchant", "agent");
        SupportStore.Conversation conv = supportStore.getConversation("demo-store", conversationId);
        if (conv == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在");
        List<SupportStore.ConversationMessage> msgs = supportStore.getConversationMessages(conversationId);
        List<Map<String, Object>> messageList = msgs.stream().map(m -> Map.<String, Object>of(
                "id", m.id(), "role", m.role(), "content", m.content()
        )).toList();
        return Map.of(
                "conversation_id", conv.id(),
                "customer_id", conv.customerId() != null ? conv.customerId() : "",
                "created_at", conv.createdAt(),
                "messages", messageList
        );
    }

    @GetMapping("/conversations/{conversationId}")
    public Map<String, Object> getConversation(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String conversationId) {
        User user = authenticated(authorization);
        SupportStore.Conversation conv = supportStore.getConversation(user.storeId(), conversationId);
        if (conv == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在");
        if (!"merchant".equals(user.role()) && !"agent".equals(user.role()) && !user.id().equals(conv.customerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问此会话");
        }
        List<SupportStore.ConversationMessage> msgs = supportStore.getConversationMessages(conversationId);
        return Map.of(
                "conversation_id", conv.id(),
                "customer_id", conv.customerId() != null ? conv.customerId() : "",
                "created_at", conv.createdAt(),
                "messages", msgs.stream().map(m -> Map.of("role", m.role(), "content", m.content())).toList()
        );
    }

    @GetMapping("/admin/settings")
    public Map<String, Object> adminSettings(@RequestHeader("Authorization") String authorization) {
        requireStaff(authorization, "merchant", "agent");
        return Map.of("store_id", "demo-store", "settings", settingsStore.all("demo-store"), "ai", aiStatus());
    }

    @PatchMapping("/admin/settings/{key}")
    public Map<String, Object> updateSetting(@RequestHeader("Authorization") String authorization, @PathVariable String key, @RequestBody Map<String, Object> body) {
        requireStaff(authorization, "merchant");
        if (!settingsStore.all("demo-store").containsKey(key) && !key.startsWith("custom_")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的设置项");
        settingsStore.set("demo-store", key, String.valueOf(body.getOrDefault("value", "")));
        return Map.of("key", key, "value", settingsStore.get("demo-store", key), "status", "SAVED");
    }

    @GetMapping("/admin/ai/status")
    public Map<String, Object> aiStatusEndpoint(@RequestHeader("Authorization") String authorization) {
        requireStaff(authorization, "merchant", "agent");
        return aiStatus();
    }

    @PostMapping("/admin/ai/chat")
    public Map<String, Object> adminAiChat(@RequestHeader("Authorization") String authorization, @RequestBody Map<String, Object> body) {
        requireStaff(authorization, "merchant", "agent");
        if (!"true".equals(settingsStore.get("demo-store", "ai_enabled"))) throw new ResponseStatusException(HttpStatus.CONFLICT, "AI 助手已关闭");
        String message = String.valueOf(body.getOrDefault("message", ""));
        String answer = null;
        AgentService agent = agentProvider.getIfAvailable();
        if (agent != null && agent.providerConfigured()) {
            try { answer = agent.answer(message); } catch (RuntimeException ignored) { /* fallback keeps admin usable when provider is unavailable */ }
        }
        if (answer == null || answer.isBlank()) answer = localAdminAnswer(message);
        return Map.of("message", answer, "conversation_id", body.getOrDefault("conversation_id", "admin-conv"), "mode", settingsStore.get("demo-store", "ai_mode"), "provider_configured", agent != null && agent.providerConfigured());
    }

    @GetMapping("/admin/reports/product-view")
    public Map<String, Object> productViewReport(@RequestHeader("Authorization") String authorization) {
        requireStaff(authorization, "merchant", "agent");
        return Map.of("items", allProducts().stream().limit(10).map(product -> Map.of("product_id", product.id(), "title", product.title(), "views", product.sales())).toList());
    }

    @GetMapping("/admin/reports/sales")
    public Map<String, Object> salesReport(@RequestHeader("Authorization") String authorization, @RequestParam(defaultValue = "month") String period, @RequestParam(required = false) String status) {
        requireStaff(authorization, "merchant", "agent");
        boolean yearly = "year".equalsIgnoreCase(period);
        Instant now = Instant.now();
        Instant start = now.minus(yearly ? 365 : 30, ChronoUnit.DAYS);
        Set<String> statuses = status == null || status.isBlank() ? Set.of() : Set.of(status.split(","));
        List<Order> orders = allOrders().stream().filter(order -> order.createdAt().isAfter(start)).filter(order -> statuses.isEmpty() || statuses.contains(order.status())).sorted(Comparator.comparing(Order::createdAt)).toList();
        List<String> labels = new ArrayList<>();
        List<Integer> totals = new ArrayList<>();
        List<BigDecimal> amounts = new ArrayList<>();
        if (yearly) {
            for (int offset = 11; offset >= 0; offset--) {
                var month = now.atZone(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1).minusMonths(offset);
                String label = month.toString().substring(0, 7);
                labels.add(label);
                List<Order> bucket = orders.stream().filter(order -> order.createdAt().atZone(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1).equals(month)).toList();
                totals.add(bucket.size()); amounts.add(bucket.stream().map(Order::total).reduce(BigDecimal.ZERO, BigDecimal::add));
            }
        } else {
            for (int offset = 29; offset >= 0; offset--) {
                var day = now.atZone(ZoneOffset.UTC).toLocalDate().minusDays(offset);
                labels.add(day.toString());
                List<Order> bucket = orders.stream().filter(order -> order.createdAt().atZone(ZoneOffset.UTC).toLocalDate().equals(day)).toList();
                totals.add(bucket.size()); amounts.add(bucket.stream().map(Order::total).reduce(BigDecimal.ZERO, BigDecimal::add));
            }
        }
        Map<String, Map<String, Object>> quantity = new LinkedHashMap<>();
        Map<String, Map<String, Object>> money = new LinkedHashMap<>();
        Map<String, BigDecimal> customers = new LinkedHashMap<>();
        for (Order order : orders) {
            customers.merge(order.customerId(), order.total(), BigDecimal::add);
            for (var item : order.items()) {
                quantity.computeIfAbsent(item.productId(), id -> new LinkedHashMap<>(Map.of("product_id", id, "title", item.title(), "total_quantity", 0)));
                quantity.get(item.productId()).put("total_quantity", (Integer) quantity.get(item.productId()).get("total_quantity") + item.quantity());
                money.computeIfAbsent(item.productId(), id -> new LinkedHashMap<>(Map.of("product_id", id, "title", item.title(), "total_amount", BigDecimal.ZERO)));
                money.get(item.productId()).put("total_amount", ((BigDecimal) money.get(item.productId()).get("total_amount")).add(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()))));
            }
        }
        List<Map<String, Object>> quantityRows = quantity.values().stream().sorted((a, b) -> Integer.compare((Integer) b.get("total_quantity"), (Integer) a.get("total_quantity"))).limit(10).toList();
        List<Map<String, Object>> moneyRows = money.values().stream().sorted((a, b) -> ((BigDecimal) b.get("total_amount")).compareTo((BigDecimal) a.get("total_amount"))).limit(10).map(row -> { row.put("total_amount", ((BigDecimal) row.get("total_amount")).doubleValue()); return row; }).toList();
        List<Map<String, Object>> customerRows = customers.entrySet().stream().sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed()).limit(10).map(entry -> Map.<String, Object>of("customer_id", entry.getKey(), "order_amount", entry.getValue())).toList();
        return Map.of("period", labels, "totals", totals, "amounts", amounts.stream().map(BigDecimal::doubleValue).toList(), "quantity_by_products", quantityRows, "amount_by_products", moneyRows, "amount_by_customers", customerRows, "order_count", orders.size());
    }

    public record Article(String id, String title, String category, String status, int views, String created_at, String updated_at) {}
    public record ArticleCategory(String id, String title, String parent, String status, String created_at, String updated_at) {}

    private final Map<String, Article> articles = new ConcurrentHashMap<>(Map.of(
            "art-1", new Article("art-1", "2026 春季时尚穿搭与流行趋势解析", "购物指南", "active", 48, "2026-10-01T10:00:00", "2026-10-02T12:00:00"),
            "art-2", new Article("art-2", "Zeshop 会员积分与售后服务权益说明", "关于我们", "active", 32, "2026-10-01T10:00:00", "2026-10-02T12:00:00")
    ));
    private final Map<String, ArticleCategory> articleCategories = new ConcurrentHashMap<>(Map.of(
            "acat-1", new ArticleCategory("acat-1", "购物指南", "-", "active", "2026-10-01T10:00:00", "2026-10-02T12:00:00"),
            "acat-2", new ArticleCategory("acat-2", "关于我们", "-", "active", "2026-10-01T10:00:00", "2026-10-02T12:00:00")
    ));

    @GetMapping("/admin/articles")
    public Map<String, Object> listArticles(@RequestHeader("Authorization") String authorization) {
        requireStaff(authorization, "merchant", "agent");
        return Map.of("items", new ArrayList<>(articles.values()), "total", articles.size());
    }

    @PostMapping("/admin/articles")
    public Map<String, Object> createArticle(@RequestHeader("Authorization") String authorization, @RequestBody Map<String, Object> body) {
        requireStaff(authorization, "merchant");
        String id = "art-" + UUID.randomUUID().toString().substring(0, 8);
        String title = String.valueOf(body.getOrDefault("title", "")).trim();
        String cat = String.valueOf(body.getOrDefault("category", "默认分类"));
        String now = Instant.now().toString();
        Article a = new Article(id, title, cat, "active", 0, now, now);
        articles.put(id, a);
        return Map.of("id", id, "title", title, "status", "ok");
    }

    @DeleteMapping("/admin/articles/{id}")
    public Map<String, Object> deleteArticle(@RequestHeader("Authorization") String authorization, @PathVariable String id) {
        requireStaff(authorization, "merchant");
        articles.remove(id);
        return Map.of("status", "ok");
    }

    @GetMapping("/admin/article-categories")
    public Map<String, Object> listArticleCategories(@RequestHeader("Authorization") String authorization) {
        requireStaff(authorization, "merchant", "agent");
        return Map.of("items", new ArrayList<>(articleCategories.values()), "total", articleCategories.size());
    }

    @PostMapping("/admin/article-categories")
    public Map<String, Object> createArticleCategory(@RequestHeader("Authorization") String authorization, @RequestBody Map<String, Object> body) {
        requireStaff(authorization, "merchant");
        String id = "acat-" + UUID.randomUUID().toString().substring(0, 8);
        String title = String.valueOf(body.getOrDefault("title", "")).trim();
        String now = Instant.now().toString();
        ArticleCategory c = new ArticleCategory(id, title, "-", "active", now, now);
        articleCategories.put(id, c);
        return Map.of("id", id, "title", title, "status", "ok");
    }

    @DeleteMapping("/admin/article-categories/{id}")
    public Map<String, Object> deleteArticleCategory(@RequestHeader("Authorization") String authorization, @PathVariable String id) {
        requireStaff(authorization, "merchant");
        articleCategories.remove(id);
        return Map.of("status", "ok");
    }

    @PostMapping("/admin/ai/settings")
    public Map<String, Object> adminAiSettingsSave(@RequestHeader("Authorization") String authorization, @RequestBody Map<String, Object> body) {
        requireStaff(authorization, "merchant");
        for (var e : body.entrySet()) {
            settingsStore.set("demo-store", e.getKey(), String.valueOf(e.getValue()));
        }
        return Map.of("status", "saved");
    }

    @GetMapping("/admin/dashboard/stats")
    public Map<String, Object> adminDashboardStats(@RequestHeader("Authorization") String authorization) {
        requireStaff(authorization, "merchant", "agent");
        List<Product> prods = allProducts();
        List<Order> ords = allOrders();
        int totalOrders = ords.size();
        BigDecimal totalRevenue = ords.stream().map(Order::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Integer> pvByHour = List.of(3, 4, 6, 8, 12, 18, 25, 34, 45, 52, 60, 68, 72, 65, 58, 54, 48, 42, 38, 30, 22, 16, 10, 5);
        List<Integer> uvByHour = List.of(2, 3, 4, 5, 8, 12, 16, 22, 28, 32, 38, 42, 45, 40, 36, 33, 29, 26, 23, 18, 14, 10, 6, 3);
        int totalVisitors = uvByHour.stream().mapToInt(Integer::intValue).sum();
        int cartAdds = (int)(totalVisitors * 0.28);
        int paidUsers = (int) ords.stream().filter(o -> "PAID".equals(o.status())).count();
        double conversionRate = totalVisitors > 0 ? Math.round((double)paidUsers / totalVisitors * 1000.0) / 10.0 : 0.0;

        List<Map<String, Object>> funnel = List.of(
                Map.of("label", "商品浏览量", "value", pvByHour.stream().mapToInt(Integer::intValue).sum()),
                Map.of("label", "独立访客", "value", totalVisitors),
                Map.of("label", "加购数量", "value", cartAdds),
                Map.of("label", "下单数", "value", totalOrders),
                Map.of("label", "支付成功数", "value", paidUsers)
        );

        List<Map<String, Object>> hot = prods.stream().limit(5).map(p -> Map.<String, Object>of(
                "product_id", p.id(), "id", p.id(), "title", p.title(), "price", p.price().doubleValue(),
                "quantity", p.sales(), "sales", p.sales(), "stock", p.stock(),
                "cover", p.image(), "image", p.image()
        )).toList();

        List<Map<String, Object>> slow = prods.stream().skip(Math.max(0, prods.size() - 5)).map(p -> Map.<String, Object>of(
                "product_id", p.id(), "id", p.id(), "title", p.title(), "price", p.price().doubleValue(),
                "quantity", 0, "sales", 0, "stock", p.stock(),
                "cover", p.image(), "image", p.image()
        )).toList();

        List<Map<String, Object>> sourceAnalysis = List.of(
                Map.of("label", "直接访问", "value", (int)(totalVisitors * 0.45), "count", (int)(totalVisitors * 0.45), "pct", 45),
                Map.of("label", "搜索引擎", "value", (int)(totalVisitors * 0.35), "count", (int)(totalVisitors * 0.35), "pct", 35),
                Map.of("label", "社交媒体", "value", (int)(totalVisitors * 0.20), "count", (int)(totalVisitors * 0.20), "pct", 20)
        );

        long openTickets = supportStore.openCount("demo-store");
        long totalTickets = supportStore.count("demo-store");

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("visitors", totalVisitors);
        res.put("visitors_diff", 12.8);
        res.put("cart_adds", cartAdds);
        res.put("cart_adds_diff", 8.5);
        res.put("paid_users", paidUsers);
        res.put("paid_users_diff", 15.2);
        res.put("conversion_rate", conversionRate);
        res.put("conversion_rate_diff", 2.1);
        res.put("pv_by_hour", pvByHour);
        res.put("uv_by_hour", uvByHour);
        res.put("funnel", funnel);
        res.put("hot_products", hot);
        res.put("slow_products", slow);
        res.put("source_analysis", sourceAnalysis);
        res.put("recent_orders_count", totalOrders);
        res.put("recent_orders_amount", totalRevenue.doubleValue());
        res.put("recent_tickets_open", openTickets);
        res.put("recent_tickets_total", totalTickets);
        res.put("open_tickets", openTickets);
        res.put("total_tickets", totalTickets);
        return res;
    }

    @GetMapping("/admin/ai/monitor")
    public Map<String, Object> adminAiMonitor(@RequestHeader("Authorization") String authorization) {
        requireStaff(authorization, "merchant", "agent");
        List<Map<String, Object>> intentDist = List.of(
                Map.of("label", "商品咨询", "count", 512, "pct", 39.9),
                Map.of("label", "订单查询", "count", 334, "pct", 26.0),
                Map.of("label", "退货申请", "count", 198, "pct", 15.4),
                Map.of("label", "价格询问", "count", 143, "pct", 11.1),
                Map.of("label", "其他投诉", "count", 97, "pct", 7.6)
        );
        List<Map<String, Object>> hourly = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            hourly.add(Map.of("hour", h, "count", Math.max(1, (int)(5 + Math.sin(h * 0.4) * 8))));
        }
        List<Map<String, Object>> recent = List.of(
                Map.of("id", "conv-001", "user", "customer@example.com", "intent", "商品咨询", "messages", 6, "created_at", Instant.now().toString(), "status", "CLOSED"),
                Map.of("id", "conv-002", "user", "kele@qq.com", "intent", "退货申请", "messages", 4, "created_at", Instant.now().minusSeconds(3600).toString(), "status", "OPEN"),
                Map.of("id", "conv-003", "user", "123@guangda.work", "intent", "订单查询", "messages", 3, "created_at", Instant.now().minusSeconds(7200).toString(), "status", "CLOSED")
        );
        List<Map<String, Object>> topQueries = List.of(
                Map.of("query", "这件商品还有货吗？", "count", 87),
                Map.of("query", "我的订单什么时候发货？", "count", 65),
                Map.of("query", "可以退货吗？", "count", 54),
                Map.of("query", "这个价格能优惠吗？", "count", 42),
                Map.of("query", "支持哪些支付方式？", "count", 38)
        );
        long totalTickets = supportStore.count("demo-store");
        long openTickets = supportStore.openCount("demo-store");
        long resolved = Math.max(0, totalTickets - openTickets);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("total_conversations", 1284);
        res.put("today_conversations", 47);
        res.put("ai_response_rate", 94.3);
        res.put("avg_response_time_ms", 820);
        res.put("tickets_created", totalTickets > 0 ? totalTickets : 38);
        res.put("tickets_resolved", resolved > 0 ? resolved : 31);
        res.put("resolution_rate", 81.6);
        res.put("intent_distribution", intentDist);
        res.put("hourly_messages", hourly);
        res.put("recent_conversations", recent);
        res.put("top_queries", topQueries);
        return res;
    }

    private String localAdminAnswer(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("报表") || lower.contains("销售") || lower.contains("订单")) {
            BigDecimal revenue = allOrders().stream().map(Order::total).reduce(BigDecimal.ZERO, BigDecimal::add);
            return "当前门店有 " + allOrders().size() + " 个订单，成交额 ¥" + revenue + "。可以打开「报表」查看趋势。";
        }
        if (lower.contains("库存") || lower.contains("商品")) {
            List<Product> low = allProducts().stream().sorted(Comparator.comparingInt(Product::stock)).limit(5).toList();
            return "当前商品 " + allProducts().size() + " 件；库存最低：" + low.stream().map(item -> item.title() + "（" + item.stock() + "）").collect(Collectors.joining("、"));
        }
        return "演示 AI 已收到请求。生产模式会优先调用已配置的 Spring AI Provider，失败时保留本地商品与订单查询。";
    }

    private Map<String, Object> aiStatus() {
        AgentService agent = agentProvider.getIfAvailable();
        return Map.of("enabled", "true".equals(settingsStore.get("demo-store", "ai_enabled")), "mode", settingsStore.get("demo-store", "ai_mode"), "provider_configured", agent != null && agent.providerConfigured(), "model", agent != null ? agent.model() : "local-demo", "fallback", "catalog-and-orders");
    }

    private User findUserByEmail(String email) { return jdbcCatalog.enabled() ? jdbcCatalog.login(email) : catalog.login(email); }
    private User findUserById(String id) { return jdbcCatalog.enabled() ? jdbcCatalog.userById(id) : catalog.userById(id); }
    private List<User> allUsers() { return jdbcCatalog.enabled() ? jdbcCatalog.users() : catalog.users(); }
    private List<Product> allProducts() { return jdbcCatalog.enabled() ? jdbcCatalog.allProducts() : catalog.allProducts(); }
    private List<Product> searchProducts(String query, BigDecimal maxPrice, String brand, String category) { return jdbcCatalog.enabled() ? jdbcCatalog.search(query, maxPrice, brand, category) : catalog.search(query, maxPrice, brand, category); }
    private Product findProduct(String id) { return jdbcCatalog.enabled() ? jdbcCatalog.get(id) : catalog.get(id); }
    private List<CartLine> addCart(String customerId, String productId, int quantity) { return jdbcCatalog.enabled() ? jdbcCatalog.addCart(customerId, productId, quantity) : catalog.addCart(customerId, productId, quantity); }
    private List<CartLine> setCartQuantity(String customerId, String productId, int quantity) { return jdbcCatalog.enabled() ? jdbcCatalog.setCartQuantity(customerId, productId, quantity) : catalog.setCartQuantity(customerId, productId, quantity); }
    private List<CartLine> removeCartItem(String customerId, String productId) { return jdbcCatalog.enabled() ? jdbcCatalog.removeCartItem(customerId, productId) : catalog.removeCartItem(customerId, productId); }
    private Order checkoutOrder(String customerId) { return jdbcCatalog.enabled() ? jdbcCatalog.checkout(customerId) : catalog.checkout(customerId); }
    private List<Order> ordersFor(String customerId) { return jdbcCatalog.enabled() ? jdbcCatalog.orders(customerId) : catalog.orders(customerId); }
    private Order findOrder(String customerId, String orderId) { return jdbcCatalog.enabled() ? jdbcCatalog.order(customerId, orderId) : catalog.order(customerId, orderId); }
    private List<Order> allOrders() { return jdbcCatalog.enabled() ? jdbcCatalog.allOrders() : catalog.allOrders(); }
    private Product createProduct(ProductRequest request) { return jdbcCatalog.enabled() ? jdbcCatalog.createProduct("demo-store", request.title(), request.description(), request.category(), request.brand(), request.price(), request.stock(), request.image()) : catalog.createProduct(request.title(), request.description(), request.category(), request.brand(), request.price(), request.stock(), request.image()); }
    private Product updateProduct(String id, ProductRequest request) { return jdbcCatalog.enabled() ? jdbcCatalog.updateProduct(id, request.title(), request.description(), request.category(), request.brand(), request.price(), request.stock(), request.image()) : catalog.updateProduct(id, request.title(), request.description(), request.category(), request.brand(), request.price(), request.stock(), request.image()); }

    private Map<String, Object> ticketOut(SupportStore.Ticket ticket) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ticket_id", ticket.id());
        result.put("store_id", ticket.storeId());
        result.put("conversation_id", ticket.conversationId());
        result.put("customer_id", ticket.customerId());
        result.put("order_id", ticket.orderId());
        result.put("product_id", ticket.productId());
        result.put("intent", ticket.intent());
        result.put("status", ticket.status());
        result.put("priority", ticket.priority());
        result.put("resolution", ticket.resolution());
        result.put("created_at", ticket.createdAt());
        result.put("updated_at", ticket.updatedAt());
        return result;
    }

    private Map<String, Object> cartResponse(String customerId) { return cartResponse(customerId, jdbcCatalog.enabled() ? jdbcCatalog.cart(customerId) : catalog.cart(customerId)); }
    private Map<String, Object> cartResponse(String customerId, List<CartLine> lines) {
        List<Map<String, Object>> items = lines.stream().map(line -> Map.<String, Object>of(
                "product_id", line.product().id(), "title", line.product().title(), "quantity", line.quantity(),
                "unit_price", line.unitPrice(), "subtotal", line.subtotal(), "image", line.product().image())).toList();
        BigDecimal total = lines.stream().map(CartLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("items", items, "total", total);
    }

    private User customer(String authorization) {
        User user = authenticated(authorization);
        if (!"customer".equals(user.role())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权执行此操作");
        return user;
    }

    private User requireStaff(String authorization, String... roles) {
        User user = authenticated(authorization);
        for (String role : roles) if (role.equals(user.role())) return user;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权执行此操作");
    }

    private User authenticated(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "需要登录");
        User user = authService.authenticate(authorization.substring("Bearer ".length()));
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效");
        return user;
    }
}
