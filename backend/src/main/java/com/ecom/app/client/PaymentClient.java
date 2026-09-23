package com.ecom.app.client;

import com.ecom.app.exception.PaymentServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Talks to payment-service the way this app would talk to real Stripe: a base URL and a
// bearer API key, both externalized so a real deployment could point this at the actual
// Stripe API by changing config, not code.
@Component
public class PaymentClient {

    private final RestClient restClient;
    private final String apiKey;

    public PaymentClient(
            RestClient.Builder builder,
            @Value("${payment.service.base-url}") String baseUrl,
            @Value("${payment.service.api-key}") String apiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public ChargeResult charge(ChargeRequest request) {
        try {
            return restClient.post()
                    .uri("/api/v1/charges")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(ChargeResult.class);
        } catch (RestClientException ex) {
            throw new PaymentServiceUnavailableException("Could not reach the payment service", ex);
        }
    }
}
