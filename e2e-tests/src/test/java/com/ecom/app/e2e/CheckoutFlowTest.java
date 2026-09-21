package com.ecom.app.e2e;

import com.ecom.app.e2e.support.BaseSeleniumTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckoutFlowTest extends BaseSeleniumTest {

    @Test
    void loggedInUser_canCompleteCheckout_andSeeOrderInHistory() {
        open("/login");
        driver.findElement(By.id("email")).sendKeys("demo@example.com");
        driver.findElement(By.id("password")).sendKeys("password123");
        driver.findElement(By.xpath("//button[@type='submit']")).click();
        wait.until(d -> !d.findElements(By.xpath("//button[contains(.,'Add to cart')]")).isEmpty());

        driver.findElements(By.xpath("//button[contains(.,'Add to cart')]")).get(0).click();

        open("/checkout");
        wait.until(d -> !d.findElements(By.xpath("//button[contains(.,'Place Order')]")).isEmpty());
        assertTrue(driver.getPageSource().contains("Wireless Mouse"), "Checkout summary should list the item added to cart");

        driver.findElement(By.xpath("//button[contains(.,'Place Order')]")).click();

        wait.until(d -> d.getCurrentUrl().contains("/orders"));
        wait.until(d -> d.getPageSource().contains("PLACED"));
        assertTrue(driver.getPageSource().contains("Wireless Mouse"), "Order history should show the item just ordered");
    }
}
