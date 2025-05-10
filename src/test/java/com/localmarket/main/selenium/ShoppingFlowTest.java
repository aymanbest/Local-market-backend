package com.localmarket.main.selenium;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ShoppingFlowTest extends BaseTest {

    @Test
    @DisplayName("Complete shopping flow including payment")
    @Order(1)
    public void testTC001_CompleteShoppingFlowIncludingPayment() {
        try {
            // Navigate to the store page
            driver.get("http://localhost:5173/store");

            // Wait for products to load by waiting for the first product card
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".grid-cols-1.md\\:grid-cols-2.lg\\:grid-cols-3 > a")));

            // Wait for preloader to disappear before clicking
            waitForPreloaderToDisappear();

            // Find and click the first Plus icon button (Add to Cart)
            WebElement addToCartButton = driver.findElement(
                    By.cssSelector(".grid-cols-1.md\\:grid-cols-2.lg\\:grid-cols-3 > a button"));
            
            // Use JavaScript to click, which can bypass some overlay issues
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addToCartButton);

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

            // Complete payment
            completePayment();

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

            System.out.println("TC_001 - Complete shopping flow including payment: PASSED");

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

    @Test
    @DisplayName("Guest checkout with account creation")
    @Order(2)
    public void testTC002_GuestCheckoutWithAccountCreation() {
        try {
            // Reset session for a clean test
            resetSession();
            
            // Test ID: TC_002 - Guest checkout + account creation
            System.out.println("Running test: TC_002 - Guest checkout + account creation");
            
            // Navigate to the store page
            driver.get("http://localhost:5173/store");

            // Wait for products to load
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".grid-cols-1.md\\:grid-cols-2.lg\\:grid-cols-3 > a")));

            // Wait for preloader to disappear before clicking
            waitForPreloaderToDisappear();

            // Add product to cart
            WebElement addToCartButton = driver.findElement(
                    By.cssSelector(".grid-cols-1.md\\:grid-cols-2.lg\\:grid-cols-3 > a button"));
            
            // Use JavaScript to click, which can bypass some overlay issues
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addToCartButton);

            // Wait for cart notification
            waitForAnimations();

            // Navigate to cart page
            driver.get("http://localhost:5173/cart");

            // Wait for form loading
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type=\"email\"]")));

            // Use variables for credentials so we can reuse them for login
            String testEmail = "test_user_" + System.currentTimeMillis() + "@example.com";
            String testPassword = "Test123!";
            
            // Fill out guest checkout form - first fill the email
            WebElement emailInput = driver.findElement(By.cssSelector("input[type=\"email\"]"));
            emailInput.sendKeys(testEmail);
            
            // Wait for the Make an account checkbox to appear after email is entered
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//span[text()='Make an account?']")));
            
            // Click the Make an account checkbox
            WebElement makeAccountLabel = driver.findElement(By.xpath("//span[text()='Make an account?']/parent::label"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", makeAccountLabel);
            
            // Wait for the First Name field to appear (this is the first field in the sequence)
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//span[contains(text(), 'First Name')]/following::input")));
            
            // Fill First Name - must be filled first before other fields appear
            WebElement firstNameInput = driver.findElement(
                    By.xpath("//span[contains(text(), 'First Name')]/following::input"));
            firstNameInput.sendKeys("Test");
            
            // Wait for Last Name field to appear after filling first name
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//span[contains(text(), 'Last Name')]/following::input")));
            
            // Fill Last Name
            WebElement lastNameInput = driver.findElement(
                    By.xpath("//span[contains(text(), 'Last Name')]/following::input"));
            lastNameInput.sendKeys("User");
            
            // Wait for Username field to appear after filling last name
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//span[contains(text(), 'Username')]/following::input")));
            
            // Fill Username
            WebElement usernameInput = driver.findElement(
                    By.xpath("//span[contains(text(), 'Username')]/following::input"));
            usernameInput.sendKeys("testuser" + System.currentTimeMillis());
            
            // Wait for Password field to appear after filling username
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//span[contains(text(), 'Password')]/following::input")));
            
            // Fill Password
            WebElement passwordInput = driver.findElement(
                    By.xpath("//span[contains(text(), 'Password')]/following::input"));
            passwordInput.sendKeys(testPassword);

            // Wait for shipping input to appear
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("input[placeholder=\"Enter your shipping address\"]")));
            
            // Shipping Address
            WebElement shippingInput = driver.findElement(
                    By.cssSelector("input[placeholder=\"Enter your shipping address\"]"));
            shippingInput.sendKeys("123 Test Street");

            // Wait for phone input to appear after filling shipping
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

            // Click the Checkout/Place Order button
            WebElement checkoutButton = driver.findElement(By.cssSelector("button[type=\"submit\"]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", checkoutButton);

            Wait<WebDriver> paymentWait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(60))  // Allow up to 1 minute for payment processing
                .pollingEvery(Duration.ofSeconds(1))  // Check every second
                .ignoring(NoSuchElementException.class);
            // Wait for redirect to payment page

            paymentWait.until(ExpectedConditions.urlContains("/payment"));

            // Complete payment
            completePayment();

            // Wait for payment success
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//h1[contains(text(), 'Payment Successful!')]")));

            // Verify success message
            WebElement successMessage = driver.findElement(By.xpath("//h1[contains(text(), 'Payment Successful!')]"));
            assertTrue(successMessage.isDisplayed(), "Success message should be displayed");

            // Now test login with the created account
            // Navigate to login page
            driver.get("http://localhost:5173/login");

            // Wait for login form
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//h1[contains(text(), 'WELCOME TO OUR MARKET')]")));

            // Fill login form with the credentials used during checkout
            WebElement loginEmailInput = driver.findElement(By.cssSelector("input[type=\"text\"]"));
            loginEmailInput.sendKeys(testEmail);

            WebElement loginPasswordInput = driver.findElement(By.cssSelector("input[type=\"password\"]"));
            loginPasswordInput.sendKeys(testPassword);

            // Wait for any preloader to disappear before clicking the login button
            waitForPreloaderToDisappear();
            
            // Use JavaScript to click the button to avoid preloader interference
            WebElement loginButton = driver.findElement(By.cssSelector("button[type=\"submit\"]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loginButton);

            // Wait for redirect to home page (successful login redirects to /)
            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/"));

            // Verify we're on the main page
            assertTrue(driver.getCurrentUrl().equals("http://localhost:5173/"), 
                    "User should be redirected to home page after successful login");
            
            // Add additional verification of logged-in state by looking for the account link
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".lucide-circle-user")));
            
            System.out.println("TC_002 - Guest checkout + account creation: PASSED");

        } catch (Exception e) {
            // Take screenshot and log on failure
            if (driver instanceof JavascriptExecutor) {
                try {
                    ((JavascriptExecutor) driver).executeScript(
                            "console.error('Test failed at URL: ' + window.location.href)");
                } catch (Exception ignored) {}
            }
            
            System.err.println("TC_002 test failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    @DisplayName("Order as a logged-in member")
    @Order(3)
    public void testTC003_OrderAsRegisteredMember() {
        try {
            // Reset session for a clean test
            resetSession();
            
            // Test ID: TC_003 - Order as a logged-in member
            System.out.println("Running test: TC_003 - Order as a logged-in member");
            
            // Step 1: Register a new account
            String timestamp = String.valueOf(System.currentTimeMillis());
            String testEmail = "test_user_" + timestamp + "@example.com";
            String testUsername = "testuser" + timestamp;
            String testPassword = "Test123!";
            String testFirstName = "Test";
            String testLastName = "User";

            // Navigate to registration page
            driver.get("http://localhost:5173/register");
            
            waitForPreloaderToDisappear();
            // Wait for the registration form to load
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//h1[contains(text(), 'WELCOME TO OUR MARKET')]")));
            
            // Wait for preloader to disappear
            waitForPreloaderToDisappear();
            
            // Fill out registration form
            WebElement firstNameInput = driver.findElement(By.name("firstname"));
            firstNameInput.sendKeys(testFirstName);
            
            WebElement lastNameInput = driver.findElement(By.name("lastname"));
            lastNameInput.sendKeys(testLastName);
            
            WebElement emailInput = driver.findElement(By.name("email"));
            emailInput.sendKeys(testEmail);
            
            WebElement usernameInput = driver.findElement(By.name("username"));
            usernameInput.sendKeys(testUsername);
            
            WebElement passwordInput = driver.findElement(
                    By.xpath("//input[@name='password']"));
            passwordInput.sendKeys(testPassword);
        
            
            // Submit registration form using JavaScript click for reliability
            WebElement registerButton = driver.findElement(By.xpath("//button[@type='submit']"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", registerButton);
            
            waitForPreloaderToDisappear();
            // Wait for registration to complete and redirect to home page
            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/"));

            // Verify login successful by checking for the user menu
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".lucide-circle-user")));
            
            // Step 3: Now shop as a logged-in user
            // Navigate to the store page
            driver.get("http://localhost:5173/store");
            
            // Wait for products to load
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".grid-cols-1.md\\:grid-cols-2.lg\\:grid-cols-3 > a")));
            
            // Wait for preloader to disappear
            waitForPreloaderToDisappear();
            
            // Add a product to cart
            WebElement addToCartButton = driver.findElement(
                    By.cssSelector(".grid-cols-1.md\\:grid-cols-2.lg\\:grid-cols-3 > a button"));
            
            // Use JavaScript to click, which can bypass some overlay issues
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addToCartButton);
            
            // Wait for cart notification
            waitForAnimations();
            
            // Navigate to cart page
            driver.get("http://localhost:5173/cart");
            
            // For a logged-in user, we only need to fill out shipping address and phone number
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("input[placeholder=\"Enter your shipping address\"]")));
            
            // Fill shipping address 
            WebElement shippingInput = driver.findElement(
                    By.cssSelector("input[placeholder=\"Enter your shipping address\"]"));
            shippingInput.sendKeys("123 Test Street");
            
            // Wait for phone input to appear after filling shipping
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("input[placeholder=\"Enter your phone number\"]")));
            
            // Fill phone number
            WebElement phoneInput = driver.findElement(
                    By.cssSelector("input[placeholder=\"Enter your phone number\"]"));
            phoneInput.sendKeys("1234567890");
            
            // Wait for animations
            waitForAnimations();
            
            // Check Terms of Service checkbox
            WebElement tosCheckbox = driver.findElement(By.cssSelector("input[name=\"tos\"]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", tosCheckbox);
            
            // Wait for animations
            waitForAnimations();
            
            // Click Checkout button
            WebElement checkoutButton = driver.findElement(By.cssSelector("button[type=\"submit\"]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", checkoutButton);
            
            // Use fluentWait for potential longer redirect to payment page
            Wait<WebDriver> paymentWait = new FluentWait<>(driver)
                    .withTimeout(Duration.ofSeconds(60))  // Allow up to 1 minute for payment processing
                    .pollingEvery(Duration.ofSeconds(1))  // Check every second
                    .ignoring(NoSuchElementException.class);
                    
            // Wait for redirect to payment page
            paymentWait.until(ExpectedConditions.urlContains("/payment"));
            
            // Complete payment
            completePayment();
            
            // Wait for payment success
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//h1[contains(text(), 'Payment Successful!')]")));
            
            // Verify success message
            WebElement successMessage = driver.findElement(By.xpath("//h1[contains(text(), 'Payment Successful!')]"));
            assertTrue(successMessage.isDisplayed(), "Success message should be displayed");
            
            System.out.println("TC_003 - Order as a logged-in member: PASSED");
            
        } catch (Exception e) {
            // Take screenshot and log on failure
            if (driver instanceof JavascriptExecutor) {
                try {
                    ((JavascriptExecutor) driver).executeScript(
                            "console.error('TC_003 test failed at URL: ' + window.location.href)");
                } catch (Exception ignored) {}
            }
            
            System.err.println("TC_003 test failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            // Close the WebDriver when this test is done if running standalone
            // Only close if not part of the test suite
            if (driver != null && !(this.getClass().getName().equals("com.localmarket.main.selenium.SeleniumTestSuite"))) {
                System.out.println("Closing WebDriver for ShoppingFlowTest");
                driver.quit();
                driver = null;
            }
        }
    }
} 