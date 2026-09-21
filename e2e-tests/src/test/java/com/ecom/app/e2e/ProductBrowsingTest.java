package com.ecom.app.e2e;

import com.ecom.app.e2e.support.BaseSeleniumTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductBrowsingTest extends BaseSeleniumTest {

    @Test
    void homePage_listsSeededProducts_withoutLogin() {
        open("/");

        wait.until(d -> !d.findElements(By.cssSelector(".card")).isEmpty());

        List<org.openqa.selenium.WebElement> cards = driver.findElements(By.cssSelector(".card"));
        assertFalse(cards.isEmpty(), "Expected at least one product card on the home page");

        assertTrue(driver.getPageSource().contains("Wireless Mouse"), "Expected seeded product 'Wireless Mouse' to be visible");
        assertTrue(driver.findElements(By.linkText("Login")).size() == 1, "Guest should see a Login link, not be forced to authenticate");
    }

    @Test
    void productDetail_isReachable_withoutLogin() {
        open("/");
        wait.until(d -> !d.findElements(By.linkText("Wireless Mouse")).isEmpty());

        driver.findElement(By.linkText("Wireless Mouse")).click();

        wait.until(d -> d.getCurrentUrl().contains("/products/"));
        wait.until(d -> !d.findElements(By.xpath("//button[contains(.,'Add to cart')]")).isEmpty());
        assertTrue(driver.getPageSource().contains("Wireless Mouse"));
    }
}
