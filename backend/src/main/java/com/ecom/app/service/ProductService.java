package com.ecom.app.service;

import com.ecom.app.dto.ProductResponse;
import com.ecom.app.entity.Product;
import com.ecom.app.exception.ResourceNotFoundException;
import com.ecom.app.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductService::toResponse)
                .toList();
    }

    public ProductResponse getProductById(Long id) {
        return toResponse(getProductEntity(id));
    }

    public Product getProductEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
    }

    private static ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .stock(product.getStock())
                .build();
    }
}
