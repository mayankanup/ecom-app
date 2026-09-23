package com.ecom.payment.service;

import com.ecom.payment.dto.ChargeRequest;
import com.ecom.payment.dto.ChargeResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ChargeService {

    // Stripe's real, publicly-documented generic-decline test card. Any other card number
    // is treated as succeeding, matching how Stripe's own test mode behaves.
    private static final String DECLINE_CARD_SUFFIX = "0002";

    public ChargeResponse charge(ChargeRequest request) {
        boolean approved = !request.getSource().endsWith(DECLINE_CARD_SUFFIX);

        return ChargeResponse.builder()
                .id("ch_" + UUID.randomUUID().toString().replace("-", ""))
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(approved ? "succeeded" : "failed")
                .created(Instant.now().getEpochSecond())
                .build();
    }
}
