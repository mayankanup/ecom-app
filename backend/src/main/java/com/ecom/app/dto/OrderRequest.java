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
}
