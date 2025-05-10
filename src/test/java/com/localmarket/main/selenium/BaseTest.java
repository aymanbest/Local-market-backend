package com.localmarket.main.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseTest {
    // Static WebDriver instance to be shared across test classes
    public static WebDriver staticDriver = null;
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected Wait<WebDriver> fluentWait;

    @BeforeEach
    public void setUp() {
        // Suppress Selenium CDP warning messages
        Logger.getLogger("org.openqa.selenium").setLevel(Level.SEVERE);
        System.setProperty("webdriver.chrome.silentOutput", "true");
        
        // Set up WebDriverManager for ChromeDriver
        WebDriverManager.chromedriver().setup();
        
        // Chrome WebDriver setup
        ChromeOptions options = new ChromeOptions();
        // Add options if needed (headless, disable-gpu, etc.)
        //options.addArguments("--headless");
        
        // Reuse the static driver if it exists, otherwise create a new one
        if (staticDriver == null) {
            System.out.println("Creating new WebDriver instance");
            staticDriver = new ChromeDriver(options);
        } else {
            System.out.println("Reusing existing WebDriver instance");
        }
        driver = staticDriver;
        
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
        // Only close browser instance if this is the SeleniumTestSuite
        if (driver != null) {
            System.out.println("Closing WebDriver instance");
            driver.quit();
            staticDriver = null;
        }
    }
    
    /**
     * Creates a new browser session by closing the current one and reinitializing
     * This is useful when we need to reset the session state (e.g., for logout/login tests)
     */
    protected void resetSession() {
        System.out.println("Resetting browser session...");
        if (driver != null) {
            driver.quit();
            staticDriver = null;
        }
        
        // Set up a new driver
        ChromeOptions options = new ChromeOptions();
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
     * Helper method to create a new browser session for a specific user role
     * @param email User email for login
     * @param password User password
     * @return WebDriver instance with logged-in session
     */
    protected WebDriver createNewUserSession(String email, String password) throws InterruptedException {
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