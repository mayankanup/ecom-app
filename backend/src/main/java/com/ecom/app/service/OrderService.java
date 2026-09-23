package com.ecom.app.service;

import com.ecom.app.client.ChargeRequest;
import com.ecom.app.client.ChargeResult;
import com.ecom.app.client.PaymentClient;
import com.ecom.app.dto.OrderItemRequest;
import com.ecom.app.dto.OrderItemResponse;
import com.ecom.app.dto.OrderRequest;
import com.ecom.app.dto.OrderResponse;
import com.ecom.app.entity.Order;
import com.ecom.app.entity.OrderItem;
import com.ecom.app.entity.Product;
import com.ecom.app.entity.User;
import com.ecom.app.exception.PaymentDeclinedException;
import com.ecom.app.exception.ResourceNotFoundException;
import com.ecom.app.repository.OrderRepository;
import com.ecom.app.repository.ProductRepository;
import com.ecom.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    // Stripe's real "always succeeds" test card - used whenever the caller (currently the
    // checkout UI) doesn't specify one, so ordinary checkout keeps working without a card form.
    private static final String DEFAULT_TEST_CARD = "4242424242424242";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentClient paymentClient;

    @Transactional
    public OrderResponse placeOrder(String userEmail, OrderRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Order order = Order.builder().user(user).total(BigDecimal.ZERO).build();
        BigDecimal total = BigDecimal.ZERO;

        // Lock products in a consistent (ascending id) order across all transactions, so two
        // multi-item orders sharing products can never deadlock waiting on each other's locks.
        List<OrderItemRequest> items = request.getItems().stream()
                .sorted(Comparator.comparing(OrderItemRequest::getProductId))
                .toList();

        for (OrderItemRequest itemRequest : items) {
            Product product = productRepository.findByIdForUpdate(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + itemRequest.getProductId()));

            if (product.getStock() < itemRequest.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for product '" + product.getName() + "'");
            }

            product.setStock(product.getStock() - itemRequest.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .priceAtPurchase(product.getPrice())
                    .build();
            order.addItem(orderItem);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }

        order.setTotal(total);

        String cardNumber = request.getCardNumber() != null && !request.getCardNumber().isBlank()
                ? request.getCardNumber()
                : DEFAULT_TEST_CARD;
        long amountInCents = total.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
        ChargeResult charge = paymentClient.charge(ChargeRequest.builder()
                .amount(amountInCents)
                .currency("usd")
                .source(cardNumber)
                .build());
        if (!charge.isSucceeded()) {
            throw new PaymentDeclinedException("Payment was declined for this card");
        }

        Order saved = orderRepository.save(order);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(OrderService::toResponse)
                .toList();
    }

    private static OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .priceAtPurchase(item.getPriceAtPurchase())
                        .lineTotal(item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .items(items)
                .total(order.getTotal())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
