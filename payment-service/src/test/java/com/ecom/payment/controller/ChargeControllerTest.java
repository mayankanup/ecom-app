package com.ecom.payment.controller;

import com.ecom.payment.service.ChargeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChargeController.class)
class ChargeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChargeService chargeService;

    private static final String VALID_BODY =
            "{\"amount\":2999,\"currency\":\"usd\",\"source\":\"4242424242424242\"}";

    @Test
    void charge_returns401_whenAuthorizationHeaderMissing() throws Exception {
        mockMvc.perform(post("/api/v1/charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.type").value("authentication_error"));
    }

    @Test
    void charge_returns401_whenApiKeyWrong() throws Exception {
        mockMvc.perform(post("/api/v1/charges")
                        .header("Authorization", "Bearer sk_test_wrong_key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void charge_returns400_whenAmountMissing() throws Exception {
        mockMvc.perform(post("/api/v1/charges")
                        .header("Authorization", "Bearer sk_test_51_local_dev_key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"usd\",\"source\":\"4242424242424242\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.type").value("invalid_request_error"));
    }
}
