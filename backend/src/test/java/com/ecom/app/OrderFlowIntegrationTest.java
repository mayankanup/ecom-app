package com.ecom.app;

import com.ecom.app.client.ChargeResult;
import com.ecom.app.client.PaymentClient;
import com.ecom.app.dto.*;
import com.ecom.app.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// PaymentClient is mocked so this test class stays fully self-contained - no payment-service
// needs to be running for `./mvnw test` to pass standalone (unlike e2e-tests/perf-tests, which
// hit a real running backend and therefore do need payment-service up).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class OrderFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @MockitoBean
    private PaymentClient paymentClient;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void registerLoginPlaceOrderAndFetchHistory_endToEnd() {
        when(paymentClient.charge(any())).thenReturn(new ChargeResult("ch_test", 0L, "usd", "succeeded", 0L));

        ProductResponse product = restTemplate.getForObject(baseUrl() + "/api/products", ProductResponse[].class)[0];
        int stockBefore = product.getStock();

        RegisterRequest register = new RegisterRequest();
        register.setName("Integration Tester");
        register.setEmail("integration-tester@example.com");
        register.setPassword("password123");
        ResponseEntity<UserResponse> registerResponse = restTemplate.postForEntity(
                baseUrl() + "/api/auth/register", register, UserResponse.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        LoginRequest login = new LoginRequest();
        login.setEmail("integration-tester@example.com");
        login.setPassword("password123");
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                baseUrl() + "/api/auth/login", login, AuthResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResponse.getBody().getToken();
        assertThat(token).isNotBlank();

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(product.getId());
        itemRequest.setQuantity(2);
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setItems(List.of(itemRequest));

        ResponseEntity<OrderResponse> orderResponse = restTemplate.postForEntity(
                baseUrl() + "/api/orders", new HttpEntity<>(orderRequest, authHeaders), OrderResponse.class);
        assertThat(orderResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        OrderResponse placedOrder = orderResponse.getBody();
        assertThat(placedOrder.getTotal()).isEqualByComparingTo(product.getPrice().multiply(BigDecimal.valueOf(2)));
        assertThat(placedOrder.getStatus().name()).isEqualTo("PLACED");

        ResponseEntity<OrderResponse[]> historyResponse = restTemplate.exchange(
                baseUrl() + "/api/orders/my", HttpMethod.GET, new HttpEntity<>(authHeaders), OrderResponse[].class);
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(historyResponse.getBody()).extracting(OrderResponse::getId).contains(placedOrder.getId());

        int stockAfter = productRepository.findById(product.getId()).orElseThrow().getStock();
        assertThat(stockAfter).isEqualTo(stockBefore - 2);
    }

    @Test
    void placingOrder_withDeclinedPayment_returnsPaymentRequired_andDoesNotDecrementStock() {
        when(paymentClient.charge(any())).thenReturn(new ChargeResult("ch_test", 0L, "usd", "failed", 0L));

        ProductResponse product = restTemplate.getForObject(baseUrl() + "/api/products", ProductResponse[].class)[0];
        int stockBefore = product.getStock();

        RegisterRequest register = new RegisterRequest();
        register.setName("Declined Tester");
        register.setEmail("declined-tester@example.com");
        register.setPassword("password123");
        restTemplate.postForEntity(baseUrl() + "/api/auth/register", register, UserResponse.class);

        LoginRequest login = new LoginRequest();
        login.setEmail("declined-tester@example.com");
        login.setPassword("password123");
        String token = restTemplate.postForEntity(baseUrl() + "/api/auth/login", login, AuthResponse.class)
                .getBody().getToken();

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(product.getId());
        itemRequest.setQuantity(1);
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setItems(List.of(itemRequest));

        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                baseUrl() + "/api/orders", new HttpEntity<>(orderRequest, authHeaders), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);

        int stockAfter = productRepository.findById(product.getId()).orElseThrow().getStock();
        assertThat(stockAfter).isEqualTo(stockBefore);
    }

    @Test
    void placingOrder_withoutToken_isRejected() {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(1);
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setItems(List.of(itemRequest));

        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl() + "/api/orders", orderRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
