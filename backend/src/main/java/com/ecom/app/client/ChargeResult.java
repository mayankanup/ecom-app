package com.ecom.app.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChargeResult {
    private String id;
    private Long amount;
    private String currency;
    private String status;
    private Long created;

    public boolean isSucceeded() {
        return "succeeded".equals(status);
    }
}
