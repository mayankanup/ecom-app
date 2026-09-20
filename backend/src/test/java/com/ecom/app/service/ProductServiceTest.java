package com.ecom.app.service;

import com.ecom.app.dto.ProductResponse;
import com.ecom.app.entity.Product;
import com.ecom.app.exception.ResourceNotFoundException;
import com.ecom.app.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getAllProducts_mapsEntitiesToResponses() {
        Product product = Product.builder()
                .id(1L).name("Mouse").description("desc")
                .price(new BigDecimal("19.99")).imageUrl("img").stock(10)
                .build();
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> result = productService.getAllProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("Mouse");
        assertThat(result.get(0).getPrice()).isEqualTo(new BigDecimal("19.99"));
    }

    @Test
    void getProductById_returnsResponse_whenFound() {
        Product product = Product.builder()
                .id(5L).name("Keyboard").description("desc")
                .price(new BigDecimal("59.99")).imageUrl("img").stock(3)
                .build();
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        ProductResponse result = productService.getProductById(5L);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getName()).isEqualTo("Keyboard");
    }

    @Test
    void getProductById_throwsNotFound_whenMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
