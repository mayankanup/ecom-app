package com.ecom.payment.service;

import com.ecom.payment.dto.ChargeRequest;
import com.ecom.payment.dto.ChargeResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChargeServiceTest {

    private final ChargeService chargeService = new ChargeService();

    private static ChargeRequest request(String cardNumber) {
        ChargeRequest request = new ChargeRequest();
        request.setAmount(2999L);
        request.setCurrency("usd");
        request.setSource(cardNumber);
        return request;
    }

    @Test
    void charge_succeeds_forStandardTestCard() {
        ChargeResponse response = chargeService.charge(request("4242424242424242"));

        assertThat(response.getStatus()).isEqualTo("succeeded");
        assertThat(response.getId()).startsWith("ch_");
        assertThat(response.getAmount()).isEqualTo(2999L);
        assertThat(response.getCurrency()).isEqualTo("usd");
        assertThat(response.getCreated()).isPositive();
    }

    @Test
    void charge_fails_forStripesGenericDeclineTestCard() {
        ChargeResponse response = chargeService.charge(request("4000000000000002"));

        assertThat(response.getStatus()).isEqualTo("failed");
        assertThat(response.getId()).startsWith("ch_");
    }

    @Test
    void charge_generatesUniqueIds_acrossCalls() {
        ChargeResponse first = chargeService.charge(request("4242424242424242"));
        ChargeResponse second = chargeService.charge(request("4242424242424242"));

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }
}
