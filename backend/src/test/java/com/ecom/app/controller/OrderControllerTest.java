package com.ecom.app.controller;

import com.ecom.app.config.SecurityConfig;
import com.ecom.app.dto.OrderItemResponse;
import com.ecom.app.dto.OrderResponse;
import com.ecom.app.entity.OrderStatus;
import com.ecom.app.repository.UserRepository;
import com.ecom.app.security.CustomUserDetailsService;
import com.ecom.app.security.JwtAuthFilter;
import com.ecom.app.security.JwtUtil;
import com.ecom.app.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, CustomUserDetailsService.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void placeOrder_returns401_withoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":2}]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "demo@example.com")
    void placeOrder_returns201_whenAuthenticated() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .items(List.of(OrderItemResponse.builder()
                        .productId(1L).productName("Mouse").quantity(2)
                        .priceAtPurchase(new BigDecimal("19.99")).lineTotal(new BigDecimal("39.98")).build()))
                .total(new BigDecimal("39.98"))
                .status(OrderStatus.PLACED)
                .createdAt(Instant.now())
                .build();
        when(orderService.placeOrder(eq("demo@example.com"), any())).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(39.98));
    }

    @Test
    void getMyOrders_returns401_withoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/orders/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "demo@example.com")
    void getMyOrders_returns200_whenAuthenticated() throws Exception {
        when(orderService.getOrdersForUser("demo@example.com")).thenReturn(List.of());

        mockMvc.perform(get("/api/orders/my"))
                .andExpect(status().isOk());
    }
}
