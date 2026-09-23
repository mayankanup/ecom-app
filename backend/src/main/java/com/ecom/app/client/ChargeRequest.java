package com.ecom.app.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Mirrors payment-service's request shape (Stripe's own charges API convention: amount in
// minor units/cents, source = card number). Internal to the backend - never exposed to clients.
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeRequest {
    private Long amount;
    private String currency;
    private String source;
}
