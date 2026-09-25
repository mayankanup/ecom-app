package com.ecom.app.karate;

import com.intuit.karate.junit5.Karate;

class OrdersTest {

    @Karate.Test
    Karate testOrders() {
        return Karate.run("classpath:karate/orders.feature");
    }
}
