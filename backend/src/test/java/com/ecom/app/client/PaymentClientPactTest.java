package com.ecom.app.client;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// The consumer side of the contract: describes exactly what backend expects from
// payment-service's /api/v1/charges, and asserts PaymentClient correctly parses a response
// matching that shape. Generates pacts/backend-payment-service.json (see backend/pom.xml's
// surefire config), which payment-service's provider verification test then replays against
// the real controller - see payment-service/src/test/java/com/ecom/payment/PaymentServiceProviderPactTest.java.
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "payment-service", pactVersion = PactSpecVersion.V3)
class PaymentClientPactTest {

    // Must match payment-service's default app.api-key (application.yml in both modules) so
    // the provider verification test - which runs payment-service with its real default config,
    // not a special test override - actually authenticates these replayed requests.
    private static final String TEST_API_KEY = "sk_test_51_local_dev_key";

    @Pact(consumer = "backend", provider = "payment-service")
    public RequestResponsePact succeededCharge(PactDslWithProvider builder) {
        return builder
                .given("a card that will be approved")
                .uponReceiving("a charge request for a valid card")
                .path("/api/v1/charges")
                .method("POST")
                .headers("Authorization", "Bearer " + TEST_API_KEY, "Content-Type", "application/json")
                .body(new PactDslJsonBody()
                        .numberType("amount", 2999)
                        .stringType("currency", "usd")
                        .stringType("source", "4242424242424242"))
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringType("id", "ch_abc123")
                        .numberType("amount", 2999)
                        .stringType("currency", "usd")
                        .stringMatcher("status", "succeeded|failed", "succeeded")
                        .numberType("created", 1700000000))
                .toPact();
    }

    @Pact(consumer = "backend", provider = "payment-service")
    public RequestResponsePact declinedCharge(PactDslWithProvider builder) {
        return builder
                .given("a card that will be declined")
                .uponReceiving("a charge request for a card that gets declined")
                .path("/api/v1/charges")
                .method("POST")
                .headers("Authorization", "Bearer " + TEST_API_KEY, "Content-Type", "application/json")
                .body(new PactDslJsonBody()
                        .numberType("amount", 2999)
                        .stringType("currency", "usd")
                        .stringType("source", "4000000000000002"))
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringType("id", "ch_def456")
                        .numberType("amount", 2999)
                        .stringType("currency", "usd")
                        .stringMatcher("status", "succeeded|failed", "failed")
                        .numberType("created", 1700000000))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "succeededCharge")
    void paymentClient_parsesSucceededCharge(MockServer mockServer) {
        PaymentClient client = new PaymentClient(RestClient.builder(), mockServer.getUrl(), TEST_API_KEY);

        ChargeResult result = client.charge(ChargeRequest.builder()
                .amount(2999L).currency("usd").source("4242424242424242").build());

        assertThat(result.getStatus()).isEqualTo("succeeded");
        assertThat(result.isSucceeded()).isTrue();
        assertThat(result.getId()).isNotBlank();
    }

    @Test
    @PactTestFor(pactMethod = "declinedCharge")
    void paymentClient_parsesDeclinedCharge(MockServer mockServer) {
        PaymentClient client = new PaymentClient(RestClient.builder(), mockServer.getUrl(), TEST_API_KEY);

        ChargeResult result = client.charge(ChargeRequest.builder()
                .amount(2999L).currency("usd").source("4000000000000002").build());

        assertThat(result.getStatus()).isEqualTo("failed");
        assertThat(result.isSucceeded()).isFalse();
    }
}
