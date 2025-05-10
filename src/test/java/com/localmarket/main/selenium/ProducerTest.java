package com.localmarket.main.selenium;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProducerTest extends BaseTest {
    // Static variables to store seller credentials across test methods
    private static String sellerEmail = null;
    private static String sellerPassword = null;

    @Test
    @DisplayName("Become a Producer")
    @Order(4)
    public void testTC004_BecomeProducer() {
        try {
            // Test ID: TC_004 - Become a Producer
            System.out.println("Running test: TC_004 - Become a Producer");
            
            // Make sure driver is initialized
            if (driver == null) {
                System.out.println("Driver is null, initializing...");
                setUp();
            }
            
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
    @DisplayName("Reapply as Producer")
    @Order(5)
    public void testTC005_ReapplyAsProducer() {
        try {
            // Test ID: TC_005 - Reapply as Producer
            System.out.println("Running test: TC_005 - Reapply as Producer");
            
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
            
            boolean needsLogin = true;
            
            // Verify we have seller credentials from previous test
            if (sellerEmail == null || sellerPassword == null) {
                System.out.println("No seller credentials found. Run testBecomeProducer first.");
                // Create new credentials if needed
                String timestamp = String.valueOf(System.currentTimeMillis());
                sellerEmail = "test_seller_" + timestamp + "@example.com";
                sellerPassword = "Test123!";
                
                // Close this driver
                driver.quit();
                
                // Register a new user and apply as seller first
                testTC004_BecomeProducer();
                
                // No need to login again since testBecomeProducer already registered and logged in
                needsLogin = false;
                return;
            }
            
            // Step 1: Login with seller account (only if needed)
            if (needsLogin) {
                driver.get("http://localhost:5173/login");
                
                waitForPreloaderToDisappear();
                breakpoint("After waitForPreloaderToDisappear", 5);
                
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
        } finally {
            // Close the WebDriver when this test is done
            if (driver != null) {
                System.out.println("Closing WebDriver for TC_005");
                driver.quit();
                driver = null;
            }
        }
    }
} 