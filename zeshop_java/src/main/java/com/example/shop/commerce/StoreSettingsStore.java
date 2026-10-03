package com.example.shop.commerce;

import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Store settings use memory in local-demo and PostgreSQL in the production profile.
 * Secret provider credentials never belong in this store; they stay in environment variables.
 */
@Component
public class StoreSettingsStore {
    private static final String STORE_ID = "demo-store";
    private static final Map<String, String> DEFAULTS = Map.of(
            "store_name", "Zeshop",
            "customer_service_phone", "028-8888-6666",
            "checkout_free_shipping_threshold", "99",
            "ai_enabled", "true",
            "ai_mode", "auto",
            "ai_assistant_name", "Zeshop AI");

    private final ObjectProvider<JdbcTemplate> jdbcProvider;
    private final Map<String, String> localValues = new ConcurrentHashMap<>(DEFAULTS);
    private JdbcTemplate jdbc;

    public StoreSettingsStore(ObjectProvider<JdbcTemplate> jdbcProvider) {
        this.jdbcProvider = jdbcProvider;
    }

    @PostConstruct
    void initialize() {
        jdbc = jdbcProvider.getIfAvailable();
        if (jdbc == null) return;
        jdbc.execute("CREATE TABLE IF NOT EXISTS store_settings (store_id VARCHAR(80) NOT NULL, setting_key VARCHAR(100) NOT NULL, setting_value TEXT NOT NULL, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (store_id, setting_key))");
        DEFAULTS.forEach((key, value) -> jdbc.update("INSERT INTO store_settings(store_id, setting_key, setting_value) VALUES (?, ?, ?) ON CONFLICT (store_id, setting_key) DO NOTHING", STORE_ID, key, value));
    }

    public Map<String, String> all(String storeId) {
        Map<String, String> result = new LinkedHashMap<>(DEFAULTS);
        if (jdbc == null) {
            result.putAll(localValues);
            return result;
        }
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT setting_key, setting_value FROM store_settings WHERE store_id = ?", storeId);
        rows.forEach(row -> result.put(String.valueOf(row.get("setting_key")), String.valueOf(row.get("setting_value"))));
        return result;
    }

    public String get(String storeId, String key) {
        return all(storeId).getOrDefault(key, "");
    }

    public String setTerm(String storeId, String kind, String value) {
        String key = "catalog_" + kind;
        set(storeId, key + "." + value, value);
        return key + "." + value;
    }

    public void set(String storeId, String key, String value) {
        if (jdbc == null) {
            localValues.put(key, value);
            return;
        }
        jdbc.update("INSERT INTO store_settings(store_id, setting_key, setting_value) VALUES (?, ?, ?) ON CONFLICT (store_id, setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value, updated_at = CURRENT_TIMESTAMP", storeId, key, value);
    }
}
