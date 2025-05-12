package com.localmarket.main.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommunicationTest extends BaseTest {
    // WebDriver instances for different user sessions
    private WebDriver producerDriver = null;
    private WebDriver adminDriver = null;

    // WebDriverWait instances for different browsers
    private WebDriverWait producerWait = null;
    private WebDriverWait adminWait = null;

    // Test data
    private static final String PRODUCER_EMAIL = "producer5@test.com";
    private static final String PRODUCER_PASSWORD = "producer123";
    private static final String ADMIN_EMAIL = "admin@localmarket.com";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String TEST_MESSAGE = "Le message envoyé devrait safficher immédiatement dans la fenêtre de chat.";
    private static final String TEST_ID = "TC_010";
    
    @Override
    public void tearDown() {
        closeAllDrivers();
    }
    
    @AfterEach
    public void tearDownAfterEach() {
        closeAllDrivers();
    }
    
    /**
     * Helper method to close all WebDriver instances
     */
    private void closeAllDrivers() {
        System.out.println("Cleaning up all WebDrivers for CommunicationTest");
        
        // Close producer browser instance
        if (producerDriver != null) {
            try {
                System.out.println("Closing producer WebDriver instance");
                producerDriver.quit();
            } catch (Exception e) {
                System.err.println("Error closing producer driver: " + e.getMessage());
            } finally {
                producerDriver = null;
            }
        }
        
        // Close admin browser instance
        if (adminDriver != null) {
            try {
                System.out.println("Closing admin WebDriver instance");
                adminDriver.quit();
            } catch (Exception e) {
                System.err.println("Error closing admin driver: " + e.getMessage());
            } finally {
                adminDriver = null;
            }
        }

        // Don't call super.tearDown() to avoid closing the main driver
        // The main driver will be closed by SeleniumTestSuite
    }
    
    @Test
    @DisplayName("Real-time communication between producer and admin")
    @Order(10)
    public void testTC010_RealTimeCommunication() throws InterruptedException {
        try {
            System.out.println("\n=== Starting TC_010: Real-time communication between producer and admin ===\n");
            
            // Initialize producer browser session
            producerDriver = createNewUserSession(PRODUCER_EMAIL, PRODUCER_PASSWORD);
            producerWait = new WebDriverWait(producerDriver, Duration.ofSeconds(10));
            takeScreenshot(producerDriver, TEST_ID, "ETAP1_Producer_Login_Success");
            
            // Initialize admin browser session
            adminDriver = createNewUserSession(ADMIN_EMAIL, ADMIN_PASSWORD);
            adminWait = new WebDriverWait(adminDriver, Duration.ofSeconds(10));
            takeScreenshot(adminDriver, TEST_ID, "ETAP1_Admin_Login_Success");
            
            // Step 1: Producer creates a support ticket
            // Navigate to support page in producer session
            navigateToProducerSupport(producerDriver, producerWait);
            takeScreenshot(producerDriver, TEST_ID, "ETAP2_Producer_Support_Page");
            
            // Create a new ticket
            String ticketSubject = "Test Support Ticket " + System.currentTimeMillis();
            createNewTicket(producerDriver, producerWait, ticketSubject);
            takeScreenshot(producerDriver, TEST_ID, "ETAP3_Producer_Ticket_Created");
            
            // Wait for the chat box to appear automatically
            waitForChatBox(producerDriver, producerWait);
            takeScreenshot(producerDriver, TEST_ID, "ETAP4_Producer_Chat_Open");
            
            // Step 2: Admin assigns the ticket and replies
            // Navigate to support page in admin session
            navigateToAdminSupport(adminDriver, adminWait);
            takeScreenshot(adminDriver, TEST_ID, "ETAP5_Admin_Support_Page");
            
            // Click on the "Unassigned" tab
            clickUnassignedTab(adminDriver, adminWait);
            takeScreenshot(adminDriver, TEST_ID, "ETAP6_Admin_Unassigned_Tab");
            
            // Find the ticket and assign it to the admin
            assignTicketToAdmin(adminDriver, adminWait, ticketSubject);
            takeScreenshot(adminDriver, TEST_ID, "ETAP7_Admin_Ticket_Assigned");
            
            // Navigate to "My Tickets" tab to find the assigned ticket
            clickMyTicketsTab(adminDriver, adminWait);
            takeScreenshot(adminDriver, TEST_ID, "ETAP8_Admin_My_Tickets_Tab");
            
            // Open the assigned ticket
            openTicket(adminDriver, adminWait, ticketSubject);
            takeScreenshot(adminDriver, TEST_ID, "ETAP9_Admin_Ticket_Open");
            
            // Send a message from admin to producer
            sendMessage(adminDriver, adminWait, TEST_MESSAGE);
            takeScreenshot(adminDriver, TEST_ID, "ETAP10_Admin_Message_Sent");
            
            // Step 3: Verify the message appears in producer's chat
            verifyMessageReceived(producerDriver, producerWait, TEST_MESSAGE);
            takeScreenshot(producerDriver, TEST_ID, "ETAP11_Producer_Message_Received");
            
            // Step 4: Close the ticket from admin side
            closeTicket(adminDriver, adminWait);
            takeScreenshot(adminDriver, TEST_ID, "ETAP12_Admin_Ticket_Closed");
            
            System.out.println("\n=== TC_010 COMPLETED SUCCESSFULLY ===\n");
            
        } catch (Exception e) {
            // Take screenshot on failure
            if (producerDriver != null) {
                takeScreenshot(producerDriver, TEST_ID, "ERROR_Producer_State");
            }
            if (adminDriver != null) {
                takeScreenshot(adminDriver, TEST_ID, "ERROR_Admin_State");
            }
            
            System.err.println("TC_010 test failed with exception: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to fail the test
        } finally {
            closeTicket(adminDriver, adminWait);
            // Always clean up WebDriver instances regardless of test outcome
            closeAllDrivers();
        }
    }

    /**
     * Navigate to the support page in the producer interface
     */
    private void navigateToProducerSupport(WebDriver driver, WebDriverWait wait) {
        driver.get("http://localhost:5173/producer");
        
        // Wait for page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[contains(@href, '/producer/support')]")));
                
        // Click on the Support link
        WebElement supportLink = driver.findElement(By.xpath("//a[contains(@href, '/producer/support')]"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", supportLink);
        
        // Wait for support page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h1[contains(text(), 'Support Tickets')]")));
                
        System.out.println("Navigated to producer support page");
    }
    
    /**
     * Create a new support ticket
     */
    private void createNewTicket(WebDriver driver, WebDriverWait wait, String subject) {
        // Click on "New Ticket" button
        WebElement newTicketButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'New Ticket')]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", newTicketButton);
        
        // Wait for new ticket modal to appear
        WebElement subjectInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//label[contains(text(), 'Subject')]/following::input[1]")));
        
        // Fill in ticket details
        subjectInput.sendKeys(subject);
        
        WebElement messageInput = driver.findElement(
                By.xpath("//label[contains(text(), 'Message')]/following::textarea[1]"));
        messageInput.sendKeys("This is a test support ticket created for automated testing purposes.");
        
        // Select priority (default = LOW is fine)
        
        // Submit the form
        WebElement createButton = driver.findElement(
                By.xpath("//button[contains(text(), 'Create Ticket')]"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", createButton);
        
        System.out.println("Created new support ticket: " + subject);
    }
    
    /**
     * Wait for the chat box to appear after creating a ticket
     */
    private void waitForChatBox(WebDriver driver, WebDriverWait wait) {
        // Wait for chat box to appear
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class, 'fixed inset-0 flex items-center justify-center z-[9999]')]")));
                
        // Verify the message input field is present
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//input[@placeholder='Type your message...']")));
                
        System.out.println("Chat box appeared for producer");
    }
    
    /**
     * Navigate to the support page in the admin interface
     */
    private void navigateToAdminSupport(WebDriver driver, WebDriverWait wait) {
        driver.get("http://localhost:5173/admin");
        
        // Wait for admin dashboard to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[contains(@href, '/admin/support')]")));
                
        // Click on the Support link
        WebElement supportLink = driver.findElement(By.xpath("//a[contains(@href, '/admin/support')]"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", supportLink);
        
        // Wait for support page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h1[contains(text(), 'Support Tickets')]")));
                
        System.out.println("Navigated to admin support page");
    }
    
    /**
     * Click on the "Unassigned" tab in admin support page
     */
    private void clickUnassignedTab(WebDriver driver, WebDriverWait wait) {
        WebElement unassignedTab = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'flex') and .//span[text()='Unassigned']]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", unassignedTab);
        
        // Wait for tab content to load
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class, 'grid gap-4')]")));
                
        System.out.println("Clicked on Unassigned tab");
    }
    
    /**
     * Find a ticket by subject and assign it to the admin
     */
    private void assignTicketToAdmin(WebDriver driver, WebDriverWait wait, String subject) {
        // Wait for ticket cards to appear
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(@class, 'bg-cardBg p-6 rounded-xl')]")));
                
        // Look for the ticket with the matching subject
        List<WebElement> ticketCards = driver.findElements(
                By.xpath("//div[contains(@class, 'bg-cardBg p-6 rounded-xl')]"));
                
        boolean foundTicket = false;
        
        for (WebElement ticketCard : ticketCards) {
            try {
                WebElement ticketSubjectElement = ticketCard.findElement(
                        By.xpath(".//h3[contains(@class, 'font-medium')]"));
                
                if (ticketSubjectElement.getText().equals(subject)) {
                    // Found our ticket, now click the "Assign to Me" button
                    WebElement assignButton = ticketCard.findElement(
                            By.xpath(".//button[contains(text(), 'Assign to Me')]"));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", assignButton);
                    
                    // Give time for assignment to process
                    Thread.sleep(1000);
                    
                    foundTicket = true;
                    System.out.println("Found and assigned ticket: " + subject);
                    break;
                }
            } catch (Exception e) {
                // Skip this card if it doesn't have the expected structure
                continue;
            }
        }
        
        if (!foundTicket) {
            throw new RuntimeException("Could not find ticket with subject: " + subject);
        }
    }
    
    /**
     * Click on the "My Tickets" tab in admin support page
     */
    private void clickMyTicketsTab(WebDriver driver, WebDriverWait wait) {
        WebElement myTicketsTab = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'font-medium') and .//span[text()='My Tickets']]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", myTicketsTab);
        
        // Wait for tab content to load
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class, 'grid gap-4')]")));
                
        System.out.println("Clicked on My Tickets tab");
    }
    
    /**
     * Open a ticket by subject
     */
    private void openTicket(WebDriver driver, WebDriverWait wait, String subject) {
        // Wait for ticket cards to appear
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(@class, 'bg-cardBg p-6 rounded-xl')]")));
                
        // Look for the ticket with the matching subject
        List<WebElement> ticketCards = driver.findElements(
                By.xpath("//div[contains(@class, 'bg-cardBg p-6 rounded-xl')]"));
                
        boolean foundTicket = false;
        
        for (WebElement ticketCard : ticketCards) {
            try {
                WebElement ticketSubjectElement = ticketCard.findElement(
                        By.xpath(".//h3[contains(@class, 'font-medium')]"));
                
                if (ticketSubjectElement.getText().equals(subject)) {
                    // Found our ticket, click it to open
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", ticketCard);
                    
                    // Wait for chat window to open
                    wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//input[@placeholder='Type your message...']")));
                    
                    foundTicket = true;
                    System.out.println("Opened ticket: " + subject);
                    break;
                }
            } catch (Exception e) {
                // Skip this card if it doesn't have the expected structure
                continue;
            }
        }
        
        if (!foundTicket) {
            throw new RuntimeException("Could not find ticket with subject: " + subject);
        }
    }
    
    /**
     * Send a message in the chat window
     */
    private void sendMessage(WebDriver driver, WebDriverWait wait, String message) {
        // Find and fill the message input
        WebElement messageInput = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//input[@placeholder='Type your message...']")));
        messageInput.sendKeys(message);
        
        // Find and click the send button
        WebElement sendButton = driver.findElement(
                By.xpath("//button[@type='submit']"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", sendButton);
        
        // Wait for message to appear in the chat
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//p[contains(text(), '" + message + "')]")));
                
        System.out.println("Sent message: " + message);
    }
    
    /**
     * Verify that a message appears in the producer's chat window
     */
    private void verifyMessageReceived(WebDriver driver, WebDriverWait wait, String expectedMessage) {
        // Wait for the message to appear (giving time for real-time sync)
        try {
            Thread.sleep(2000); // Allow some time for real-time message to arrive
            
            // Look for the message in the chat
            WebElement message = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//p[contains(text(), '" + expectedMessage + "')]")));
                    
            // Verify the message text
            assertTrue(message.isDisplayed(), "Message should be visible");
            assertTrue(message.getText().contains(expectedMessage), 
                    "Message text should match what was sent by admin");
            
            System.out.println("Successfully verified message received by producer");
        } catch (Exception e) {
            System.err.println("Failed to verify message: " + e.getMessage());
            throw new RuntimeException("Real-time message was not received by producer", e);
        }
    }
    
    /**
     * Close the ticket from the admin chat window
     */
    private void closeTicket(WebDriver driver, WebDriverWait wait) {
        try {
            if (driver == null || wait == null) {
                System.out.println("Cannot close ticket - driver or wait is null");
                return;
            }
            
            System.out.println("Attempting to close the ticket...");
            
            // Find and click the Close Ticket button
            WebElement closeTicketButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class, 'bg-red-600') and contains(text(), 'Close Ticket')]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", closeTicketButton);
            
            // Take screenshot of confirmation dialog
            takeScreenshot(driver, TEST_ID, "ETAP_Close_Ticket_Dialog");
            
            // Wait for the confirmation modal to appear
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//h3[contains(text(), 'Close Ticket')]")));
            
            // Find and click the "Close Permanently" button in the modal
            WebElement closePermanentlyButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class, 'bg-red-600') and contains(text(), 'Close Permanently')]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", closePermanentlyButton);
            
            // Wait for the modal to disappear, indicating the action was completed
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.xpath("//h3[contains(text(), 'Close Ticket')]")));
            
            // Wait for the chat modal to close
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.xpath("//div[contains(@class, 'fixed inset-0 flex items-center justify-center z-[9999]')]")));
            
            // Verify we're back on the support page
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//h1[contains(text(), 'Support Tickets')]")));
            
            System.out.println("Successfully closed the ticket");
        } catch (Exception e) {
            System.err.println("Failed to close ticket: " + e.getMessage());
            // Don't throw an exception here, as this might be called during cleanup
        }
    }
} 