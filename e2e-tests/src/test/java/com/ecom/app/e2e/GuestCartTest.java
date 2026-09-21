package com.ecom.app.e2e;

import com.ecom.app.e2e.support.BaseSeleniumTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GuestCartTest extends BaseSeleniumTest {

    @Test
    void addingToCartAsGuest_updatesBadge_andCheckoutRedirectsToLogin() {
        open("/");
        wait.until(d -> !d.findElements(By.xpath("//button[contains(.,'Add to cart')]")).isEmpty());

        WebElement firstAddToCart = driver.findElements(By.xpath("//button[contains(.,'Add to cart')]")).get(0);
        firstAddToCart.click();

        wait.until(d -> !d.findElements(By.cssSelector(".badge.bg-primary")).isEmpty());
        WebElement cartBadge = driver.findElement(By.cssSelector(".badge.bg-primary"));
        assertTrue(cartBadge.getText().contains("1"), "Cart badge should show 1 item after adding a product");

        open("/cart");
        wait.until(d -> d.getPageSource().contains("Total"));
        assertTrue(driver.getPageSource().contains("Wireless Mouse"), "Cart page should list the added product");

        driver.findElement(By.xpath("//button[contains(.,'Proceed to Checkout')]")).click();

        wait.until(d -> d.getCurrentUrl().contains("/login"));
        assertTrue(driver.getCurrentUrl().contains("redirect="), "Login redirect should preserve where the guest came from");
    }
}
