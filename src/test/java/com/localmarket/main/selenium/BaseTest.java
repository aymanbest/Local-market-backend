package com.localmarket.main.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseTest {
    // Static WebDriver instance to be shared across test classes
    public static WebDriver staticDriver = null;
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected Wait<WebDriver> fluentWait;
    protected ChromeOptions options = new ChromeOptions();
    // Configure Chrome options
    {
        options.addArguments("--headless");
    }
    
    // Directory for storing screenshots
    private static final String SCREENSHOT_DIR = "test-screenshots";

    @BeforeEach
    public void setUp() {
        // Suppress Selenium CDP warning messages
        Logger.getLogger("org.openqa.selenium").setLevel(Level.SEVERE);
        System.setProperty("webdriver.chrome.silentOutput", "true");
        
        WebDriverManager.chromedriver().setup();
        
        // ChromeOptions options = new ChromeOptions();
        //options.addArguments("--headless");
        
        // Check if there's an existing driver and if it's valid
        boolean needNewDriver = true;
        
        if (staticDriver != null) {
            try {
                // Try to get the current URL to check if driver is still active
                staticDriver.getCurrentUrl();
                needNewDriver = false;
                System.out.println("Reusing existing WebDriver instance");
            } catch (Exception e) {
                System.out.println("Existing WebDriver is not responding, creating new instance");
                try {
                    staticDriver.quit();
                } catch (Exception ex) {
                    // Ignore - driver may already be closed
                }
                staticDriver = null;
            }
        }
        
        if (needNewDriver) {
            System.out.println("Creating new WebDriver instance");
            staticDriver = new ChromeDriver(options);
        }
        
        driver = staticDriver;
        
        // Standard wait (10 seconds)
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Fluent wait configuration
        fluentWait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(30))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
                
        // Maximize window for better element visibility
        driver.manage().window().maximize();
        
        // Create screenshot directory if it doesn't exist
        createScreenshotDirectory();
    }
    
    /**
     * Creates the screenshot directory if it doesn't exist
     */
    private void createScreenshotDirectory() {
        File directory = new File(SCREENSHOT_DIR);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                System.out.println("Created screenshot directory: " + directory.getAbsolutePath());
            } else {
                System.err.println("Failed to create screenshot directory: " + directory.getAbsolutePath());
            }
        }
    }
    
    /**
     * Takes a screenshot and saves it with a custom name
     * 
     * @param driver The WebDriver instance to use for taking the screenshot
     * @param testName The test case name (e.g., "TC_001")
     * @param stepName The step name or description (e.g., "Login_Form_Filled")
     * @return Path to the saved screenshot file, or null if screenshot failed
     */
    protected String takeScreenshot(WebDriver driver, String testName, String stepName) {
        // if (driver == null) {
        //     System.err.println("Cannot take screenshot - driver is null");
        //     return null;
        // }
        
        // try {
        //     // Clean up the step name to make it file-system friendly
        //     String cleanStepName = stepName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            
        //     // Create timestamp for unique filename
        //     String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            
        //     // Create filename
        //     String filename = testName + "_" + cleanStepName + "_" + timestamp + ".png";
            
        //     // Take screenshot
        //     File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            
        //     // Create target file path
        //     Path targetPath = Paths.get(SCREENSHOT_DIR, filename);
            
        //     // Copy screenshot to target location
        //     Files.copy(screenshotFile.toPath(), targetPath);
            
        //     System.out.println("Screenshot saved: " + targetPath.toAbsolutePath());
        //     return targetPath.toString();
            
        // } catch (IOException e) {
        //     System.err.println("Failed to save screenshot: " + e.getMessage());
        //     return null;
        // } catch (Exception e) {
        //     System.err.println("Error taking screenshot: " + e.getMessage());
        //     return null;
        // }
        return null;
    }

    @AfterAll
    public void tearDown() {
        if (driver != null) {
            try {
                System.out.println("Closing WebDriver instance from " + this.getClass().getSimpleName());
                driver.quit();
                // Only set staticDriver to null if it's the same instance being closed
                if (staticDriver == driver) {
                    staticDriver = null;
                }
                driver = null;
            } catch (Exception e) {
                System.err.println("Error closing WebDriver: " + e.getMessage());
            }
        }
    }
    
    /**
     * Creates a new browser session by closing the current one and reinitializing
     * This is useful when we need to reset the session state (e.g., for logout/login tests)
     */
    protected void resetSession() {
        System.out.println("Resetting browser session...");
        if (driver != null) {
            try {
                driver.quit();
                // Only set staticDriver to null if it's the same instance being closed
                if (staticDriver == driver) {
                    staticDriver = null;
                }
                driver = null;
            } catch (Exception e) {
                System.err.println("Error closing WebDriver during reset: " + e.getMessage());
            }
        }
        
        // Set up a new driver
        // ChromeOptions options = new ChromeOptions();
        staticDriver = new ChromeDriver(options);
        driver = staticDriver;
        
        // Reinitialize waits
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        fluentWait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(30))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
                
        // Maximize window for better element visibility
        driver.manage().window().maximize();
        System.out.println("New browser session created");
    }
    
    /**
     * Clean up any WebDriver resources that may have been created independently
     * Call this when a test creates its own WebDriver instead of using staticDriver
     * @param individualDriver The WebDriver instance to close
     */
    protected void closeIndividualDriver(WebDriver individualDriver) {
        if (individualDriver != null && individualDriver != staticDriver) {
            try {
                System.out.println("Closing individual WebDriver instance");
                individualDriver.quit();
            } catch (Exception e) {
                System.err.println("Error closing individual WebDriver: " + e.getMessage());
            }
        }
    }
    
    /**
     * Wait for React-based animations and state changes to complete
     */
    protected void waitForAnimations() {
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
     * THE TRUCK PRELOADER
     */
    protected void waitForPreloaderToDisappear() {
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
    
    /**
     * Complete payment form with test credit card details
     */
    protected void completePayment() {
        completePayment(null);
    }
    
    protected void completePayment(String TestName) {
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
        
        takeScreenshot(driver, TestName, "Fill_out_payment_form");
        // Wait for animations
        waitForAnimations();
        
        // Click Pay Securely Now button
        WebElement payButton = driver.findElement(By.cssSelector("button[type=\"submit\"]"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", payButton);

        takeScreenshot(driver, TestName, "Pay_Button_Clicked");
    }
    
    /**
     * Helper method to create a new browser session for a specific user role
     * @param email User email for login
     * @param password User password
     * @return WebDriver instance with logged-in session
     */
    protected WebDriver createNewUserSession(String email, String password) throws InterruptedException {
        System.out.println("Creating new browser session for: " + email);
        
        // Set up a new driver instance
        WebDriverManager.chromedriver().setup();
        // ChromeOptions options = new ChromeOptions();
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
     * @param seconds Number of seconds to pause
     */
    protected void breakpoint(String message, int seconds) {
        System.out.println("\n=== BREAKPOINT: " + message + " ===");
        System.out.println("Test paused for " + seconds + " seconds to allow inspection...");
        
        try {
            // Take screenshot if driver is available
            if (driver instanceof JavascriptExecutor) {
                ((JavascriptExecutor) driver).executeScript(
                    "console.log('BREAKPOINT: " + message.replace("'", "\\'") + "')");
            }
            
            // Show current URL for context
            System.out.println("Browser URL: " + (driver != null ? driver.getCurrentUrl() : "N/A"));
            
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
    protected void breakpoint(String message) {
        breakpoint(message, 10);
    }
} 