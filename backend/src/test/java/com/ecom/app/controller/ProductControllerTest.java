package com.ecom.app.controller;

import com.ecom.app.config.SecurityConfig;
import com.ecom.app.dto.ProductResponse;
import com.ecom.app.exception.ResourceNotFoundException;
import com.ecom.app.repository.UserRepository;
import com.ecom.app.security.CustomUserDetailsService;
import com.ecom.app.security.JwtAuthFilter;
import com.ecom.app.security.JwtUtil;
import com.ecom.app.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, CustomUserDetailsService.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void getAllProducts_isPubliclyAccessible() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(
                ProductResponse.builder().id(1L).name("Mouse").price(new BigDecimal("19.99")).stock(10).build()
        ));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Mouse"));
    }

    @Test
    void getProductById_returns404_whenMissing() throws Exception {
        when(productService.getProductById(99L)).thenThrow(new ResourceNotFoundException("Product not found with id 99"));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id 99"));
    }
}
