package com.example.shop.security;

import com.example.shop.commerce.CatalogService;
import com.example.shop.commerce.JdbcCatalogStore;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Local-demo keeps the passwordless fixture; production uses BCrypt passwords and signed expiring tokens.
 */
@Service
public class AuthService {
    private static final long TOKEN_TTL_SECONDS = 24 * 60 * 60;
    private final CatalogService demoCatalog;
    private final JdbcCatalogStore store;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final String secret;

    public AuthService(CatalogService demoCatalog, JdbcCatalogStore store) {
        this.demoCatalog = demoCatalog;
        this.store = store;
        this.secret = System.getenv().getOrDefault("SHOP_AUTH_SECRET", "change-me-in-production");
        if (store.enabled() && "change-me-in-production".equals(this.secret)) {
            throw new IllegalStateException("SHOP_AUTH_SECRET must be changed in production");
        }
    }

    @PostConstruct
    void bootstrapProductionAdmin() {
        if (!store.enabled()) return;
        String email = System.getenv("SHOP_BOOTSTRAP_ADMIN_EMAIL");
        String password = System.getenv("SHOP_BOOTSTRAP_ADMIN_PASSWORD");
        if (email != null && !email.isBlank() && password != null && !password.isBlank() && store.login(email) == null) {
            store.createUser("merchant-java-admin", email.trim().toLowerCase(), "merchant", "demo-store", "Store administrator", passwords.encode(password));
        }
    }

    public CatalogService.User login(String email, String password) {
        CatalogService.User user = store.enabled() ? store.login(email) : demoCatalog.login(email);
        if (user == null) throw unauthorized();
        if (store.enabled()) {
            String encoded = store.passwordHash(user.id());
            if (password == null || encoded == null || !passwords.matches(password, encoded)) throw unauthorized();
        }
        return user;
    }

    public CatalogService.User register(String email, String password, String displayName) {
        if (!store.enabled()) throw new ResponseStatusException(HttpStatus.CONFLICT, "演示模式不支持注册");
        if (password == null || password.length() < 8) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码至少需要 8 位");
        if (store.login(email) != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已注册");
        return store.createUser("usr-java-" + java.util.UUID.randomUUID().toString().substring(0, 12), email.trim().toLowerCase(), "customer", "demo-store", displayName.trim(), passwords.encode(password));
    }

    public String issueToken(CatalogService.User user) {
        if (!store.enabled()) return "local-" + user.id();
        long expiresAt = System.currentTimeMillis() / 1000 + TOKEN_TTL_SECONDS;
        String payload = encode(user.id() + ":" + expiresAt);
        return "v1." + payload + "." + sign(payload);
    }

    public CatalogService.User authenticate(String token) {
        if (!store.enabled() && token != null && token.startsWith("local-")) return demoCatalog.userById(token.substring("local-".length()));
        if (token == null) return null;
        try {
            String[] parts = token.split("\\.", 3);
            if (parts.length != 3 || !"v1".equals(parts[0]) || !MessageDigest.isEqual(parts[2].getBytes(StandardCharsets.UTF_8), sign(parts[1]).getBytes(StandardCharsets.UTF_8))) return null;
            String[] claims = new String(Base64.getUrlDecoder().decode(parts[1])).split(":", 2);
            if (claims.length != 2 || Long.parseLong(claims[1]) < System.currentTimeMillis() / 1000) return null;
            return store.userById(claims[0]);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public boolean production() { return store.enabled(); }

    public CatalogService.User userById(String id) {
        return store.enabled() ? store.userById(id) : demoCatalog.userById(id);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("无法初始化认证签名", exception);
        }
    }

    private String encode(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private ResponseStatusException unauthorized() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "邮箱或密码错误"); }
}
