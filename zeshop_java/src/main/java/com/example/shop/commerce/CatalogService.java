package com.example.shop.commerce;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {
    public record Product(String id, String title, String description, String category, String brand, BigDecimal price, int stock, String image, String tag, int sales) {}
    public record User(String id, String email, String role, @JsonProperty("store_id") String storeId, @JsonProperty("display_name") String displayName) {}
    public record CartLine(Product product, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
    public record OrderItem(@JsonProperty("product_id") String productId, String title, int quantity, @JsonProperty("unit_price") BigDecimal unitPrice) {}
    public record Order(String id, @JsonProperty("customer_id") String customerId, String status, BigDecimal total, @JsonProperty("shipping_status") String shippingStatus, @JsonProperty("created_at") Instant createdAt, List<OrderItem> items) {}

    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final Map<String, Integer> stocks = new ConcurrentHashMap<>();
    private final Map<String, User> users = Map.of(
            "customer@example.com", new User("customer-demo", "customer@example.com", "customer", "demo-store", "演示顾客"),
            "merchant@example.com", new User("merchant-demo", "merchant@example.com", "merchant", "demo-store", "演示商户"),
            "agent@example.com", new User("agent-demo", "agent@example.com", "agent", "demo-store", "演示客服"));
    private final Map<String, Map<String, Integer>> carts = new ConcurrentHashMap<>();
    private final Map<String, List<Order>> orders = new ConcurrentHashMap<>();
    private final Set<String> deletedProducts = ConcurrentHashMap.newKeySet();
    private final Set<String> customCategories = ConcurrentHashMap.newKeySet();
    private final Set<String> customBrands = ConcurrentHashMap.newKeySet();
    private final Map<String, String> settings = new ConcurrentHashMap<>(Map.of(
            "store_name", "Zeshop",
            "customer_service_phone", "028-8888-6666",
            "checkout_free_shipping_threshold", "99",
            "ai_enabled", "true",
            "ai_mode", "auto",
            "ai_assistant_name", "Zeshop AI"));
    private long nextProductId = 1000;
    private long nextOrderId = 1;

    public CatalogService() {
        add("1", "欧洲站夏季新款时尚休闲短裤热裤女裤运动家具纯棉韩版宽松百搭裤", "轻盈透气的夏季休闲短裤，宽松剪裁适合日常出行。", "下装", "Burberry", "88.30", 2222, "1.webp", "Clearance", 982);
        add("2", "中长款牛仔半身裙女春夏季2021新款薄款高腰开叉包臀长裙A字裙子", "高腰开叉牛仔半身裙，通勤与休闲场景都适合。", "下装", "Versace", "324.00", 546, "2.webp", "Hot", 816);
        add("3", "双肩包书包男女笔记本电脑包时尚潮流旅行背包", "轻便大容量旅行背包，适配笔记本电脑与日常通勤。", "配饰", "Saint Laurent", "299.00", 444, "3.webp", "", 721);
        add("4", "男子 休闲鞋 TANJUN 天君 休闲鞋 运动鞋 812654", "简洁百搭的轻量休闲运动鞋，柔软鞋底提升日常舒适度。", "鞋履", "Chanel", "99.00", 333, "4.webp", "Hot", 654);
        add("5", "高级感男装夏季潮牌美式复古短袖t恤男士重磅纯棉宽松半袖男体恤", "重磅纯棉面料，美式复古廓形，适合日常搭配。", "上装", "Balenciaga", "999.00", 421, "5.webp", "", 498);
        add("6", "冰丝褶皱垂感圆领长袖T恤男宽松薄款高级感v领男装上衣", "冰丝垂感面料，轻薄透气，打造简约高级的日常造型。", "上装", "Valentino", "423.00", 123, "6.webp", "", 462);
        add("7", "轻便跑鞋女夏新款跑步运动鞋减震软底网面透气休闲运动鞋女鞋", "网面透气跑鞋，减震软底适合跑步和轻运动。", "运动户外", "Dior", "99.00", 3333, "7.webp", "New", 932);
        add("8", "夏季新款模糊数码直喷短袖上衣男装情侣纯棉t恤ins潮", "纯棉短袖上衣，数码直喷图案，适合情侣搭配。", "上装", "Louis Vuitton", "99.00", 2222, "8.webp", "", 380);
        add("9", "春夏新品暗黑系甜辣风吊带裙欧根纱蓬蓬裙冷淡法式连衣裙女", "法式欧根纱吊带连衣裙，轻盈裙摆展现优雅气质。", "女装", "Valentino", "299.00", 99, "9.webp", "New", 388);
        add("10", "法式小众设计高级感连衣裙2022年新款收腰显瘦中长款裙子女夏", "收腰显瘦中长款连衣裙，小众设计适合夏日出行。", "女装", "Gucci", "169.00", 33, "10.webp", "", 319);
        add("11", "夏季新款polo领连衣裙女学院风减龄不规则下摆长裙子", "学院风 Polo 领连衣裙，不规则裙摆俏皮又利落。", "女装", "Valentino", "98.00", 2223, "11.webp", "Clearance", 287);
        add("12", "凉鞋女款夏季2022年新款坡跟女鞋夏款松糕鞋高跟鞋子爆款女士", "夏季坡跟凉鞋，舒适增高设计，适合度假和日常搭配。", "鞋履", "Valentino", "79.00", 2222, "12.webp", "", 251);
        add("13", "休闲polo衬衫连衣裙大码女装高级感夏2022新款小个子御姐", "休闲 Polo 衬衫连衣裙，兼顾舒适度和高级感。", "女装", "Dior", "199.00", 3445, "13.webp", "", 243);
        add("14", "夏季套装短袖T恤男装一套搭配帅气潮情侣男生半袖上衣服", "短袖 T 恤套装，轻松完成夏季情侣搭配。", "男装", "Armani", "299.00", 423, "14.webp", "", 211);
        add("15", "男鞋2022夏季透气冲孔时尚休闲板鞋压花耐磨小白鞋男", "冲孔透气小白鞋，耐磨鞋底适合城市日常穿着。", "鞋履", "Prada", "324.00", 546, "15.webp", "Hot", 205);
        add("35", "气质通勤高街蛋青色挺括烟管裤9分裤套装下装22秋女", "挺括烟管裤，利落剪裁适合通勤和正式场合。", "下装", "Gucci", "100.99", 999, "16.webp", "", 184);
        add("39", "夏季新款女装法式气质洋气高级感温柔风吊带仙女连衣裙", "温柔风吊带仙女裙，轻盈面料呈现法式夏日气质。", "女装", "Saint Laurent", "100.00", 999, "18.webp", "New", 172);
        add("101", "GG 马衔扣皮革乐福鞋", "奢华品牌演示目录商品，经典马衔扣元素与柔软皮革鞋面。", "奢华鞋履", "Gucci", "6890.00", 6, "4.webp", "Luxury", 68);
        add("102", "Re-Nylon 轻旅双肩包", "奢华品牌演示目录商品，轻量尼龙材质与城市通勤结构。", "奢华配饰", "Prada", "12600.00", 4, "3.webp", "Luxury", 54);
        add("103", "Oblique 印花旅行手袋", "奢华品牌演示目录商品，旅行容量与经典印花风格。", "奢华配饰", "Dior", "15800.00", 3, "5.webp", "Luxury", 47);
        add("104", "Monogram 经典旅行袋", "奢华品牌演示目录商品，适合短途出行的轻便旅行包。", "奢华配饰", "Louis Vuitton", "18500.00", 2, "6.webp", "Luxury", 42);
        add("105", "羊皮链条肩背包", "奢华品牌演示目录商品，柔软菱格纹皮革与链条肩带。", "奢华配饰", "Chanel", "29800.00", 2, "9.webp", "Luxury", 38);
        add("106", "真丝方巾限定配色", "奢华品牌演示目录商品，真丝面料与艺术图案。", "奢华配饰", "Hermès", "4200.00", 8, "10.webp", "Luxury", 76);
        add("107", "Kate 细链条肩背包", "奢华品牌演示目录商品，简洁晚宴造型与金属链条。", "奢华配饰", "Saint Laurent", "13200.00", 5, "11.webp", "Luxury", 61);
        add("108", "编织皮革斜挎包", "奢华品牌演示目录商品，手工感编织皮革与日常容量。", "奢华配饰", "Bottega Veneta", "23600.00", 2, "12.webp", "Luxury", 33);
        add("109", "Triomphe 经典太阳镜", "奢华品牌演示目录商品，复古镜框与轻量镜片。", "奢华配饰", "Celine", "3600.00", 12, "13.webp", "Luxury", 88);
        add("110", "FF 标识羊毛围巾", "奢华品牌演示目录商品，羊毛混纺面料与经典标识。", "奢华配饰", "Fendi", "5800.00", 7, "14.webp", "Luxury", 41);
        add("111", "复古方形腕表演示款", "奢华品牌演示目录商品，复古方形表盘与简洁皮表带。", "奢华腕表", "Cartier", "42600.00", 1, "15.webp", "Luxury", 19);
        add("112", "经典潜水腕表演示款", "奢华品牌演示目录商品，运动风格与耐用表壳设计。", "奢华腕表", "Rolex", "76800.00", 1, "16.webp", "Luxury", 12);
        add("113", "Rockstud 铆钉高跟鞋", "奢华品牌演示目录商品，尖头鞋型与标志性铆钉装饰。", "奢华鞋履", "Valentino", "8900.00", 5, "7.webp", "Luxury", 51);
        add("114", "Hourglass 轮廓手袋", "奢华品牌演示目录商品，结构化轮廓与通勤容量。", "奢华配饰", "Balenciaga", "17400.00", 3, "17.webp", "Luxury", 29);
        add("115", "Peekaboo 手提包演示款", "奢华品牌演示目录商品，经典手提结构与可拆卸肩带。", "奢华配饰", "Fendi", "22900.00", 2, "18.webp", "Luxury", 24);
        add("116", "Puzzle 几何手袋演示款", "奢华品牌演示目录商品，几何拼接皮革与多种背法。", "奢华配饰", "Loewe", "19800.00", 3, "1.webp", "Luxury", 31);
        add("117", "羽绒夹克限定演示款", "奢华品牌演示目录商品，轻量保暖填充与城市户外风格。", "奢华服饰", "Moncler", "15900.00", 4, "2.webp", "Luxury", 36);
        add("118", "Alexander McQueen 运动鞋", "奢华品牌演示目录商品，厚底鞋型与简洁城市风格。", "奢华鞋履", "Alexander McQueen", "7600.00", 6, "7.webp", "Luxury", 44);
    }

    private void add(String id, String title, String description, String category, String brand, String price, int stock, String image, String tag, int sales) {
        Product product = new Product(id, title, description, category, brand, new BigDecimal(price), stock, "/image/catalog/demo/product/" + image, tag, sales);
        products.put(id, product);
        stocks.put(id, stock);
    }

    public List<Product> search(String query, BigDecimal maxPrice, String brand, String category) {
        String q = query == null ? "" : query.toLowerCase();
        return products.values().stream().filter(p -> !deletedProducts.contains(p.id())).filter(p -> stocks.getOrDefault(p.id(), 0) > 0)
                .filter(p -> maxPrice == null || p.price().compareTo(maxPrice) <= 0)
                .filter(p -> brand == null || brand.isBlank() || p.brand().equalsIgnoreCase(brand))
                .filter(p -> category == null || category.isBlank() || p.category().equalsIgnoreCase(category))
                .filter(p -> q.isBlank() || (p.title() + p.description() + p.brand() + p.category()).toLowerCase().contains(q))
                .map(this::withCurrentStock)
                .toList();
    }

    private Product withCurrentStock(Product product) {
        return new Product(product.id(), product.title(), product.description(), product.category(), product.brand(), product.price(), stocks.getOrDefault(product.id(), product.stock()), product.image(), product.tag(), product.sales());
    }

    public Product get(String id) {
        Product product = products.get(id);
        return product == null || deletedProducts.contains(id) ? null : withCurrentStock(product);
    }
    public User login(String email) { return users.get(email); }
    public User userById(String id) { return users.values().stream().filter(user -> user.id().equals(id)).findFirst().orElse(null); }
    public List<User> users() { return List.copyOf(users.values()); }
    public Map<String, String> settings() { return Map.copyOf(settings); }
    public String setting(String key) { return settings.get(key); }
    public void setSetting(String key, String value) { settings.put(key, value); }
    public List<Product> allProducts() { return products.values().stream().filter(product -> !deletedProducts.contains(product.id())).map(this::withCurrentStock).toList(); }
    public List<Product> deletedProducts() { return products.values().stream().filter(product -> deletedProducts.contains(product.id())).map(this::withCurrentStock).toList(); }
    public void deleteProduct(String id) { if (products.containsKey(id)) deletedProducts.add(id); }
    public void restoreProduct(String id) { deletedProducts.remove(id); }
    public List<String> categories() { TreeSet<String> values = new TreeSet<>(customCategories); products.values().stream().map(Product::category).filter(value -> value != null && !value.isBlank()).forEach(values::add); return List.copyOf(values); }
    public List<String> brands() { TreeSet<String> values = new TreeSet<>(customBrands); products.values().stream().map(Product::brand).filter(value -> value != null && !value.isBlank()).forEach(values::add); return List.copyOf(values); }
    public String addCategory(String name) { customCategories.add(name); return name; }
    public String addBrand(String name) { customBrands.add(name); return name; }
    public List<Order> allOrders() { return users.values().stream().flatMap(user -> orders(user.id()).stream()).toList(); }

    public synchronized Order updateOrder(String orderId, String status, String shippingStatus) {
        for (List<Order> customerOrders : orders.values()) {
            for (int index = 0; index < customerOrders.size(); index++) {
                Order current = customerOrders.get(index);
                if (current.id().equals(orderId)) {
                    Order updated = new Order(current.id(), current.customerId(), status == null ? current.status() : status, current.total(), shippingStatus == null ? current.shippingStatus() : shippingStatus, current.createdAt(), current.items());
                    customerOrders.set(index, updated);
                    return updated;
                }
            }
        }
        return null;
    }
    public List<CartLine> cart(String customerId) {
        Map<String, Integer> items = carts.getOrDefault(customerId, Map.of());
        return items.entrySet().stream().map(entry -> {
            Product product = get(entry.getKey());
            if (product == null) return null;
            int quantity = entry.getValue();
            return new CartLine(product, quantity, product.price(), product.price().multiply(BigDecimal.valueOf(quantity)));
        }).filter(java.util.Objects::nonNull).toList();
    }
    public synchronized List<CartLine> addCart(String customerId, String productId, int quantity) {
        Product product = get(productId);
        if (product == null) throw new IllegalArgumentException("商品不存在");
        Map<String, Integer> cart = carts.computeIfAbsent(customerId, key -> new ConcurrentHashMap<>());
        int nextQuantity = cart.getOrDefault(productId, 0) + quantity;
        if (quantity < 1 || nextQuantity > product.stock()) throw new IllegalStateException("库存不足");
        cart.put(productId, nextQuantity);
        return cart(customerId);
    }
    public synchronized List<CartLine> setCartQuantity(String customerId, String productId, int quantity) {
        Map<String, Integer> cart = carts.get(customerId);
        if (cart == null || !cart.containsKey(productId)) throw new java.util.NoSuchElementException("购物车中没有该商品");
        Product product = get(productId);
        if (product == null || quantity < 1 || quantity > product.stock()) throw new IllegalStateException("库存不足");
        cart.put(productId, quantity);
        return cart(customerId);
    }
    public synchronized List<CartLine> removeCartItem(String customerId, String productId) {
        Map<String, Integer> cart = carts.get(customerId);
        if (cart != null) {
            cart.remove(productId);
            if (cart.isEmpty()) carts.remove(customerId);
        }
        return cart(customerId);
    }
    public synchronized Order checkout(String customerId) {
        Map<String, Integer> cart = carts.get(customerId);
        if (cart == null || cart.isEmpty()) throw new IllegalStateException("购物车为空");
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            Product product = get(entry.getKey());
            int quantity = entry.getValue();
            if (product == null || product.stock() < quantity) throw new IllegalStateException("商品库存已变化，请刷新购物车");
            total = total.add(product.price().multiply(BigDecimal.valueOf(quantity)));
            items.add(new OrderItem(product.id(), product.title(), quantity, product.price()));
        }
        for (OrderItem item : items) stocks.computeIfPresent(item.productId(), (id, stock) -> stock - item.quantity());
        Order order = new Order("ord-java-" + nextOrderId++, customerId, "PAID", total, "PROCESSING", Instant.now(), List.copyOf(items));
        orders.computeIfAbsent(customerId, key -> new ArrayList<>()).add(order);
        carts.remove(customerId);
        return order;
    }
    public List<Order> orders(String customerId) { return List.copyOf(orders.getOrDefault(customerId, List.of())); }
    public Order order(String customerId, String orderId) { return orders(customerId).stream().filter(order -> order.id().equals(orderId)).findFirst().orElse(null); }
    public synchronized Product createProduct(String title, String description, String category, String brand, BigDecimal price, int stock, String image) {
        String id = Long.toString(nextProductId++);
        Product product = new Product(id, title, description, category, brand, price, stock, image, "", 0);
        products.put(id, product);
        stocks.put(id, stock);
        return product;
    }
    public Product updateProduct(String id, String title, String description, String category, String brand, BigDecimal price, int stock, String image) {
        Product existing = products.get(id);
        if (existing == null) throw new java.util.NoSuchElementException("商品不存在");
        Product updated = new Product(id, title, description, category, brand, price, stock, image, existing.tag(), existing.sales());
        products.put(id, updated);
        stocks.put(id, stock);
        return updated;
    }
}
