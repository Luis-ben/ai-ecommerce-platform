package com.example.shop.commerce;

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

@Component
public class SupportStore {
    public record Ticket(String id, String storeId, String conversationId, String customerId, String orderId, String productId, String intent, String status, String priority, String resolution, String createdAt, String updatedAt) {}
    public record Message(String id, String ticketId, String role, String content, String createdAt) {}
    public record Conversation(String id, String storeId, String customerId, String createdAt) {}
    public record ConversationMessage(String id, String conversationId, String role, String content, String createdAt) {}

    private final ObjectProvider<JdbcTemplate> jdbcProvider;
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final Map<String, List<Message>> messages = new ConcurrentHashMap<>();
    private final Map<String, Conversation> conversations = new ConcurrentHashMap<>();
    private final Map<String, List<ConversationMessage>> conversationMessages = new ConcurrentHashMap<>();
    private JdbcTemplate jdbc;

    public SupportStore(ObjectProvider<JdbcTemplate> jdbcProvider) { this.jdbcProvider = jdbcProvider; }

    @PostConstruct
    void initialize() {
        jdbc = jdbcProvider.getIfAvailable();
        if (jdbc == null) return;
        jdbc.execute("CREATE TABLE IF NOT EXISTS support_tickets (id VARCHAR(80) PRIMARY KEY, store_id VARCHAR(80) NOT NULL, conversation_id VARCHAR(80), customer_id VARCHAR(80) NOT NULL, order_id VARCHAR(80), product_id VARCHAR(80), intent VARCHAR(40) NOT NULL, status VARCHAR(20) NOT NULL, priority VARCHAR(20) NOT NULL, resolution TEXT, created_at VARCHAR(40) NOT NULL, updated_at VARCHAR(40) NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS support_ticket_messages (id VARCHAR(80) PRIMARY KEY, ticket_id VARCHAR(80) NOT NULL, role VARCHAR(20) NOT NULL, content TEXT NOT NULL, created_at VARCHAR(40) NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS support_conversations (id VARCHAR(80) PRIMARY KEY, store_id VARCHAR(80) NOT NULL, customer_id VARCHAR(80), created_at VARCHAR(40) NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS support_conversation_messages (id VARCHAR(80) PRIMARY KEY, conversation_id VARCHAR(80) NOT NULL, role VARCHAR(20) NOT NULL, content TEXT NOT NULL, created_at VARCHAR(40) NOT NULL)");
    }

    public Ticket create(String store, String customer, String conversation, String order, String product, String intent) {
        String now = Instant.now().toString();
        Ticket ticket = new Ticket("ticket-java-" + UUID.randomUUID().toString().substring(0, 12), store, conversation, customer, order, product, intent, "OPEN", "NORMAL", null, now, now);
        if (jdbc == null) tickets.put(ticket.id(), ticket); else jdbc.update("INSERT INTO support_tickets VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", ticket.id(), ticket.storeId(), ticket.conversationId(), ticket.customerId(), ticket.orderId(), ticket.productId(), ticket.intent(), ticket.status(), ticket.priority(), ticket.resolution(), ticket.createdAt(), ticket.updatedAt());
        return ticket;
    }

    public List<Ticket> list(String store, String customer, String status) {
        if (jdbc == null) return tickets.values().stream().filter(t -> t.storeId().equals(store)).filter(t -> customer == null || t.customerId().equals(customer)).filter(t -> status == null || status.isBlank() || t.status().equals(status)).sorted((a, b) -> b.createdAt().compareTo(a.createdAt())).toList();
        String sql = "SELECT id, store_id, conversation_id, customer_id, order_id, product_id, intent, status, priority, resolution, created_at, updated_at FROM support_tickets WHERE store_id = ?";
        List<Object> args = new ArrayList<>(List.of(store));
        if (customer != null) { sql += " AND customer_id = ?"; args.add(customer); }
        if (status != null && !status.isBlank()) { sql += " AND status = ?"; args.add(status); }
        sql += " ORDER BY created_at DESC";
        return jdbc.query(sql, args.toArray(), (rs, row) -> new Ticket(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12)));
    }

    public Ticket get(String store, String id) {
        if (jdbc == null) return tickets.values().stream().filter(t -> t.storeId().equals(store) && t.id().equals(id)).findFirst().orElse(null);
        try { return jdbc.queryForObject("SELECT id, store_id, conversation_id, customer_id, order_id, product_id, intent, status, priority, resolution, created_at, updated_at FROM support_tickets WHERE store_id = ? AND id = ?", (rs, row) -> new Ticket(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12)), store, id); } catch (EmptyResultDataAccessException e) { return null; }
    }

    public Ticket update(String store, String id, String status, String priority, String resolution) {
        Ticket old = get(store, id);
        if (old == null) return null;
        Ticket next = new Ticket(old.id(), old.storeId(), old.conversationId(), old.customerId(), old.orderId(), old.productId(), old.intent(), status == null ? old.status() : status, priority == null ? old.priority() : priority, resolution == null ? old.resolution() : resolution, old.createdAt(), Instant.now().toString());
        if (jdbc == null) tickets.put(id, next); else jdbc.update("UPDATE support_tickets SET status = ?, priority = ?, resolution = ?, updated_at = ? WHERE store_id = ? AND id = ?", next.status(), next.priority(), next.resolution(), next.updatedAt(), store, id);
        return next;
    }

    public Message reply(String store, String id, String content) {
        if (get(store, id) == null) return null;
        Message message = new Message("message-java-" + UUID.randomUUID().toString().substring(0, 12), id, "agent", content, Instant.now().toString());
        if (jdbc == null) messages.computeIfAbsent(id, ignored -> new ArrayList<>()).add(message); else jdbc.update("INSERT INTO support_ticket_messages VALUES (?, ?, ?, ?, ?)", message.id(), message.ticketId(), message.role(), message.content(), message.createdAt());
        return message;
    }

    public List<Message> messages(String id) { if (jdbc == null) return List.copyOf(messages.getOrDefault(id, List.of())); return jdbc.query("SELECT id, ticket_id, role, content, created_at FROM support_ticket_messages WHERE ticket_id = ? ORDER BY created_at", (rs, row) -> new Message(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)), id); }
    public long count(String store) { return list(store, null, null).size(); }
    public long openCount(String store) { return list(store, null, null).stream().filter(t -> !"CLOSED".equals(t.status())).count(); }

    public Conversation getOrCreateConversation(String store, String customer, String conversationId) {
        String cid = (conversationId != null && !conversationId.isBlank()) ? conversationId : "conv-" + UUID.randomUUID().toString().substring(0, 12);
        Conversation existing = getConversation(store, cid);
        if (existing != null) return existing;
        String now = Instant.now().toString();
        Conversation conv = new Conversation(cid, store, customer, now);
        if (jdbc == null) {
            conversations.put(cid, conv);
        } else {
            jdbc.update("INSERT INTO support_conversations VALUES (?, ?, ?, ?)", conv.id(), conv.storeId(), conv.customerId(), conv.createdAt());
        }
        return conv;
    }

    public Conversation getConversation(String store, String id) {
        if (jdbc == null) {
            Conversation c = conversations.get(id);
            return (c != null && c.storeId().equals(store)) ? c : null;
        }
        try {
            return jdbc.queryForObject("SELECT id, store_id, customer_id, created_at FROM support_conversations WHERE store_id = ? AND id = ?",
                (rs, row) -> new Conversation(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)), store, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Conversation> listConversations(String store) {
        if (jdbc == null) {
            return conversations.values().stream()
                .filter(c -> c.storeId().equals(store))
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .toList();
        }
        return jdbc.query("SELECT id, store_id, customer_id, created_at FROM support_conversations WHERE store_id = ? ORDER BY created_at DESC",
            (rs, row) -> new Conversation(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)), store);
    }

    public ConversationMessage addConversationMessage(String conversationId, String role, String content) {
        String mid = "cmsg-" + UUID.randomUUID().toString().substring(0, 12);
        String now = Instant.now().toString();
        ConversationMessage msg = new ConversationMessage(mid, conversationId, role, content, now);
        if (jdbc == null) {
            conversationMessages.computeIfAbsent(conversationId, ignored -> new ArrayList<>()).add(msg);
        } else {
            jdbc.update("INSERT INTO support_conversation_messages VALUES (?, ?, ?, ?, ?)", msg.id(), msg.conversationId(), msg.role(), msg.content(), msg.createdAt());
        }
        return msg;
    }

    public List<ConversationMessage> getConversationMessages(String conversationId) {
        if (jdbc == null) {
            return List.copyOf(conversationMessages.getOrDefault(conversationId, List.of()));
        }
        return jdbc.query("SELECT id, conversation_id, role, content, created_at FROM support_conversation_messages WHERE conversation_id = ? ORDER BY created_at ASC",
            (rs, row) -> new ConversationMessage(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)), conversationId);
    }
}
