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
import org.openqa.selenium.support.ui.Select;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProducerTest extends BaseTest {
    // Static variables to store seller credentials across test methods
    private static String sellerEmail = null;
    private static String sellerPassword = null;
    private static boolean tc004Completed = false;

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

            takeScreenshot(driver, "TC_004", "Fill_out_registration_form");
            
            // Submit registration form using JavaScript click for reliability
            WebElement registerButton = driver.findElement(By.xpath("//button[@type='submit']"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", registerButton);

            takeScreenshot(driver, "TC_004", "Register_Button_Clicked");
            
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

            takeScreenshot(driver, "TC_004", "Access_application_form_for_producer");
            
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
            Select dropdown = new Select(regionSelect);
            
            // Wait for dropdown options to load
            waitForAnimations();
            
            // Get all options
            List<WebElement> options = dropdown.getOptions();
            
            // Find a valid option
            // select the first option in the dropdown
            if (options.size() > 1) {
                dropdown.selectByIndex(1);
            } else if (options.size() > 0) {
                // Fallback
                dropdown.selectByIndex(0);
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

            takeScreenshot(driver, "TC_004", "Fill_out_application_form");
            
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton);
            
            waitForPreloaderToDisappear();

            takeScreenshot(driver, "TC_004", "Submit_application_form");
            
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
            
            takeScreenshot(driver, "TC_004", "Application_Under_Review_Message_Displayed");
            
            // Additional verification for Pending status
            WebElement pendingStatus = driver.findElement(
                    By.xpath("//h2[contains(text(), 'Application Under Review')]"));
            assertTrue(pendingStatus.isDisplayed(), "Application status should show 'Application Under Review'");
            
            System.out.println("TC_004 - Become a Producer: PASSED");
            
            // Mark test as completed and update static driver
            tc004Completed = true;
            BaseTest.staticDriver = driver;
            
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
        // IMPORTANT: Do not close the driver here as TC005 may need it
    }

    @Test
    @DisplayName("Reapply as Producer")
    @Order(5)
    public void testTC005_ReapplyAsProducer() {
        try {
            // Test ID: TC_005 - Reapply as Producer
            System.out.println("Running test: TC_005 - Reapply as Producer");
            
            // If test is running standalone (not through test suite)
            if (!tc004Completed && (sellerEmail == null || sellerPassword == null)) {
                System.out.println("TC_004 not completed, running it first...");
                testTC004_BecomeProducer();
                System.out.println("TC_004 completed, continuing with TC_005...");
            }
            
            // Ensure we have a valid driver
            if (driver == null) {
                System.out.println("Driver is null, initializing...");
                setUp();
            }
            
            System.out.println("Checking seller application status with credentials: " + sellerEmail);
            
            // Navigate to seller application page again
            driver.get("http://localhost:5173/account/apply-seller");
            
            waitForPreloaderToDisappear();
            
            // takeScreenshot(driver, "TC_005", "Access_Seller_Application_Page");

            breakpoint("apply seller", 5);
            
            // Verify we're redirected to account page or shown pending application status
            try {
                // Option 1: We might get redirected to account page
                WebElement accountHeading = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h1[contains(text(), 'Account')]")));
                System.out.println("Redirected to account page as expected when application is pending");
                
                takeScreenshot(driver, "TC_005", "Redirected_To_Account_Page");
                
            } catch (Exception e) {
                // Option 2: We might stay on the page with a pending message
                try {
                    WebElement pendingMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//h2[contains(text(), 'Application Under Review')] | //h2[contains(text(), 'Application Pending')] | //div[contains(text(), 'pending')]")));
                    assertTrue(pendingMessage.isDisplayed(), "Application pending message should be shown");
                    System.out.println("Pending application message shown as expected");
                    
                    takeScreenshot(driver, "TC_005", "Pending_Application_Message");
                    
                } catch (Exception e2) {
                    // If both options fail, navigate to become-seller to check status
                    driver.get("http://localhost:5173/become-seller");
                    waitForPreloaderToDisappear();
                    
                    // Verify application status shows as pending/under review
                    WebElement pendingStatus = wait.until(ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//h2[contains(text(), 'Application Under Review')]")));
                    assertTrue(pendingStatus.isDisplayed(), "Application status should show 'Application Under Review'");
                    
                    takeScreenshot(driver, "TC_005", "Application_Under_Review_Status");
                }
            }
            
            System.out.println("TC_005 - Reapply as Producer: PASSED");
            
        } catch (Exception e) {
            // Take screenshot and log on failure
            if (driver instanceof JavascriptExecutor) {
                try {
                    ((JavascriptExecutor) driver).executeScript(
                            "console.error('TC_005 test failed at URL: ' + window.location.href)");
                    
                    takeScreenshot(driver, "TC_005", "Error_State");
                } catch (Exception ignored) {}
            }
            
            System.err.println("TC_005 test failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        // Let the test suite manage the driver closing
    }
} 