package com.ecom.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChargeRequest {

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private Long amount;

    @NotBlank(message = "currency is required")
    private String currency;

    @NotBlank(message = "source is required")
    private String source;
}
