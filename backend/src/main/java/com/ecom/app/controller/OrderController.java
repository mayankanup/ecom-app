package com.ecom.app.controller;

import com.ecom.app.dto.OrderRequest;
import com.ecom.app.dto.OrderResponse;
import com.ecom.app.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@AuthenticationPrincipal UserDetails principal, @Valid @RequestBody OrderRequest request) {
        return orderService.placeOrder(principal.getUsername(), request);
    }

    @GetMapping("/my")
    public List<OrderResponse> getMyOrders(@AuthenticationPrincipal UserDetails principal) {
        return orderService.getOrdersForUser(principal.getUsername());
    }
}
