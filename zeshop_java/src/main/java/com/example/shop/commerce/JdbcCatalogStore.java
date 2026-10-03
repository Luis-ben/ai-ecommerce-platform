package com.example.shop.commerce;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * PostgreSQL-backed commerce storage used by the production profile.
 * The local-demo profile deliberately keeps CatalogService's deterministic in-memory fixture.
 */
@Component
public class JdbcCatalogStore {
    private final JdbcTemplate jdbc;
    private final CatalogService seedCatalog;
    private final RowMapper<CatalogService.Product> productMapper = (rs, row) -> new CatalogService.Product(
            rs.getString("id"), rs.getString("title"), rs.getString("description"), rs.getString("category"),
            rs.getString("brand"), rs.getBigDecimal("price"), rs.getInt("stock"), rs.getString("image"),
            rs.getString("tag"), rs.getInt("sales"));
    private final RowMapper<CatalogService.User> userMapper = (rs, row) -> new CatalogService.User(
            rs.getString("id"), rs.getString("email"), rs.getString("role"), rs.getString("store_id"), rs.getString("display_name"));

    public JdbcCatalogStore(ObjectProvider<JdbcTemplate> provider, CatalogService seedCatalog) {
        this.jdbc = provider.getIfAvailable();
        this.seedCatalog = seedCatalog;
    }

    public boolean enabled() { return jdbc != null; }

    @PostConstruct
    void initializeProductionStore() {
        initialize(seedCatalog.allProducts().stream().collect(java.util.stream.Collectors.toMap(CatalogService.Product::id, product -> product)), seedCatalog.users().stream().collect(java.util.stream.Collectors.toMap(CatalogService.User::id, user -> user)));
    }

    public void initialize(Map<String, CatalogService.Product> seedProducts, Map<String, CatalogService.User> seedUsers) {
        if (!enabled()) return;
        jdbc.execute("CREATE TABLE IF NOT EXISTS shop_users (id VARCHAR(80) PRIMARY KEY, email VARCHAR(255) UNIQUE NOT NULL, role VARCHAR(30) NOT NULL, store_id VARCHAR(80) NOT NULL, display_name VARCHAR(120) NOT NULL, password_hash VARCHAR(256))");
        jdbc.execute("ALTER TABLE shop_users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(256)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS shop_products (id VARCHAR(80) PRIMARY KEY, store_id VARCHAR(80) NOT NULL, title VARCHAR(240) NOT NULL, description TEXT NOT NULL, category VARCHAR(120) NOT NULL, brand VARCHAR(120) NOT NULL, price NUMERIC(14,2) NOT NULL, stock INTEGER NOT NULL, image VARCHAR(500), tag VARCHAR(60), sales INTEGER NOT NULL DEFAULT 0, active BOOLEAN NOT NULL DEFAULT TRUE)");
        jdbc.execute("ALTER TABLE shop_products ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE");
        jdbc.execute("CREATE TABLE IF NOT EXISTS shop_categories (id VARCHAR(80) PRIMARY KEY, store_id VARCHAR(80) NOT NULL, name VARCHAR(120) NOT NULL, UNIQUE(store_id, name))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS shop_brands (id VARCHAR(80) PRIMARY KEY, store_id VARCHAR(80) NOT NULL, name VARCHAR(120) NOT NULL, UNIQUE(store_id, name))");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_shop_products_store ON shop_products(store_id, active, stock)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS shop_carts (customer_id VARCHAR(80) NOT NULL, product_id VARCHAR(80) NOT NULL, quantity INTEGER NOT NULL, PRIMARY KEY(customer_id, product_id), FOREIGN KEY(product_id) REFERENCES shop_products(id))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS shop_orders (id VARCHAR(100) PRIMARY KEY, customer_id VARCHAR(80) NOT NULL, status VARCHAR(30) NOT NULL, total NUMERIC(14,2) NOT NULL, shipping_status VARCHAR(30) NOT NULL, created_at TIMESTAMP NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS shop_order_items (id VARCHAR(100) PRIMARY KEY, order_id VARCHAR(100) NOT NULL, product_id VARCHAR(80) NOT NULL, title VARCHAR(240) NOT NULL, quantity INTEGER NOT NULL, unit_price NUMERIC(14,2) NOT NULL, FOREIGN KEY(order_id) REFERENCES shop_orders(id))");
        if (Boolean.parseBoolean(System.getenv().getOrDefault("SHOP_SEED_DEMO", "false"))) {
            seedUsers.values().forEach(user -> jdbc.update("INSERT INTO shop_users(id, email, role, store_id, display_name, password_hash) VALUES (?, ?, ?, ?, ?, NULL) ON CONFLICT(email) DO NOTHING", user.id(), user.email(), user.role(), user.storeId(), user.displayName()));
            seedProducts.values().forEach(product -> jdbc.update("INSERT INTO shop_products(id, store_id, title, description, category, brand, price, stock, image, tag, sales) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(id) DO NOTHING", product.id(), "demo-store", product.title(), product.description(), product.category(), product.brand(), product.price(), product.stock(), product.image(), product.tag(), product.sales()));
        }
    }

    public List<CatalogService.Product> search(String query, BigDecimal maxPrice, String brand, String category) {
        if (!enabled()) return List.of();
        StringBuilder sql = new StringBuilder("SELECT * FROM shop_products WHERE active = TRUE AND stock > 0");
        List<Object> args = new ArrayList<>();
        if (maxPrice != null) { sql.append(" AND price <= ?"); args.add(maxPrice); }
        if (brand != null && !brand.isBlank()) { sql.append(" AND LOWER(brand) = LOWER(?)"); args.add(brand); }
        if (category != null && !category.isBlank()) { sql.append(" AND LOWER(category) = LOWER(?)"); args.add(category); }
        if (query != null && !query.isBlank()) { sql.append(" AND LOWER(CONCAT(title, ' ', description, ' ', category, ' ', brand)) LIKE LOWER(?)"); args.add("%" + query.trim() + "%"); }
        sql.append(" ORDER BY sales DESC, id ASC");
        return jdbc.query(sql.toString(), productMapper, args.toArray());
    }

    public List<CatalogService.Product> allProducts() { return enabled() ? jdbc.query("SELECT * FROM shop_products WHERE active = TRUE ORDER BY sales DESC, id ASC", productMapper) : List.of(); }

    public List<CatalogService.Product> deletedProducts() { return enabled() ? jdbc.query("SELECT * FROM shop_products WHERE active = FALSE ORDER BY id ASC", productMapper) : List.of(); }

    public void deleteProduct(String id) { jdbc.update("UPDATE shop_products SET active = FALSE WHERE id = ?", id); }
    public void restoreProduct(String id) { jdbc.update("UPDATE shop_products SET active = TRUE WHERE id = ?", id); }

    public List<String> categories(String storeId) { return jdbc.query("SELECT name FROM shop_categories WHERE store_id = ? ORDER BY name", (rs, row) -> rs.getString(1), storeId); }
    public List<String> brands(String storeId) { return jdbc.query("SELECT name FROM shop_brands WHERE store_id = ? ORDER BY name", (rs, row) -> rs.getString(1), storeId); }
    public String addCategory(String storeId, String name) { String id = "cat-java-" + UUID.randomUUID().toString().substring(0, 12); jdbc.update("INSERT INTO shop_categories(id, store_id, name) VALUES (?, ?, ?) ON CONFLICT(store_id, name) DO NOTHING", id, storeId, name); return id; }
    public String addBrand(String storeId, String name) { String id = "brand-java-" + UUID.randomUUID().toString().substring(0, 12); jdbc.update("INSERT INTO shop_brands(id, store_id, name) VALUES (?, ?, ?) ON CONFLICT(store_id, name) DO NOTHING", id, storeId, name); return id; }

    public CatalogService.Product get(String id) {
        if (!enabled()) return null;
        List<CatalogService.Product> rows = jdbc.query("SELECT * FROM shop_products WHERE id = ?", productMapper, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public CatalogService.User login(String email) {
        if (!enabled()) return null;
        List<CatalogService.User> rows = jdbc.query("SELECT * FROM shop_users WHERE LOWER(email) = LOWER(?)", userMapper, email.trim());
        return rows.isEmpty() ? null : rows.get(0);
    }

    public CatalogService.User userById(String id) {
        if (!enabled()) return null;
        List<CatalogService.User> rows = jdbc.query("SELECT * FROM shop_users WHERE id = ?", userMapper, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public String passwordHash(String userId) {
        if (!enabled()) return null;
        List<String> rows = jdbc.query("SELECT password_hash FROM shop_users WHERE id = ?", (rs, row) -> rs.getString(1), userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public CatalogService.User createUser(String id, String email, String role, String storeId, String displayName, String passwordHash) {
        if (!enabled()) throw new IllegalStateException("生产用户存储未启用");
        jdbc.update("INSERT INTO shop_users(id, email, role, store_id, display_name, password_hash) VALUES (?, ?, ?, ?, ?, ?)", id, email, role, storeId, displayName, passwordHash);
        return userById(id);
    }

    public List<CatalogService.User> users() { return enabled() ? jdbc.query("SELECT * FROM shop_users ORDER BY id", userMapper) : List.of(); }

    public List<CatalogService.CartLine> cart(String customerId) {
        if (!enabled()) return List.of();
        return jdbc.query("SELECT p.*, c.quantity AS cart_quantity FROM shop_carts c JOIN shop_products p ON p.id = c.product_id WHERE c.customer_id = ? ORDER BY p.id", (rs, row) -> {
            CatalogService.Product product = productMapper.mapRow(rs, row);
            int quantity = rs.getInt("cart_quantity");
            return new CatalogService.CartLine(product, quantity, product.price(), product.price().multiply(BigDecimal.valueOf(quantity)));
        }, customerId);
    }

    @Transactional
    public List<CatalogService.CartLine> addCart(String customerId, String productId, int quantity) {
        CatalogService.Product product = get(productId);
        if (product == null) throw new IllegalArgumentException("商品不存在");
        List<Integer> currentRows = jdbc.query("SELECT quantity FROM shop_carts WHERE customer_id = ? AND product_id = ?", (rs, row) -> rs.getInt(1), customerId, productId);
        int next = (currentRows.isEmpty() ? 0 : currentRows.get(0)) + quantity;
        if (quantity < 1 || next > product.stock()) throw new IllegalStateException("库存不足");
        jdbc.update("INSERT INTO shop_carts(customer_id, product_id, quantity) VALUES (?, ?, ?) ON CONFLICT(customer_id, product_id) DO UPDATE SET quantity = EXCLUDED.quantity", customerId, productId, next);
        return cart(customerId);
    }

    @Transactional
    public List<CatalogService.CartLine> setCartQuantity(String customerId, String productId, int quantity) {
        CatalogService.Product product = get(productId);
        if (product == null || quantity < 1 || quantity > product.stock()) throw new IllegalStateException("库存不足");
        int rows = jdbc.update("UPDATE shop_carts SET quantity = ? WHERE customer_id = ? AND product_id = ?", quantity, customerId, productId);
        if (rows == 0) throw new java.util.NoSuchElementException("购物车中没有该商品");
        return cart(customerId);
    }

    @Transactional
    public List<CatalogService.CartLine> removeCartItem(String customerId, String productId) {
        jdbc.update("DELETE FROM shop_carts WHERE customer_id = ? AND product_id = ?", customerId, productId);
        return cart(customerId);
    }

    @Transactional
    public CatalogService.Order checkout(String customerId) {
        List<CatalogService.CartLine> lines = cart(customerId);
        if (lines.isEmpty()) throw new IllegalStateException("购物车为空");
        BigDecimal total = BigDecimal.ZERO;
        for (CatalogService.CartLine line : lines) {
            CatalogService.Product current = jdbc.queryForObject("SELECT * FROM shop_products WHERE id = ? FOR UPDATE", productMapper, line.product().id());
            if (current == null || current.stock() < line.quantity()) throw new IllegalStateException("商品库存已变化，请刷新购物车");
            total = total.add(current.price().multiply(BigDecimal.valueOf(line.quantity())));
        }
        String orderId = "ord-java-" + UUID.randomUUID().toString().substring(0, 12);
        Instant createdAt = Instant.now();
        jdbc.update("INSERT INTO shop_orders(id, customer_id, status, total, shipping_status, created_at) VALUES (?, ?, ?, ?, ?, ?)", orderId, customerId, "PAID", total, "PROCESSING", Timestamp.from(createdAt));
        List<CatalogService.OrderItem> items = new ArrayList<>();
        for (CatalogService.CartLine line : lines) {
            CatalogService.Product current = jdbc.queryForObject("SELECT * FROM shop_products WHERE id = ? FOR UPDATE", productMapper, line.product().id());
            jdbc.update("UPDATE shop_products SET stock = stock - ?, sales = sales + ? WHERE id = ?", line.quantity(), line.quantity(), line.product().id());
            CatalogService.OrderItem item = new CatalogService.OrderItem(current.id(), current.title(), line.quantity(), current.price());
            items.add(item);
            jdbc.update("INSERT INTO shop_order_items(id, order_id, product_id, title, quantity, unit_price) VALUES (?, ?, ?, ?, ?, ?)", "oi-java-" + UUID.randomUUID().toString().substring(0, 12), orderId, item.productId(), item.title(), item.quantity(), item.unitPrice());
        }
        jdbc.update("DELETE FROM shop_carts WHERE customer_id = ?", customerId);
        return new CatalogService.Order(orderId, customerId, "PAID", total, "PROCESSING", createdAt, List.copyOf(items));
    }

    public List<CatalogService.Order> orders(String customerId) {
        if (!enabled()) return List.of();
        return jdbc.query("SELECT * FROM shop_orders WHERE customer_id = ? ORDER BY created_at DESC", (rs, row) -> mapOrder(rs.getString("id"), rs.getString("customer_id"), rs.getString("status"), rs.getBigDecimal("total"), rs.getString("shipping_status"), rs.getTimestamp("created_at").toInstant()), customerId);
    }

    public CatalogService.Order order(String customerId, String orderId) {
        if (!enabled()) return null;
        List<CatalogService.Order> rows = jdbc.query("SELECT * FROM shop_orders WHERE customer_id = ? AND id = ?", (rs, row) -> mapOrder(rs.getString("id"), rs.getString("customer_id"), rs.getString("status"), rs.getBigDecimal("total"), rs.getString("shipping_status"), rs.getTimestamp("created_at").toInstant()), customerId, orderId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private CatalogService.Order mapOrder(String id, String customerId, String status, BigDecimal total, String shipping, Instant createdAt) {
        List<CatalogService.OrderItem> items = jdbc.query("SELECT product_id, title, quantity, unit_price FROM shop_order_items WHERE order_id = ?", (rs, row) -> new CatalogService.OrderItem(rs.getString("product_id"), rs.getString("title"), rs.getInt("quantity"), rs.getBigDecimal("unit_price")), id);
        return new CatalogService.Order(id, customerId, status, total, shipping, createdAt, List.copyOf(items));
    }

    public List<CatalogService.Order> allOrders() { return enabled() ? users().stream().flatMap(user -> orders(user.id()).stream()).toList() : List.of(); }

    public CatalogService.Order updateOrder(String orderId, String status, String shippingStatus) {
        if (status != null) jdbc.update("UPDATE shop_orders SET status = ? WHERE id = ?", status, orderId);
        if (shippingStatus != null) jdbc.update("UPDATE shop_orders SET shipping_status = ? WHERE id = ?", shippingStatus, orderId);
        List<CatalogService.Order> rows = jdbc.query("SELECT * FROM shop_orders WHERE id = ?", (rs, row) -> mapOrder(rs.getString("id"), rs.getString("customer_id"), rs.getString("status"), rs.getBigDecimal("total"), rs.getString("shipping_status"), rs.getTimestamp("created_at").toInstant()), orderId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public CatalogService.Product createProduct(String storeId, String title, String description, String category, String brand, BigDecimal price, int stock, String image) {
        String id = "p-java-" + UUID.randomUUID().toString().substring(0, 12);
        jdbc.update("INSERT INTO shop_products(id, store_id, title, description, category, brand, price, stock, image, tag, sales) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)", id, storeId, title, description == null ? "" : description, category == null ? "" : category, brand == null ? "" : brand, price, stock, image, "");
        return get(id);
    }

    public CatalogService.Product updateProduct(String id, String title, String description, String category, String brand, BigDecimal price, int stock, String image) {
        int rows = jdbc.update("UPDATE shop_products SET title = ?, description = ?, category = ?, brand = ?, price = ?, stock = ?, image = ? WHERE id = ?", title, description == null ? "" : description, category == null ? "" : category, brand == null ? "" : brand, price, stock, image, id);
        if (rows == 0) throw new java.util.NoSuchElementException("商品不存在");
        return get(id);
    }
}
