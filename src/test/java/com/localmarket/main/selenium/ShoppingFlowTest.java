package com.localmarket.main.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ShoppingFlowTest {
    private WebDriver driver;
    private WebDriverWait wait;
    private Wait<WebDriver> fluentWait;

    @BeforeAll
    public void setUp() {
        // Set up WebDriverManager for ChromeDriver
        WebDriverManager.chromedriver().setup();
        
        // Chrome WebDriver setup
        ChromeOptions options = new ChromeOptions();
        // Add options if needed (headless, disable-gpu, etc.)
        // options.addArguments("--headless");
        
        driver = new ChromeDriver(options);
        
        // Standard wait (10 seconds)
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Fluent wait configuration (useful for animations and slower operations)
        fluentWait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(30))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
                
        // Maximize window for better element visibility
        driver.manage().window().maximize();
    }

    @AfterAll
    public void tearDown() {
        // Close the browser after tests
        if (driver != null) {
            driver.quit();
        }
    }
    
    /**
     * Wait for React-based animations and state changes to complete
     */
    private void waitForAnimations() {
        try {
            // Wait for document ready state
            ExpectedCondition<Boolean> pageLoadCondition = driver -> {
                return ((JavascriptExecutor) driver)
                        .executeScript("return document.readyState").equals("complete");
            };
            
            // Wait for document ready first
            fluentWait.until(pageLoadCondition);

            try {
                // Brief pause to allow React to process state changes
                Thread.sleep(500);
                
                // Additional check: wait for any loading indicators to disappear
                wait.until(ExpectedConditions.invisibilityOfElementLocated(
                        By.cssSelector("[data-loading='true'], .loading, .animate-spin")));
            } catch (Exception e) {
                // Loading indicator might not be present, that's fine
            }
        } catch (TimeoutException e) {
            System.out.println("Page animations timed out, continuing...");
        }
    }

    @Test
    public void testCompleteShoppingFlowIncludingPayment() {
        try {
            // Navigate to the store page
            driver.get("http://localhost:5173/store");

            // Wait for products to load by waiting for the first product card
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".grid-cols-1.md\\:grid-cols-2.lg\\:grid-cols-3 > a")));

            // Find and click the first Plus icon button (Add to Cart)
            WebElement addToCartButton = driver.findElement(
                    By.cssSelector(".grid-cols-1.md\\:grid-cols-2.lg\\:grid-cols-3 > a button"));
            addToCartButton.click();

            // Use fluent wait for cart notification/update
            waitForAnimations();

            // Navigate directly to cart page
            driver.get("http://localhost:5173/cart");

            // Wait for the form to be loaded
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type=\"email\"]")));

            // Fill out the guest checkout form
            // Email
            WebElement emailInput = driver.findElement(By.cssSelector("input[type=\"email\"]"));
            emailInput.sendKeys("test@example.com");

            // Wait for shipping input to be visible
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("input[placeholder=\"Enter your shipping address\"]")));
            
            // Shipping Address
            WebElement shippingInput = driver.findElement(
                    By.cssSelector("input[placeholder=\"Enter your shipping address\"]"));
            shippingInput.sendKeys("123 Test Street");

            // Wait for phone input to be visible
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("input[placeholder=\"Enter your phone number\"]")));
            
            // Phone Number
            WebElement phoneInput = driver.findElement(
                    By.cssSelector("input[placeholder=\"Enter your phone number\"]"));
            phoneInput.sendKeys("1234567890");

            // Wait for animations
            waitForAnimations();

            // Check "I agree to the Terms of Service"
            WebElement tosCheckbox = driver.findElement(By.cssSelector("input[name=\"tos\"]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", tosCheckbox);

            // Wait for animations
            waitForAnimations();

            // Click the Checkout button using JavaScript
            WebElement checkoutButton = driver.findElement(By.cssSelector("button[type=\"submit\"]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", checkoutButton);

            // Use fluent wait for checkout processing and redirection
            // This wait allows more time for API calls and SMTP processing
            Wait<WebDriver> checkoutWait = new FluentWait<>(driver)
                    .withTimeout(Duration.ofSeconds(120))  // Allow up to 2 minutes for checkout
                    .pollingEvery(Duration.ofSeconds(2))   // Check every 2 seconds
                    .ignoring(NoSuchElementException.class);
                    
            // Wait for redirect to payment page
            checkoutWait.until(webDriver -> webDriver.getCurrentUrl().contains("/payment"));

            // Assert that we've been redirected to payment page
            assertTrue(driver.getCurrentUrl().contains("/payment"), 
                    "User should be redirected to payment page");

            // Wait for payment form to load
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("input[placeholder=\"John Doe\"]")));

            // Fill out payment form
            // Card Holder Name
            WebElement cardHolderInput = driver.findElement(By.cssSelector("input[placeholder=\"John Doe\"]"));
            cardHolderInput.sendKeys("John Doe");

            // Card Number
            WebElement cardNumberInput = driver.findElement(
                    By.cssSelector("input[placeholder=\"1234 5678 9012 3456\"]"));
            cardNumberInput.sendKeys("1234567890123456");

            // Expiry Date
            WebElement expiryInput = driver.findElement(By.cssSelector("input[placeholder=\"MM/YY\"]"));
            expiryInput.sendKeys("1225");

            // CVV
            WebElement cvvInput = driver.findElement(By.cssSelector("input[placeholder=\"•••\"]"));
            cvvInput.sendKeys("123");

            // Wait for animations
            waitForAnimations();

            // Click Pay Securely Now button
            WebElement payButton = driver.findElement(By.cssSelector("button[type=\"submit\"]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", payButton);

            // Use fluent wait for payment processing
            Wait<WebDriver> paymentWait = new FluentWait<>(driver)
                    .withTimeout(Duration.ofSeconds(60))  // Allow up to 1 minute for payment processing
                    .pollingEvery(Duration.ofSeconds(1))  // Check every second
                    .ignoring(NoSuchElementException.class);
                    
            // Wait for the success message to appear
            paymentWait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//h1[contains(text(), 'Payment Successful!')]")));

            // Verify the success message is displayed
            WebElement successMessage = driver.findElement(By.xpath("//h1[contains(text(), 'Payment Successful!')]"));
            String messageText = successMessage.getText();
            assertEquals("Payment Successful!", messageText, "Success message should be displayed");

        } catch (Exception e) {
            // Take screenshot on failure
            if (driver instanceof JavascriptExecutor) {
                try {
                    ((JavascriptExecutor) driver).executeScript(
                            "console.error('Test failed at URL: ' + window.location.href)");
                } catch (Exception ignored) {}
            }
            
            // Log the error and rethrow
            System.err.println("Test failed: " + e.getMessage());
            throw e;
        }
    }
} 