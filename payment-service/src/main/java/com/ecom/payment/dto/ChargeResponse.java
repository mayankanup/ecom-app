package com.ecom.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeResponse {
    private String id;
    private Long amount;
    private String currency;
    private String status;
    private Long created;
}
