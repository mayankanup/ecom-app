package com.ecom.payment.controller;

import com.ecom.payment.dto.ChargeRequest;
import com.ecom.payment.dto.ChargeResponse;
import com.ecom.payment.exception.UnauthorizedException;
import com.ecom.payment.service.ChargeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChargeController {

    private final ChargeService chargeService;

    @Value("${app.api-key}")
    private String apiKey;

    @PostMapping("/charges")
    public ChargeResponse charge(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ChargeRequest request) {
        if (authorization == null || !authorization.equals("Bearer " + apiKey)) {
            throw new UnauthorizedException("Invalid API key provided");
        }
        return chargeService.charge(request);
    }
}
