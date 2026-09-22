package com.ecom.app.service;

import com.ecom.app.dto.OrderItemRequest;
import com.ecom.app.dto.OrderRequest;
import com.ecom.app.dto.OrderResponse;
import com.ecom.app.entity.Order;
import com.ecom.app.entity.Product;
import com.ecom.app.entity.User;
import com.ecom.app.exception.ResourceNotFoundException;
import com.ecom.app.repository.OrderRepository;
import com.ecom.app.repository.ProductRepository;
import com.ecom.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private static User demoUser() {
        return User.builder().id(1L).name("Demo User").email("demo@example.com").password("hashed").build();
    }

    private static Product product(long id, String name, String price, int stock) {
        return Product.builder().id(id).name(name).price(new BigDecimal(price)).stock(stock).build();
    }

    private static OrderRequest requestFor(long productId, int quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        OrderRequest request = new OrderRequest();
        request.setItems(List.of(item));
        return request;
    }

    @Test
    void placeOrder_computesTotalAndDecrementsStock() {
        User user = demoUser();
        Product mouse = product(1L, "Mouse", "19.99", 10);

        when(userRepository.findByEmail("demo@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mouse));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(100L);
            return o;
        });

        OrderResponse response = orderService.placeOrder("demo@example.com", requestFor(1L, 3));

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getTotal()).isEqualByComparingTo(new BigDecimal("59.97"));
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(mouse.getStock()).isEqualTo(7);
    }

    @Test
    void placeOrder_throwsNotFound_whenProductMissing() {
        when(userRepository.findByEmail("demo@example.com")).thenReturn(Optional.of(demoUser()));
        when(productRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder("demo@example.com", requestFor(99L, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void placeOrder_throwsConflict_whenInsufficientStock() {
        Product mouse = product(1L, "Mouse", "19.99", 2);
        when(userRepository.findByEmail("demo@example.com")).thenReturn(Optional.of(demoUser()));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mouse));

        assertThatThrownBy(() -> orderService.placeOrder("demo@example.com", requestFor(1L, 5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void placeOrder_locksProductsInAscendingIdOrder_regardlessOfRequestOrder() {
        User user = demoUser();
        Product keyboard = product(5L, "Keyboard", "59.99", 10);
        Product mouse = product(2L, "Mouse", "19.99", 10);

        when(userRepository.findByEmail("demo@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(keyboard));
        when(productRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(mouse));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderItemRequest first = new OrderItemRequest();
        first.setProductId(5L);
        first.setQuantity(1);
        OrderItemRequest second = new OrderItemRequest();
        second.setProductId(2L);
        second.setQuantity(1);
        OrderRequest request = new OrderRequest();
        request.setItems(List.of(first, second));

        orderService.placeOrder("demo@example.com", request);

        InOrder order = inOrder(productRepository);
        order.verify(productRepository).findByIdForUpdate(2L);
        order.verify(productRepository).findByIdForUpdate(5L);
    }

    @Test
    void getOrdersForUser_mapsOrdersForThatUserOnly() {
        User user = demoUser();
        when(userRepository.findByEmail("demo@example.com")).thenReturn(Optional.of(user));

        Product mouse = product(1L, "Mouse", "19.99", 5);
        Order order = Order.builder().id(7L).user(user).total(new BigDecimal("19.99")).build();
        order.addItem(com.ecom.app.entity.OrderItem.builder()
                .product(mouse).quantity(1).priceAtPurchase(new BigDecimal("19.99")).build());
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(order));

        List<OrderResponse> orders = orderService.getOrdersForUser("demo@example.com");

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getId()).isEqualTo(7L);
        assertThat(orders.get(0).getItems().get(0).getProductName()).isEqualTo("Mouse");
    }
}
