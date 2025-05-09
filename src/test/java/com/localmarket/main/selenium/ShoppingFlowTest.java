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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ShoppingFlowTest {
    private WebDriver driver;
    private WebDriverWait wait;
    private Wait<WebDriver> fluentWait;

    // Static variables to store seller credentials across test methods
    private static String sellerEmail = null;
    private static String sellerPassword = null;

    // Static variables to store customer credential and order number across test methods
    private static String customerEmail = null;
    private static String customerPassword = "customer123";
    private static String orderNumber = null;

    // WebDriver instances for different user sessions
    private WebDriver producerDriver = null;
    private WebDriver adminDriver = null;

    // Customer credentials for review tests
    private static String reviewCustomerEmail = null;
    private static String reviewCustomerUsername = null;
    private static String reviewOrderNumber1 = null;
    private static String reviewOrderNumber2 = null;

    @BeforeAll
    public void setUp() {
        // Suppress Selenium CDP warning messages
        Logger.getLogger("org.openqa.selenium").setLevel(Level.SEVERE);
        System.setProperty("webdriver.chrome.silentOutput", "true");
        
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
        // Close all browser instances
        if (driver != null) {
            driver.quit();
        }
        if (producerDriver != null) {
            producerDriver.quit();
        }
        if (adminDriver != null) {
            adminDriver.quit();
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

    /**
     * Wait specifically for the preloader overlay to disappear
     */
    private void waitForPreloaderToDisappear() {
        try {
            // Wait for the preloader overlay to disappear
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("div.fixed.inset-0.z-50.flex.flex-col.items-center.justify-center.opacity-100")));
            
            // Additional check with more specific selector
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("div.fixed.inset-0.z-50.flex.flex-col.items-center.justify-center")));
            
            // Add a small delay to ensure animations are complete
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("Waiting for preloader timed out: " + e.getMessage());
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

    @Test
    public void testTC002_GuestCheckoutWithAccountCreation() {
        try {
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

            // Fill payment form
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("input[placeholder=\"John Doe\"]")));

            WebElement cardHolderInput = driver.findElement(By.cssSelector("input[placeholder=\"John Doe\"]"));
            cardHolderInput.sendKeys("Test User");

            WebElement cardNumberInput = driver.findElement(
                    By.cssSelector("input[placeholder=\"1234 5678 9012 3456\"]"));
            cardNumberInput.sendKeys("1234567890123456");

            WebElement expiryInput = driver.findElement(By.cssSelector("input[placeholder=\"MM/YY\"]"));
            expiryInput.sendKeys("1225");

            WebElement cvvInput = driver.findElement(By.cssSelector("input[placeholder=\"•••\"]"));
            cvvInput.sendKeys("123");

            // Wait for animations
            waitForAnimations();

            // Click Pay Securely Now button
            WebElement payButton = driver.findElement(By.cssSelector("button[type=\"submit\"]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", payButton);

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
    public void testTC003_OrderAsRegisteredMember() {
        try {
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
            
            // Fill payment form
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("input[placeholder=\"John Doe\"]")));
            
            WebElement cardHolderInput = driver.findElement(By.cssSelector("input[placeholder=\"John Doe\"]"));
            cardHolderInput.sendKeys(testFirstName + " " + testLastName);
            
            WebElement cardNumberInput = driver.findElement(
                    By.cssSelector("input[placeholder=\"1234 5678 9012 3456\"]"));
            cardNumberInput.sendKeys("1234567890123456");
            
            WebElement expiryInput = driver.findElement(By.cssSelector("input[placeholder=\"MM/YY\"]"));
            expiryInput.sendKeys("1225");
            
            WebElement cvvInput = driver.findElement(By.cssSelector("input[placeholder=\"•••\"]"));
            cvvInput.sendKeys("123");
            
            // Wait for animations
            waitForAnimations();
            
            // Click Pay Securely Now button
            WebElement payButton = driver.findElement(By.cssSelector("button[type=\"submit\"]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", payButton);
            
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
        }
    }

    @Test
    public void testTC004_BecomeProducer() {
        try {
            // Test ID: TC_004 - Become a Producer
            System.out.println("Running test: TC_004 - Become a Producer");
            
            // Step 1: Register a new account
            String timestamp = String.valueOf(System.currentTimeMillis());
            String testEmail = "test_seller_" + timestamp + "@example.com";
            String testUsername = "testseller" + timestamp;
            String testPassword = "Test123!";
            String testFirstName = "Test";
            String testLastName = "Seller";
            
            // Save credentials to static variables for TC_005
            sellerEmail = testEmail;
            sellerPassword = testPassword;
            
            // Navigate to registration page
            driver.get("http://localhost:5173/register");
            
            waitForPreloaderToDisappear();
            
            // Wait for the registration form to load
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//h1[contains(text(), 'WELCOME TO OUR MARKET')]")));
            
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
            
            // Step 2: Navigate to seller application page
            driver.get("http://localhost:5173/account/apply-seller");
            
            waitForPreloaderToDisappear();
            
            // Wait for the application form to load
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//h1[contains(text(), 'Become a Seller')]")));
            
            // Fill out the application form
            // Business name
            WebElement businessNameInput = driver.findElement(
                    By.xpath("//label[contains(text(), 'Business Name')]/following::input[1]"));
            businessNameInput.sendKeys("Test Business " + timestamp);
            
            // Business description
            WebElement businessDescInput = driver.findElement(
                    By.xpath("//label[contains(text(), 'Business Description')]/following::textarea[1]"));
            businessDescInput.sendKeys("This is a test business selling organic vegetables.");
            
            // Business phone
            WebElement businessPhoneInput = driver.findElement(
                    By.xpath("//label[contains(text(), 'Business Phone Number')]/following::input[1]"));
            businessPhoneInput.sendKeys("1234567890");
            
            // Business address
            WebElement businessAddressInput = driver.findElement(
                    By.xpath("//label[contains(text(), 'Business Address')]/following::input[1]"));
            businessAddressInput.sendKeys("123 Test Street, Test City");
            
            // Select City/Region from dropdown
            WebElement regionSelect = driver.findElement(
                    By.xpath("//label[contains(text(), 'City/Region')]/following::select[1]"));
            
            // Use the Select class for proper dropdown interaction
            org.openqa.selenium.support.ui.Select dropdown = new org.openqa.selenium.support.ui.Select(regionSelect);
            
            // Wait for dropdown options to load
            waitForAnimations();
            
            // Get all options
            List<WebElement> options = dropdown.getOptions();
            
            // Find a valid option (not empty and not "Other")
            String selectedOption = null;
            for (WebElement option : options) {
                String value = option.getAttribute("value");
                if (value != null && !value.isEmpty() && !"Other".equals(value)) {
                    selectedOption = value;
                    break;
                }
            }
            
            // Select the option by value
            if (selectedOption != null) {
                dropdown.selectByValue(selectedOption);
            } else {
                // If no valid option found, select by index (the first non-empty option)
                for (int i = 0; i < options.size(); i++) {
                    String value = options.get(i).getAttribute("value");
                    if (value != null && !value.isEmpty()) {
                        dropdown.selectByIndex(i);
                        break;
                    }
                }
            }
            
            waitForAnimations();
            
            // Select a product category button
            try {
                // Try to find and click the first category button
                WebElement categoryButton = driver.findElement(
                        By.xpath("//h2[contains(text(), 'Product Categories')]/following::button[1]"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", categoryButton);
            } catch (Exception e) {
                // If first approach fails, try alternate method to find category buttons
                WebElement categoryButton = driver.findElement(
                        By.xpath("//h2[contains(text(), 'Product Categories')]/following::div[contains(@class, 'flex-wrap')]/button[1]"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", categoryButton);
            }
            
            // Years of experience
            WebElement yearsExpInput = driver.findElement(
                    By.xpath("//label[contains(text(), 'Years of Experience')]/following::input[1]"));
            yearsExpInput.sendKeys("5");
            
            // Website (optional)
            WebElement websiteInput = driver.findElement(
                    By.xpath("//label[contains(text(), 'Website or Social Media Link')]/following::input[1]"));
            websiteInput.sendKeys("https://example.com");
            
            // Message to admin (optional)
            WebElement messageInput = driver.findElement(
                    By.xpath("//label[contains(text(), 'Message to Admin')]/following::textarea[1]"));
            messageInput.sendKeys("Please review my application as soon as possible. Thank you!");
            
            // Submit the application form
            WebElement submitButton = driver.findElement(By.xpath("//button[contains(text(), 'Submit Application')]"));

            
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton);
            
            waitForPreloaderToDisappear();
            
            // Wait for redirect after submission
            // Typically redirects to /account after submission
            wait.until(driver -> {
                String currentUrl = driver.getCurrentUrl();
                return currentUrl.contains("/account");
            });
            
            // Step 3: Navigate to become-seller page to check status
            driver.get("http://localhost:5173/become-seller");
            
            waitForPreloaderToDisappear();
            
            // Verify application status shows "Application Under Review"
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//h2[contains(text(), 'Application Under Review')]")));
            
            // Additional verification for Pending status
            WebElement pendingStatus = driver.findElement(
                    By.xpath("//h2[contains(text(), 'Application Under Review')]"));
            assertTrue(pendingStatus.isDisplayed(), "Application status should show 'Application Under Review'");
            
            System.out.println("TC_004 - Become a Producer: PASSED");
            
        } catch (Exception e) {
            // Take screenshot and log on failure
            if (driver instanceof JavascriptExecutor) {
                try {
                    ((JavascriptExecutor) driver).executeScript(
                            "console.error('TC_004 test failed at URL: ' + window.location.href)");
                } catch (Exception ignored) {}
            }
            
            System.err.println("TC_004 test failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testTC005_ReapplyAsProducer() {
        try {
            // Test ID: TC_005 - Reapply as Producer
            System.out.println("Running test: TC_005 - Reapply as Producer");
            
            boolean needsLogin = true;
            
            // Verify we have seller credentials from previous test
            if (sellerEmail == null || sellerPassword == null) {
                System.out.println("No seller credentials found. Run testBecomeProducer first.");
                // Create new credentials if needed
                String timestamp = String.valueOf(System.currentTimeMillis());
                sellerEmail = "test_seller_" + timestamp + "@example.com";
                sellerPassword = "Test123!";
                
                // Register a new user and apply as seller first
                testTC004_BecomeProducer();
                
                // No need to login again since testBecomeProducer already registered and logged in
                needsLogin = false;
            }
            
            // Step 1: Login with seller account (only if needed)
            if (needsLogin) {
                driver.get("http://localhost:5173/login");
                
                waitForPreloaderToDisappear();
                
                // Wait for login form to load
                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//h1[contains(text(), 'WELCOME TO OUR MARKET')]")));
                
                // Fill out login form
                WebElement loginEmailInput = driver.findElement(By.cssSelector("input[type=\"text\"]"));
                loginEmailInput.sendKeys(sellerEmail);
                
                WebElement loginPasswordInput = driver.findElement(By.cssSelector("input[type=\"password\"]"));
                loginPasswordInput.sendKeys(sellerPassword);
                
                // Wait for preloader to disappear
                waitForPreloaderToDisappear();
                
                // Submit login form using JavaScript click
                WebElement loginButton = driver.findElement(By.cssSelector("button[type=\"submit\"]"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loginButton);
                
                // Wait for login to complete and redirect to home page
                wait.until(ExpectedConditions.urlToBe("http://localhost:5173/"));
                
                // Verify login successful by checking for the user menu
                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector(".lucide-circle-user")));
            }
            
            // Step 2: Try to access seller application page again
            driver.get("http://localhost:5173/account/apply-seller");
            
            waitForPreloaderToDisappear();
            
            // Step 3: Verify we're redirected to account page or shown pending application status
            try {
                // Option 1: We might get redirected to account page
                WebElement accountHeading = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h1[contains(text(), 'Account')]")));
                System.out.println("Redirected to account page as expected when application is pending");
                
            } catch (Exception e) {
                // Option 2: We might stay on the page with a pending message
                try {
                    WebElement pendingMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//h2[contains(text(), 'Application Under Review')] | //h2[contains(text(), 'Application Pending')] | //div[contains(text(), 'pending')]")));
                    assertTrue(pendingMessage.isDisplayed(), "Application pending message should be shown");
                    System.out.println("Pending application message shown as expected");
                    
                } catch (Exception e2) {
                    // If both options fail, navigate to become-seller to check status
                    driver.get("http://localhost:5173/become-seller");
                    waitForPreloaderToDisappear();
                    
                    // Verify application status shows as pending/under review
                    WebElement pendingStatus = wait.until(ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//h2[contains(text(), 'Application Under Review')]")));
                    assertTrue(pendingStatus.isDisplayed(), "Application status should show 'Application Under Review'");
                }
            }
            
            System.out.println("TC_005 - Reapply as Producer: PASSED");
            
        } catch (Exception e) {
            // Take screenshot and log on failure
            if (driver instanceof JavascriptExecutor) {
                try {
                    ((JavascriptExecutor) driver).executeScript(
                            "console.error('TC_005 test failed at URL: ' + window.location.href)");
                } catch (Exception ignored) {}
            }
            
            System.err.println("TC_005 test failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testTC006_AddProductReview() throws InterruptedException {
        try {
            System.out.println("\n=== Starting TC_006: Add a review for a product (connected user) ===\n");
            
            // Generate unique username/email for this test
            String testFirstName = "Test";
            String testLastName = "Customer";
            String testUsername = "testcustomer" + System.currentTimeMillis();
            String testEmail = testUsername + "@example.com";
            reviewCustomerEmail = testEmail;
            reviewCustomerUsername = testUsername;
            String testPassword = customerPassword;

            // STEP 1: Register as a customer
            driver.get("http://localhost:5173/register");
            
            waitForPreloaderToDisappear();
            
            // Wait for registration form to load
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//h1[contains(text(), 'WELCOME TO OUR MARKET')]")));
            
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

            // STEP 2: Place an order for the first product
            reviewOrderNumber1 = placeOrder(0);
            
            // STEP 3: Update order status to Delivered using producer account
            String producerEmail = "producer5@test.com"; // Default producer email from requirements
            boolean orderUpdated = updateOrderStatusToDelivered(reviewOrderNumber1, reviewCustomerEmail, producerEmail);
            
            if (!orderUpdated) {
                System.out.println("Warning: Order status may not have been updated to Delivered");
            }
            
            // STEP 4: Submit a review for the product
            boolean reviewSubmitted = submitProductReview(0, 
                    "This is a great product! I highly recommend it. The quality is excellent.");
            
            if (reviewSubmitted) {
                System.out.println("\n=== TC_006 COMPLETED SUCCESSFULLY ===\n");
            } else {
                System.err.println("TC_006 FAILED - Could not submit review");
                throw new RuntimeException("Failed to submit product review");
            }
            
        } catch (Exception e) {
            System.err.println("TC_006 test failed with exception: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to fail the test
        }
    }

    @Test
    public void testTC007_ViewPendingReviews() throws InterruptedException {
        try {
            System.out.println("\n=== Starting TC_007: View pending reviews (admin) ===\n");
            
            // Skip if previous test didn't run
            if (reviewCustomerUsername == null) {
                System.out.println("Running TC_006 first to create a review");
                testTC006_AddProductReview();
            }
            
            // Initialize admin browser session if needed
            if (adminDriver == null) {
                adminDriver = createNewUserSession("admin@localmarket.com", "admin123");
            }
            
            // Navigate to reviews management page in admin session
            adminDriver.get("http://localhost:5173/admin/reviews");
            
            // Wait for reviews management page to load
            WebDriverWait adminWait = new WebDriverWait(adminDriver, Duration.ofSeconds(10));
            adminWait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//h2[contains(text(), 'Review Management')]")));
            
            // Make sure any preloader is gone
            try {
                adminWait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("div.fixed.inset-0.z-50.flex.flex-col.items-center.justify-center")));
                Thread.sleep(1000); // Small pause
            } catch (Exception e) {
                // Preloader might not be visible
            }
            
            breakpoint("Checking for review in admin panel", 5);
            
            // Verify the review from our test customer is present
            try {
                WebElement reviewRow = adminWait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//tr[contains(., '" + reviewCustomerUsername + "')]")));
                
                assertTrue(reviewRow != null, "Review from test customer should be present in admin review list");
                System.out.println("\n=== TC_007 COMPLETED SUCCESSFULLY ===\n");
                
            } catch (TimeoutException e) {
                System.out.println("Could not find the review in the table. Checking if there are any reviews...");
                // Check if "No Pending Reviews" message is shown - this shouldn't happen in a proper test flow
                boolean noReviews = adminDriver.findElements(By.xpath("//h3[contains(text(), 'No Pending Reviews')]")).size() > 0;
                if (noReviews) {
                    System.out.println("No pending reviews found.");
                }
                System.err.println("TC_007 FAILED - Could not find review in admin panel");
                throw e; // Rethrow to fail the test
            }
            
        } catch (Exception e) {
            System.err.println("TC_007 test failed with exception: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to fail the test
        }
    }

    @Test
    public void testTC008_ApproveReview() throws InterruptedException {
        try {
            System.out.println("\n=== Starting TC_008: Approve a pending review (admin) ===\n");
            
            // Skip if previous tests didn't run
            if (reviewCustomerUsername == null) {
                System.out.println("Running TC_006 first to create a review");
                testTC006_AddProductReview();
                System.out.println("Running TC_007 to verify review is visible in admin panel");
                testTC007_ViewPendingReviews();
            }
            
            // Approve the review
            boolean reviewApproved = moderateReview(reviewCustomerUsername, true);
            
            if (reviewApproved) {
                System.out.println("\n=== TC_008 COMPLETED SUCCESSFULLY ===\n");
            } else {
                System.err.println("TC_008 FAILED - Could not approve review");
                throw new RuntimeException("Failed to approve product review");
            }
            
        } catch (Exception e) {
            System.err.println("TC_008 test failed with exception: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to fail the test
        }
    }

    @Test
    public void testTC009_RejectReview() throws InterruptedException {
        try {
            System.out.println("\n=== Starting TC_009: Reject a review (admin) ===\n");
            
            // Skip if we don't have customer credentials from previous tests
            if (reviewCustomerEmail == null || reviewCustomerUsername == null) {
                System.out.println("Running TC_006 first to set up customer account");
                testTC006_AddProductReview();
            }
            
            // Place an order for a second product
            reviewOrderNumber2 = placeOrder(1);
            
            // Update second order status to Delivered
            String producerEmail = "producer5@test.com"; // Default producer email from requirements
            boolean orderUpdated = updateOrderStatusToDelivered(reviewOrderNumber2, reviewCustomerEmail, producerEmail);
            
            if (!orderUpdated) {
                System.out.println("Warning: Second order status may not have been updated to Delivered");
            }
            
            // Submit a review for the second product
            boolean reviewSubmitted = submitProductReview(1, 
                    "This is another review that should be rejected by admin.");
            
            if (!reviewSubmitted) {
                System.err.println("TC_009 WARNING - Could not submit second review");
                // Try with the first product again as fallback
                reviewSubmitted = submitProductReview(0, 
                        "This is a fallback review that should be rejected by admin.");
                
                if (!reviewSubmitted) {
                    System.err.println("TC_009 FAILED - Could not submit any review");
                    throw new RuntimeException("Failed to submit second product review");
                }
            }
            
            // Reject the review
            boolean reviewRejected = moderateReview(reviewCustomerUsername, false);
            
            if (reviewRejected) {
                System.out.println("\n=== TC_009 COMPLETED SUCCESSFULLY ===\n");
            } else {
                System.err.println("TC_009 FAILED - Could not reject review");
                throw new RuntimeException("Failed to reject product review");
            }
            
        } catch (Exception e) {
            System.err.println("TC_009 test failed with exception: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to fail the test
        }
    }

    /**
     * Helper method to place an order for a product
     * @param productIndex Index of the product to order (0 for first product, 1 for second, etc.)
     * @return The order number of the placed order
     */
    private String placeOrder(int productIndex) throws InterruptedException {
        // Navigate to the store page
        driver.get("http://localhost:5173/store");
        
        // Wait for products to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".grid-cols-1.md\\:grid-cols-2.lg\\:grid-cols-3 > a")));
        
        waitForPreloaderToDisappear();
        
        // Get all product cards
        List<WebElement> productCards = driver.findElements(
                By.cssSelector(".grid-cols-1.md\\:grid-cols-2.lg\\:grid-cols-3 > a"));
        
        // Make sure we have enough products
        if (productCards.size() <= productIndex) {
            throw new RuntimeException("Not enough products available. Requested index: " + 
                    productIndex + ", available products: " + productCards.size());
        }
        
        // Click on the specified product
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", productCards.get(productIndex));
        
        // Wait for product details page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h1[contains(@class, 'text-4xl')]")));
        
        // Get product name for later verification
        String productName = driver.findElement(By.xpath("//h1[contains(@class, 'text-4xl')]")).getText();
        System.out.println("Selected product: " + productName);
        
        // Add item to cart
        WebElement addToCartButton = driver.findElement(
                By.xpath("//button[contains(text(), 'Add to Cart')]"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addToCartButton);
        
        waitForAnimations();
        
        // Navigate to cart
        driver.get("http://localhost:5173/cart");
        
        // Complete checkout process
        return completeCheckout();
    }
    
    /**
     * Helper method to complete the checkout process
     * @return The order number of the placed order
     */
    private String completeCheckout() throws InterruptedException {
        // Wait for cart page to load
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
        
        // Navigate to orders to get the order number
        WebElement ordButton = driver.findElement(By.cssSelector("a.px-6.py-2\\.5.border.border-border.text-base.font-medium.text-text.rounded-full"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", ordButton);
        
        WebElement orderElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h2[contains(text(), 'Order #')]")));
        String orderText = orderElement.getText();
        String orderNumber = orderText.replaceAll("[^0-9]", ""); // Extract just the number
        System.out.println("Order number: " + orderNumber);
        
        return orderNumber;
    }
    
    /**
     * Helper method to complete the payment form
     */
    private void completePayment() {
        // Fill payment form
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("input[placeholder=\"John Doe\"]")));
        
        WebElement cardHolderInput = driver.findElement(By.cssSelector("input[placeholder=\"John Doe\"]"));
        cardHolderInput.sendKeys("Test Customer");
        
        WebElement cardNumberInput = driver.findElement(
                By.cssSelector("input[placeholder=\"1234 5678 9012 3456\"]"));
        cardNumberInput.sendKeys("1234567890123456");
        
        WebElement expiryInput = driver.findElement(By.cssSelector("input[placeholder=\"MM/YY\"]"));
        expiryInput.sendKeys("1225");
        
        WebElement cvvInput = driver.findElement(By.cssSelector("input[placeholder=\"•••\"]"));
        cvvInput.sendKeys("123");
        
        // Wait for animations
        waitForAnimations();
        
        // Click Pay Securely Now button
        WebElement payButton = driver.findElement(By.cssSelector("button[type=\"submit\"]"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", payButton);
    }
    
    /**
     * Helper method to update order status through the producer account
     * @param orderNumber The order number to update
     * @param customerEmail The customer email associated with the order
     * @param producerEmail The producer email to log in with
     * @return True if the order was successfully updated to Delivered
     */
    private boolean updateOrderStatusToDelivered(String orderNumber, String customerEmail, String producerEmail) throws InterruptedException {
        System.out.println("\n=== Starting producer session to update order status for order #" + orderNumber + " ===\n");
        
        // Initialize producer browser session if needed
        if (producerDriver == null) {
            producerDriver = createNewUserSession(producerEmail, "producer123");
        }
        
        // Navigate to producer orders page in producer session
        producerDriver.get("http://localhost:5173/producer/orders");
        
        // Wait for orders page to load in producer session
        WebDriverWait producerWait = new WebDriverWait(producerDriver, Duration.ofSeconds(10));
        producerWait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h2[contains(text(), 'Order Management')]")));
        
        // Make sure any preloader is gone
        try {
            producerWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("div.fixed.inset-0.z-50.flex.flex-col.items-center.justify-center")));
            Thread.sleep(1000); // Small pause
        } catch (Exception e) {
            // Preloader might not be visible
        }
        
        // Search for the order by customer email
        WebElement searchInput = producerDriver.findElement(By.cssSelector("input[placeholder=\"Search by customer email...\"]"));
        searchInput.clear();
        searchInput.sendKeys(customerEmail);
        
        // Wait for search results
        Thread.sleep(2000);
        
        breakpoint("Looking for order #" + orderNumber, 5);
        
        // Find the order
        List<WebElement> orderRows = producerDriver.findElements(
                By.xpath("//h3[contains(@class, 'text-lg') and contains(text(), '#" + orderNumber + "')]/ancestor::div[contains(@class, 'bg-cardBg')]"));
            
        if (orderRows.isEmpty()) {
            System.out.println("No orders found with ID #" + orderNumber);
            
            // Try to find any order with a Processing button
            System.out.println("Looking for any order with Processing status...");
            orderRows = producerDriver.findElements(
                    By.xpath("//button[contains(text(), 'Processing')]/ancestor::div[contains(@class, 'bg-cardBg')]"));
            
            if (!orderRows.isEmpty()) {
                System.out.println("Found " + orderRows.size() + " orders with Processing status");
            } else {
                // Try to find any order
                System.out.println("Looking for any order...");
                orderRows = producerDriver.findElements(
                        By.xpath("//div[contains(@class, 'bg-cardBg')]"));
                System.out.println("Found " + orderRows.size() + " total orders");
            }
        }
        
        if (orderRows.isEmpty()) {
            System.err.println("Could not find any orders");
            return false;
        }
        
        WebElement orderRow = orderRows.get(0);
        System.out.println("Found order row, attempting to update status");
        
        // Process the order through all statuses
        boolean processingClicked = clickButtonIfAvailable(orderRow, "Processing");
        Thread.sleep(2000);
        
        boolean shippedClicked = clickButtonIfAvailable(orderRow, "Shipped");
        Thread.sleep(2000);
        
        boolean deliveredClicked = clickButtonIfAvailable(orderRow, "Delivered");
        Thread.sleep(2000);
        
        // Check final status
        try {
            List<WebElement> finalOrderRows = producerDriver.findElements(
                    By.xpath("//div[contains(@class, 'bg-cardBg')]"));
            if (!finalOrderRows.isEmpty()) {
                WebElement statusElement = finalOrderRows.get(0).findElement(
                        By.xpath(".//span[contains(@class, 'text-sm font-medium')]"));
                String finalStatus = statusElement.getText();
                System.out.println("Final order status: " + finalStatus);
                
                if ("Delivered".equals(finalStatus)) {
                    System.out.println("Successfully updated order to Delivered status");
                    return true;
                } else {
                    System.out.println("Warning: Final status is not Delivered. Current status: " + finalStatus);
                    return false;
                }
            }
        } catch (Exception e) {
            System.out.println("Could not verify final status: " + e.getMessage());
        }
        
        return processingClicked && shippedClicked && deliveredClicked;
    }
    
    /**
     * Helper method to click a button in an order row if it's available
     * @param orderRow The order row element
     * @param buttonText The text of the button to click
     * @return True if the button was found and clicked
     */
    private boolean clickButtonIfAvailable(WebElement orderRow, String buttonText) {
        try {
            // First try to find it within the order row
            WebElement button = null;
            
            try {
                button = orderRow.findElement(By.xpath(".//button[contains(text(), '" + buttonText + "')]"));
            } catch (NoSuchElementException e) {
                // If not found in the row, try to find it anywhere on the page
                try {
                    button = producerDriver.findElement(By.xpath("//button[contains(text(), '" + buttonText + "')]"));
                } catch (NoSuchElementException e2) {
                    System.out.println(buttonText + " button not found, may already be in " + buttonText + " state");
                    return false;
                }
            }
            
            if (button != null) {
                // Scroll to button and click
                ((JavascriptExecutor) producerDriver).executeScript("arguments[0].scrollIntoView({block: 'center'});", button);
                Thread.sleep(500);
                ((JavascriptExecutor) producerDriver).executeScript("arguments[0].click();", button);
                System.out.println("Clicked " + buttonText + " button");
                return true;
            }
        } catch (Exception e) {
            System.out.println("Error clicking " + buttonText + " button: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Helper method to submit a review for a product
     * @param productIndex Index of the product to review
     * @param reviewText The review text to submit
     * @return True if the review was successfully submitted
     */
    private boolean submitProductReview(int productIndex, String reviewText) throws InterruptedException {
        // Navigate to the store page
        driver.get("http://localhost:5173/store");
        waitForPreloaderToDisappear();
        
        // Find and click the product
        List<WebElement> productCards = driver.findElements(
                By.cssSelector(".grid-cols-1.md\\:grid-cols-2.lg\\:grid-cols-3 > a"));
        
        if (productCards.size() <= productIndex) {
            throw new RuntimeException("Not enough products available. Requested index: " + 
                    productIndex + ", available products: " + productCards.size());
        }
        
        // Click on the specified product
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", productCards.get(productIndex));
        
        // Wait for product details page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h1[contains(@class, 'text-4xl')]")));
        
        // Take a breakpoint to see the page state
        breakpoint("Checking for review section", 5);
        
        try {
            // Try multiple selectors to find the review section
            WebElement reviewHeading = null;
            
            // First try the exact text
            try {
                reviewHeading = driver.findElement(By.xpath("//h3[contains(text(), 'Share Your Experience')]"));
            } catch (NoSuchElementException e) {
                // Try alternative selectors
                try {
                    reviewHeading = driver.findElement(By.xpath("//h3[contains(text(), 'Review')]"));
                } catch (NoSuchElementException e2) {
                    // Try to find any Write Review button
                    try {
                        WebElement writeReviewButton = driver.findElement(
                                By.xpath("//button[contains(text(), 'Write Review')]"));
                        // If found, we don't need the heading
                        reviewHeading = writeReviewButton;
                        System.out.println("Found review button directly");
                    } catch (NoSuchElementException e3) {
                        // Not found with any method
                        throw new NoSuchElementException("Could not find review section with any selector");
                    }
                }
            }
            
            // Check if we found the button directly or need to click it
            WebElement writeReviewButton;
            if (reviewHeading.getTagName().equals("button")) {
                writeReviewButton = reviewHeading;
            } else {
                // Find the Write Review button
                writeReviewButton = driver.findElement(
                        By.xpath("//button[contains(text(), 'Write Review') or contains(text(), 'Add Review')]"));
            }
            
            // Click on Write Review button
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", writeReviewButton);
            
            // Wait for review form to appear
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//label[contains(text(), 'Your Review') or contains(text(), 'Comment')]")));
            
            // Find the textarea - try multiple selectors
            WebElement commentTextarea = null;
            try {
                commentTextarea = driver.findElement(
                        By.xpath("//textarea[contains(@placeholder, 'Share your thoughts') or contains(@placeholder, 'review')]"));
            } catch (NoSuchElementException e) {
                // Try a more generic approach
                commentTextarea = driver.findElement(By.tagName("textarea"));
            }
            
            commentTextarea.sendKeys(reviewText);
            
            // Submit the review - try multiple button selectors
            WebElement submitButton = null;
            try {
                submitButton = driver.findElement(
                        By.xpath("//button[contains(text(), 'Submit') and @type='submit']"));
            } catch (NoSuchElementException e) {
                // Try more generic approach
                submitButton = driver.findElement(
                        By.xpath("//form//button[@type='submit']"));
            }
            
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton);
            
            // Wait for submission to complete
            waitForAnimations();
            System.out.println("Successfully submitted review");
            return true;
            
        } catch (Exception e) {
            System.err.println("Could not submit review: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Helper method to moderate a review in the admin panel
     * @param username The username of the reviewer to find
     * @param approve True to approve the review, false to reject it
     * @return True if the moderation action was successful
     */
    private boolean moderateReview(String username, boolean approve) throws InterruptedException {
        System.out.println("\n=== Starting admin session to " + (approve ? "approve" : "reject") + " review ===\n");
        
        // Initialize admin browser session if needed
        if (adminDriver == null) {
            adminDriver = createNewUserSession("admin@localmarket.com", "admin123");
        }
        
        // Navigate to reviews management page in admin session
        adminDriver.get("http://localhost:5173/admin/reviews");
        
        // Wait for reviews management page to load
        WebDriverWait adminWait = new WebDriverWait(adminDriver, Duration.ofSeconds(10));
        adminWait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h2[contains(text(), 'Review Management')]")));
        
        // Make sure any preloader is gone
        try {
            adminWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("div.fixed.inset-0.z-50.flex.flex-col.items-center.justify-center")));
            Thread.sleep(1000); // Small pause
        } catch (Exception e) {
            // Preloader might not be visible
        }
        
        breakpoint("Looking for review by " + username, 5);
        
        // Find the review row by username
        try {
            // Find the row containing the username
            WebElement reviewRow = adminWait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//tbody/tr[contains(., '" + username + "')]")));
            
            // Get the product name from the first cell in the row to use for verification later
            WebElement productCell = reviewRow.findElement(By.xpath(".//td[1]"));
            String productName = productCell.getText().trim();
            System.out.println("Found review row for username: " + username + ", product: " + productName);
            
            // Find the actions cell (last td in the row)
            WebElement actionsCell = reviewRow.findElement(By.xpath(".//td[last()]"));
            
            // Take a screenshot or breakpoint to see the state before clicking
            breakpoint("About to click " + (approve ? "approve" : "reject") + " button", 2);
            
            // Both buttons are in a div with class "flex items-center gap-2"
            WebElement buttonsContainer = actionsCell.findElement(By.cssSelector("div.flex.items-center.gap-2"));
            
            // Get all buttons in the container
            List<WebElement> buttons = buttonsContainer.findElements(By.tagName("button"));
            
            if (buttons.size() < 2) {
                System.err.println("Expected 2 buttons, found " + buttons.size());
                return false;
            }
            
            System.out.println("Found " + buttons.size() + " action buttons");
            
            // The first button is approve (green), the second is reject (red)
            WebElement targetButton = approve ? buttons.get(0) : buttons.get(1);
            
            // Scroll to make sure the button is visible
            ((JavascriptExecutor) adminDriver).executeScript(
                    "arguments[0].scrollIntoView({block: 'center'});", targetButton);
            Thread.sleep(1000);
            
            // Try different clicking methods
            try {
                // Method 1: Direct click
                targetButton.click();
                System.out.println("Direct click successful");
            } catch (Exception e) {
                System.out.println("Direct click failed: " + e.getMessage());
                
                try {
                    // Method 2: JavaScript click
                    ((JavascriptExecutor) adminDriver).executeScript("arguments[0].click();", targetButton);
                    System.out.println("JavaScript click successful");
                } catch (Exception e2) {
                    System.out.println("JavaScript click failed: " + e2.getMessage());
                    
                    // Method 3: Action chains
                    try {
                        org.openqa.selenium.interactions.Actions actions = 
                                new org.openqa.selenium.interactions.Actions(adminDriver);
                        actions.moveToElement(targetButton).click().perform();
                        System.out.println("Actions click successful");
                    } catch (Exception e3) {
                        System.out.println("Actions click failed: " + e3.getMessage());
                        return false;
                    }
                }
            }
            
            // Wait longer after clicking
            Thread.sleep(3000);
            
            // Refresh the page to verify the result
            adminDriver.navigate().refresh();
            
            // Wait for the page to load
            adminWait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//h2[contains(text(), 'Review Management')]")));
            Thread.sleep(1000);
            
            // Check if the specific review (username + product) is still in the table
            String xpathForSpecificReview = "//tbody/tr[contains(., '" + username + "') and contains(., '" + productName + "')]";
            System.out.println("Checking if review exists with xpath: " + xpathForSpecificReview);
            
            List<WebElement> remainingReviews = adminDriver.findElements(By.xpath(xpathForSpecificReview));
            
            boolean reviewGone = remainingReviews.isEmpty();
            System.out.println("After " + (approve ? "approval" : "rejection") + 
                    ", review for " + productName + " by " + username + " " + 
                    (reviewGone ? "is gone (SUCCESS)" : "is still present (FAILURE)"));
            
            // If testing reject and still seeing the review, take another screenshot to debug
            if (!approve && !reviewGone) {
                breakpoint("FAILURE: Review still present after rejection attempt", 5);
                
                // Try one more time with a different approach as a last resort
                System.out.println("Trying a final attempt to reject the review");
                
                // Find the row again
                List<WebElement> rowsToTry = adminDriver.findElements(
                        By.xpath("//tbody/tr[contains(., '" + username + "') and contains(., '" + productName + "')]"));
                
                if (!rowsToTry.isEmpty()) {
                    // Try one more time with the second button
                    WebElement row = rowsToTry.get(0);
                    List<WebElement> allButtons = row.findElements(By.tagName("button"));
                    
                    // Find and click the last button (should be reject)
                    if (allButtons.size() >= 2) {
                        WebElement lastResortButton = allButtons.get(allButtons.size() - 1);
                        
                        // Click with JavaScript without any animations
                        ((JavascriptExecutor) adminDriver).executeScript(
                                "arguments[0].click(); console.log('Last resort click executed');", lastResortButton);
                        
                        Thread.sleep(3000);
                        adminDriver.navigate().refresh();
                        
                        // Check one more time
                        List<WebElement> finalCheck = adminDriver.findElements(By.xpath(xpathForSpecificReview));
                        boolean finalResult = finalCheck.isEmpty();
                        System.out.println("Final attempt result: " + (finalResult ? "SUCCESS" : "FAILURE"));
                        
                        return finalResult;
                    }
                }
            }
            
            return reviewGone;
            
        } catch (Exception e) {
            System.err.println("Error in moderateReview: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create a new browser session for a specific user role
     * @throws InterruptedException if thread sleep is interrupted
     */
    private WebDriver createNewUserSession(String email, String password) throws InterruptedException {
        System.out.println("Creating new browser session for: " + email);
        
        // Set up a new driver instance
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        WebDriver newDriver = new ChromeDriver(options);
        newDriver.manage().window().maximize();
        
        // Create waits for this driver
        WebDriverWait newWait = new WebDriverWait(newDriver, Duration.ofSeconds(10));
        
        try {
            // Navigate to login page
            newDriver.get("http://localhost:5173/login");
            
            // Wait for login form to load
            newWait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//h1[contains(text(), 'WELCOME TO OUR MARKET')]")));
            
            // Fill out login form
            WebElement emailInput = newDriver.findElement(By.cssSelector("input[type=\"text\"]"));
            emailInput.sendKeys(email);
            
            WebElement passwordInput = newDriver.findElement(By.cssSelector("input[type=\"password\"]"));
            passwordInput.sendKeys(password);
            
            // Wait for any preloader
            try {
                newWait.until(ExpectedConditions.invisibilityOfElementLocated(
                        By.cssSelector("div.fixed.inset-0.z-50.flex.flex-col.items-center.justify-center")));
                Thread.sleep(1000); // Small pause
            } catch (Exception e) {
                // Preloader might not be visible
            }
            
            // Submit login form using JavaScript click
            WebElement loginButton = newDriver.findElement(By.cssSelector("button[type=\"submit\"]"));
            ((JavascriptExecutor) newDriver).executeScript("arguments[0].click();", loginButton);
            
            // Wait for login to complete and redirect to home page or role-specific page
            newWait.until(ExpectedConditions.or(
                ExpectedConditions.urlToBe("http://localhost:5173/"),
                ExpectedConditions.urlToBe("http://localhost:5173/admin"),
                ExpectedConditions.urlToBe("http://localhost:5173/producer"),
                ExpectedConditions.urlToBe("http://localhost:5173/producer/products"),
                ExpectedConditions.urlContains("/dashboard")
            ));
            
            // Log where we landed
            System.out.println("Login successful - redirected to: " + newDriver.getCurrentUrl());
            
            // Wait a moment for the page to fully load
            Thread.sleep(1000);
            
            // Verify login successful by checking for the user menu
            newWait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".lucide-circle-user")));
            
            System.out.println("Login successful for: " + email);
            return newDriver;
        } catch (Exception e) {
            System.err.println("Failed to create session for: " + email);
            e.printStackTrace();
            if (newDriver != null) {
                newDriver.quit();
            }
            throw e;
        }
    }

    /**
     * Helper method to pause test execution for debugging
     * @param message Message to display at breakpoint
     * @param seconds Number of seconds to pause (default 10)
     */
    private void breakpoint(String message, int seconds) {
        System.out.println("\n=== BREAKPOINT: " + message + " ===");
        System.out.println("Test paused for " + seconds + " seconds to allow inspection...");
        
        try {
            // Take screenshot if driver is available
            if (driver instanceof JavascriptExecutor) {
                ((JavascriptExecutor) driver).executeScript(
                    "console.log('BREAKPOINT: " + message.replace("'", "\\'") + "')");
            }
            
            // Show current URL for context
            System.out.println("Customer browser URL: " + (driver != null ? driver.getCurrentUrl() : "N/A"));
            if (producerDriver != null) {
                System.out.println("Producer browser URL: " + producerDriver.getCurrentUrl());
            }
            if (adminDriver != null) {
                System.out.println("Admin browser URL: " + adminDriver.getCurrentUrl());
            }
            
            // Sleep to keep browser open for inspection
            Thread.sleep(seconds * 1000);
            System.out.println("=== Resuming test ===\n");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Breakpoint interrupted");
        }
    }
    
    /**
     * Overloaded breakpoint method with default 10 second pause
     * @param message Message to display at breakpoint
     */
    private void breakpoint(String message) {
        breakpoint(message, 10);
    }
} 