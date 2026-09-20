package com.ecom.app.dto;

import com.ecom.app.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private List<OrderItemResponse> items;
    private BigDecimal total;
    private OrderStatus status;
    private Instant createdAt;
}
