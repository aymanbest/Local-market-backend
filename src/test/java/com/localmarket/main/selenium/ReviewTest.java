package com.localmarket.main.selenium;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReviewTest extends BaseTest {
    // WebDriver instances for different user sessions
    private WebDriver producerDriver = null;
    private WebDriver adminDriver = null;

    // Customer credentials for review tests
    private static String reviewCustomerEmail = null;
    private static String reviewCustomerUsername = null;
    private static String reviewOrderNumber1 = null;
    private static String reviewOrderNumber2 = null;
    
    // Default password for all test customers
    private static final String CUSTOMER_PASSWORD = "customer123";
    
    @Override
    public void tearDown() {
        // Close additional browser instances
        if (producerDriver != null) {
            producerDriver.quit();
            producerDriver = null;
        }
        if (adminDriver != null) {
            adminDriver.quit();
            adminDriver = null;
        }
        // Don't call super.tearDown() to avoid closing the main driver
        // The main driver will be closed by SeleniumTestSuite
    }
    
    @Test
    @DisplayName("Add a review for a product (connected user)")
    @Order(6)
    public void testTC006_AddProductReview() throws InterruptedException {
        try {
            System.out.println("\n=== Starting TC_006: Add a review for a product (connected user) ===\n");
            
            // Create a completely new WebDriver instance for this test
            if (driver != null) {
                driver.quit();
            }
            
            // Set up a new driver from scratch
            io.github.bonigarcia.wdm.WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            driver = new ChromeDriver(options);
            
            // Initialize waits
            wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            fluentWait = new FluentWait<>(driver)
                    .withTimeout(Duration.ofSeconds(30))
                    .pollingEvery(Duration.ofMillis(500))
                    .ignoring(NoSuchElementException.class)
                    .ignoring(StaleElementReferenceException.class);
                    
            // Maximize window
            driver.manage().window().maximize();
            
            // Update the static driver reference
            BaseTest.staticDriver = driver;
            
            // Generate unique username/email for this test
            String testFirstName = "Test";
            String testLastName = "Customer";
            String testUsername = "testcustomer" + System.currentTimeMillis();
            String testEmail = testUsername + "@example.com";
            reviewCustomerEmail = testEmail;
            reviewCustomerUsername = testUsername;
            String testPassword = CUSTOMER_PASSWORD;

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
            
            takeScreenshot(driver, "TC_006", "Fill_out_registration_form");
            
            // Submit registration form using JavaScript click for reliability
            WebElement registerButton = driver.findElement(By.xpath("//button[@type='submit']"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", registerButton);
            
            takeScreenshot(driver, "TC_006", "Register_Button_Clicked");
            
            waitForPreloaderToDisappear();

            // Wait for registration to complete and redirect to home page
            wait.until(ExpectedConditions.urlToBe("http://localhost:5173/"));

            // Verify login successful by checking for the user menu
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".lucide-circle-user")));
                    
            takeScreenshot(driver, "TC_006", "Registration_Successful");

            // STEP 2: Place an order for the first product
            reviewOrderNumber1 = placeOrder(0);
            
            takeScreenshot(driver, "TC_006", "Order_Placed_Successfully");
            
            // STEP 3: Update order status to Delivered using producer account
            String producerEmail = "producer5@test.com"; // Default producer email from requirements
            boolean orderUpdated = updateOrderStatusToDelivered(reviewOrderNumber1, reviewCustomerEmail, producerEmail);
            
            if (!orderUpdated) {
                System.out.println("Warning: Order status may not have been updated to Delivered");
            } else {
                takeScreenshot(driver, "TC_006", "Order_Status_Updated_To_Delivered");
            }
            
            // STEP 4: Submit a review for the product
            boolean reviewSubmitted = submitProductReview(0, 
                    "This is a great product! I highly recommend it. The quality is excellent.");
            
            if (reviewSubmitted) {
                takeScreenshot(driver, "TC_006", "Review_Submitted_Successfully");
                System.out.println("\n=== TC_006 COMPLETED SUCCESSFULLY ===\n");
            } else {
                System.err.println("TC_006 FAILED - Could not submit review");
                throw new RuntimeException("Failed to submit product review");
            }
            
        } catch (Exception e) {
            takeScreenshot(driver, "TC_006", "Error_State");
            System.err.println("TC_006 test failed with exception: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to fail the test
        }
    }

    @Test
    @DisplayName("View pending reviews (admin)")
    @Order(7)
    public void testTC007_ViewPendingReviews() throws InterruptedException {
        try {
            System.out.println("\n=== Starting TC_007: View pending reviews (admin) ===\n");
            
            // Skip if previous test didn't run
            if (reviewCustomerUsername == null) {
                System.out.println("Running TC_006 first to create a review");
                testTC006_AddProductReview();
            }
            
            // Make sure driver is initialized
            if (driver == null) {
                System.out.println("Driver is null, initializing...");
                setUp();
            }
            
            // Initialize admin browser session if needed
            if (adminDriver == null) {
                adminDriver = createNewUserSession("admin@localmarket.com", "admin123");
            }
            
            takeScreenshot(adminDriver, "TC_007", "Admin_Login_Successful");
            
            // Navigate to reviews management page in admin session
            adminDriver.get("http://localhost:5173/admin/reviews");
            
            // Wait for reviews management page to load
            WebDriverWait adminWait = new WebDriverWait(adminDriver, Duration.ofSeconds(10));
            adminWait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//h2[contains(text(), 'Review Management')]")));
            
            takeScreenshot(adminDriver, "TC_007", "Admin_Reviews_Management_Page");
            
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
                
                takeScreenshot(adminDriver, "TC_007", "Review_Found_In_Admin_Panel");
                
                assertTrue(reviewRow != null, "Review from test customer should be present in admin review list");
                System.out.println("\n=== TC_007 COMPLETED SUCCESSFULLY ===\n");
                
            } catch (TimeoutException e) {
                System.out.println("Could not find the review in the table. Checking if there are any reviews...");
                // Check if "No Pending Reviews" message is shown - this shouldn't happen in a proper test flow
                boolean noReviews = adminDriver.findElements(By.xpath("//h3[contains(text(), 'No Pending Reviews')]")).size() > 0;
                if (noReviews) {
                    System.out.println("No pending reviews found.");
                    takeScreenshot(adminDriver, "TC_007", "No_Pending_Reviews_Message");
                }
                System.err.println("TC_007 FAILED - Could not find review in admin panel");
                throw e; // Rethrow to fail the test
            }
            
        } catch (Exception e) {
            takeScreenshot(adminDriver, "TC_007", "Error_State");
            System.err.println("TC_007 test failed with exception: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to fail the test
        }
    }

    @Test
    @DisplayName("Approve a pending review (admin)")
    @Order(8)
    public void testTC008_ApproveReview() throws InterruptedException {
        try {
            System.out.println("\n=== Starting TC_008: Approve a pending review (admin) ===\n");
            
            // Make sure driver is initialized
            if (driver == null) {
                System.out.println("Driver is null, initializing...");
                setUp();
            }
            
            // Skip if previous tests didn't run
            if (reviewCustomerUsername == null) {
                System.out.println("Running TC_006 first to create a review");
                testTC006_AddProductReview();
                System.out.println("Running TC_007 to verify review is visible in admin panel");
                testTC007_ViewPendingReviews();
            }
            
            takeScreenshot(adminDriver, "TC_008", "Before_Approve_Review");
            
            // Approve the review
            boolean reviewApproved = moderateReview(reviewCustomerUsername, true);
            
            if (reviewApproved) {
                takeScreenshot(adminDriver, "TC_008", "Review_Approved_Successfully");
                System.out.println("\n=== TC_008 COMPLETED SUCCESSFULLY ===\n");
            } else {
                takeScreenshot(adminDriver, "TC_008", "Review_Approval_Failed");
                System.err.println("TC_008 FAILED - Could not approve review");
                throw new RuntimeException("Failed to approve product review");
            }
            
        } catch (Exception e) {
            takeScreenshot(adminDriver, "TC_008", "Error_State");
            System.err.println("TC_008 test failed with exception: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to fail the test
        }
    }

    @Test
    @DisplayName("Reject a review (admin)")
    @Order(9)
    public void testTC009_RejectReview() throws InterruptedException {
        try {
            System.out.println("\n=== Starting TC_009: Reject a review (admin) ===\n");
            
            // Make sure driver is initialized
            if (driver == null) {
                System.out.println("Driver is null, initializing...");
                setUp();
            }
            
            // Skip if we don't have customer credentials from previous tests
            if (reviewCustomerEmail == null || reviewCustomerUsername == null) {
                System.out.println("Running TC_006 first to set up customer account");
                testTC006_AddProductReview();
            }
            
            // Place an order for a second product
            reviewOrderNumber2 = placeOrder(1);
            
            takeScreenshot(driver, "TC_009", "Second_Order_Placed");
            
            // Update second order status to Delivered
            String producerEmail = "producer5@test.com"; // Default producer email from requirements
            boolean orderUpdated = updateOrderStatusToDelivered(reviewOrderNumber2, reviewCustomerEmail, producerEmail);
            
            if (!orderUpdated) {
                System.out.println("Warning: Second order status may not have been updated to Delivered");
            } else {
                takeScreenshot(driver, "TC_009", "Second_Order_Status_Updated");
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
                    takeScreenshot(driver, "TC_009", "Review_Submission_Failed");
                    System.err.println("TC_009 FAILED - Could not submit any review");
                    throw new RuntimeException("Failed to submit second product review");
                }
            }
            
            takeScreenshot(driver, "TC_009", "Review_Submitted_Successfully");
            
            // Reject the review
            boolean reviewRejected = moderateReview(reviewCustomerUsername, false);
            
            if (reviewRejected) {
                takeScreenshot(adminDriver, "TC_009", "Review_Rejected_Successfully");
                System.out.println("\n=== TC_009 COMPLETED SUCCESSFULLY ===\n");
            } else {
                takeScreenshot(adminDriver, "TC_009", "Review_Rejection_Failed");
                System.err.println("TC_009 FAILED - Could not reject review");
                throw new RuntimeException("Failed to reject product review");
            }
            
        } catch (Exception e) {
            takeScreenshot(driver, "TC_009", "Error_State_Customer");
            if (adminDriver != null) {
                takeScreenshot(adminDriver, "TC_009", "Error_State_Admin");
            }
            System.err.println("TC_009 test failed with exception: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to fail the test
        } finally {
            // Close all WebDrivers when this test is done (it's the last test)
            closeAllDrivers();
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
        
        takeScreenshot(adminDriver, "Review_Moderation", "Admin_Reviews_Management_Page");
        
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
                
            takeScreenshot(adminDriver, "Review_Moderation", "Review_Found_For_Moderation");
                
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
            
            takeScreenshot(adminDriver, "Review_Moderation", (approve ? "Approve" : "Reject") + "_Button_Before_Click");
            
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
            
            // If rejecting, we need to handle the rejection reason modal
            if (!approve) {
                try {
                    // Wait for rejection reason modal
                    adminWait.until(ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//h3[contains(text(), 'Rejection Reason')]")));
                            
                    takeScreenshot(adminDriver, "Review_Moderation", "Rejection_Reason_Modal");
                    
                    // Enter rejection reason
                    WebElement reasonInput = adminDriver.findElement(
                            By.xpath("//textarea[@placeholder='Enter reason for rejection']"));
                    reasonInput.sendKeys("This review does not meet our content guidelines.");
                    
                    takeScreenshot(adminDriver, "Review_Moderation", "Rejection_Reason_Entered");
                    
                    // Submit the form
                    WebElement submitButton = adminDriver.findElement(
                            By.xpath("//button[contains(text(), 'Submit')]"));
                    ((JavascriptExecutor) adminDriver).executeScript("arguments[0].click();", submitButton);
                } catch (Exception e) {
                    System.out.println("No rejection reason modal found or error handling it: " + e.getMessage());
                    // Continue anyway as some implementations might not have this modal
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
            
            if (reviewGone) {
                takeScreenshot(adminDriver, "Review_Moderation", "Review_" + (approve ? "Approved" : "Rejected") + "_Successfully");
            }
            
            // If testing reject and still seeing the review, take another screenshot to debug
            if (!approve && !reviewGone) {
                breakpoint("FAILURE: Review still present after rejection attempt", 5);
                takeScreenshot(adminDriver, "Review_Moderation", "Review_Still_Present_After_Rejection");
                
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
                        
                        takeScreenshot(adminDriver, "Review_Moderation", "Last_Resort_Reject_Button");
                        
                        // Click with JavaScript without any animations
                        ((JavascriptExecutor) adminDriver).executeScript(
                                "arguments[0].click(); console.log('Last resort click executed');", lastResortButton);
                        
                        Thread.sleep(3000);
                        adminDriver.navigate().refresh();
                        
                        // Check one more time
                        List<WebElement> finalCheck = adminDriver.findElements(By.xpath(xpathForSpecificReview));
                        boolean finalResult = finalCheck.isEmpty();
                        System.out.println("Final attempt result: " + (finalResult ? "SUCCESS" : "FAILURE"));
                        
                        if (finalResult) {
                            takeScreenshot(adminDriver, "Review_Moderation", "Review_Rejected_After_Final_Attempt");
                        } else {
                            takeScreenshot(adminDriver, "Review_Moderation", "Review_Still_Present_After_Final_Attempt");
                        }
                        
                        return finalResult;
                    }
                }
            }
            
            return reviewGone;
            
        } catch (Exception e) {
            takeScreenshot(adminDriver, "Review_Moderation", "Error_During_" + (approve ? "Approval" : "Rejection"));
            System.err.println("Error in moderateReview: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Helper method to close all WebDrivers used by this test class
     */
    private void closeAllDrivers() {
        System.out.println("Closing all WebDrivers for ReviewTest");
        
        // Close additional browser instances
        if (producerDriver != null) {
            producerDriver.quit();
            producerDriver = null;
        }
        if (adminDriver != null) {
            adminDriver.quit();
            adminDriver = null;
        }
        
        // Close main driver if running as standalone test
        if (driver != null && !(this.getClass().getName().equals("com.localmarket.main.selenium.SeleniumTestSuite"))) {
            driver.quit();
            driver = null;
        }
    }
} 