package com.ecom.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequest {

    @NotEmpty(message = "Order must contain at least one item")
    private List<@Valid OrderItemRequest> items;

    // Optional: card number to charge. Omitted (the normal case for the current checkout UI)
    // means OrderService charges a fixed "always succeeds" test card - see PaymentClient.
    private String cardNumber;
}
