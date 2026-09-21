package com.ecom.app.e2e;

import com.ecom.app.e2e.support.BaseSeleniumTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthTest extends BaseSeleniumTest {

    @Test
    void login_withWrongPassword_showsError() {
        open("/login");

        driver.findElement(By.id("email")).sendKeys("demo@example.com");
        driver.findElement(By.id("password")).sendKeys("not-the-right-password");
        driver.findElement(By.xpath("//button[@type='submit']")).click();

        wait.until(d -> d.getPageSource().contains("Invalid email or password"));
        assertTrue(driver.getCurrentUrl().contains("/login"), "A failed login should not navigate away");
    }

    @Test
    void register_thenLogout_thenLoginAgain_roundTrips() {
        String uniqueEmail = "selenium-" + System.currentTimeMillis() + "@example.com";

        open("/register");
        driver.findElement(By.id("name")).sendKeys("Selenium Tester");
        driver.findElement(By.id("email")).sendKeys(uniqueEmail);
        driver.findElement(By.id("password")).sendKeys("password123");
        driver.findElement(By.xpath("//button[@type='submit']")).click();

        wait.until(d -> d.getCurrentUrl().endsWith("/")
                && !d.findElements(By.xpath("//*[contains(.,'Selenium Tester')]")).isEmpty());

        driver.findElement(By.xpath("//button[contains(.,'Logout')]")).click();
        wait.until(d -> !d.findElements(By.linkText("Login")).isEmpty());

        open("/login");
        driver.findElement(By.id("email")).sendKeys(uniqueEmail);
        driver.findElement(By.id("password")).sendKeys("password123");
        driver.findElement(By.xpath("//button[@type='submit']")).click();

        wait.until(d -> !d.findElements(By.xpath("//*[contains(.,'Selenium Tester')]")).isEmpty());
        assertTrue(driver.getPageSource().contains("Selenium Tester"), "Re-login should succeed with the just-registered account");
    }
}
