package com.example.shop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local-demo")
class CommerceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.backend", is("java")));
    }

    @Test
    void productsEndpointReturnsCatalog() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.total", greaterThan(0)));
    }

    @Test
    void customerLoginAndGetProfile() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"customer@example.com\",\"password\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.email", is("customer@example.com")))
                .andReturn().getResponse().getContentAsString();

        // Extract token
        String token = response.split("\"access_token\":\"")[1].split("\"")[0];

        // Access protected cart endpoint
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", notNullValue()));

        // Access support tickets
        mockMvc.perform(get("/api/support/my-tickets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", notNullValue()));
    }

    @Test
    void cartAddAndCheckoutFlow() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"customer@example.com\",\"password\":\"\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = loginResponse.split("\"access_token\":\"")[1].split("\"")[0];

        // Add item 1 to cart
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":\"1\",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThan(0))));

        // Checkout
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", startsWith("ord-java-")))
                .andExpect(jsonPath("$.status", is("PAID")));

        // List orders
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThan(0))));
    }

    @Test
    void chatEndpointReturnsSseStream() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"你好，请问有运动鞋吗？\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event: text_delta")));
    }

    @Test
    void adminOperationsFlow() throws Exception {
        // Login as merchant
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"merchant@example.com\",\"password\":\"\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String merchantToken = loginResponse.split("\"access_token\":\"")[1].split("\"")[0];

        // 1. Initialize catalog
        mockMvc.perform(post("/api/admin/catalog/initialize")
                        .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INITIALIZED")));

        // 2. Get customer groups
        mockMvc.perform(get("/api/admin/customer-groups")
                        .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(3))));

        // 3. Get admin customers
        mockMvc.perform(get("/api/admin/customers")
                        .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.items[0].customer_group", notNullValue()));

        // 4. Send chat message to create conversation
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"测试咨询工单和历史记录\",\"conversation_id\":\"test-conv-001\"}"))
                .andExpect(status().isOk());

        // 5. Check admin conversations
        mockMvc.perform(get("/api/admin/conversations")
                        .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThan(0))));

        // 6. Check admin conversation messages
        mockMvc.perform(get("/api/admin/conversations/test-conv-001/messages")
                        .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages", hasSize(greaterThan(0))));

        // 7. Check catalog terms
        mockMvc.perform(get("/api/catalog/terms/rma-reasons")
                        .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThan(0))));
    }
}
