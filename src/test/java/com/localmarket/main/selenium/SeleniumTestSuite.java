package com.localmarket.main.selenium;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SeleniumTestSuite {
    
    private ShoppingFlowTest shoppingFlowTest;
    private ProducerTest producerTest;
    private ReviewTest reviewTest;
    private CommunicationTest communicationTest;
    private BaseTest baseTest;
    
    @BeforeAll
    public void setUp() {
        // Initialize base test for session management
        baseTest = new BaseTest();
        baseTest.setUp();
        
        // Initialize test classes
        shoppingFlowTest = new ShoppingFlowTest();
        shoppingFlowTest.setUp();
        
        producerTest = new ProducerTest();
        producerTest.setUp();
        
        reviewTest = new ReviewTest();
        reviewTest.setUp();
        
        communicationTest = new CommunicationTest();
        communicationTest.setUp();
        
        System.out.println("All test classes initialized with WebDriver");
    }
    
    @AfterEach
    public void cleanupAfterTest() {
        // Ensure staticDriver is properly managed
        System.out.println("Checking WebDriver state after test...");
        
        // If staticDriver is null but a test class has a non-null driver, update staticDriver
        if (BaseTest.staticDriver == null) {
            if (shoppingFlowTest != null && shoppingFlowTest.driver != null) {
                BaseTest.staticDriver = shoppingFlowTest.driver;
            } else if (producerTest != null && producerTest.driver != null) {
                BaseTest.staticDriver = producerTest.driver;
            } else if (reviewTest != null && reviewTest.driver != null) {
                BaseTest.staticDriver = reviewTest.driver;
            } else if (communicationTest != null && communicationTest.driver != null) {
                BaseTest.staticDriver = communicationTest.driver;
            } else if (baseTest != null && baseTest.driver != null) {
                BaseTest.staticDriver = baseTest.driver;
            }
        }
    }
    
    @AfterAll
    public void tearDown() {
        System.out.println("Cleaning up all WebDrivers after test suite");
        
        // Close all WebDrivers in reverse order
        if (communicationTest != null) {
            try {
                communicationTest.tearDown();
                communicationTest.driver = null;
            } catch (Exception e) {
                System.err.println("Error cleaning up communicationTest driver: " + e.getMessage());
            }
        }
        
        if (reviewTest != null) {
            try {
                reviewTest.tearDown();
                reviewTest.driver = null;
            } catch (Exception e) {
                System.err.println("Error cleaning up reviewTest driver: " + e.getMessage());
            }
        }
        
        if (producerTest != null && producerTest.driver != null) {
            try {
                producerTest.driver.quit();
                producerTest.driver = null;
            } catch (Exception e) {
                System.err.println("Error cleaning up producerTest driver: " + e.getMessage());
            }
        }
        
        if (shoppingFlowTest != null && shoppingFlowTest.driver != null) {
            try {
                shoppingFlowTest.driver.quit();
                shoppingFlowTest.driver = null;
            } catch (Exception e) {
                System.err.println("Error cleaning up shoppingFlowTest driver: " + e.getMessage());
            }
        }
        
        // Final cleanup
        if (baseTest != null) {
            try {
                baseTest.tearDown();
                baseTest.driver = null;
            } catch (Exception e) {
                System.err.println("Error cleaning up baseTest driver: " + e.getMessage());
            }
        }
        
        // Make sure static driver is null
        BaseTest.staticDriver = null;
        
        System.out.println("All WebDrivers have been properly closed");
    }
    
    @Test
    @Order(1)
    @DisplayName("Shopping Flow Test - Complete shopping flow including payment")
    public void testTC001_CompleteShoppingFlowIncludingPayment() {
        // Ensure WebDriver is initialized
        if (shoppingFlowTest.driver == null) {
            shoppingFlowTest.setUp();
        }
        try {
            shoppingFlowTest.testTC001_CompleteShoppingFlowIncludingPayment();
        } finally {
            // Ensure staticDriver is updated
            BaseTest.staticDriver = shoppingFlowTest.driver;
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("Shopping Flow Test - Guest checkout with account creation")
    public void testTC002_GuestCheckoutWithAccountCreation() {
        // Reset session for a clean test
        baseTest.resetSession();
        shoppingFlowTest.setUp();
        try {
            shoppingFlowTest.testTC002_GuestCheckoutWithAccountCreation();
        } finally {
            // Ensure staticDriver is updated
            BaseTest.staticDriver = shoppingFlowTest.driver;
        }
    }
    
    @Test
    @Order(3)
    @DisplayName("Shopping Flow Test - Order as a logged-in member")
    public void testTC003_OrderAsRegisteredMember() {
        // Reset session for a clean test
        baseTest.resetSession();
        shoppingFlowTest.setUp();
        try {
            shoppingFlowTest.testTC003_OrderAsRegisteredMember();
        } finally {
            // Ensure staticDriver is updated
            BaseTest.staticDriver = shoppingFlowTest.driver;
        }
    }
    
    @Test
    @Order(4)
    @DisplayName("Producer Test - Become a Producer")
    public void testTC004_BecomeProducer() {
        // Reset session before starting Producer tests
        baseTest.resetSession();
        producerTest.setUp();
        try {
            producerTest.testTC004_BecomeProducer();
        } finally {
            // Ensure staticDriver is updated
            BaseTest.staticDriver = producerTest.driver;
        }
    }
    
    @Test
    @Order(5)
    @DisplayName("Producer Test - Reapply as Producer")
    public void testTC005_ReapplyAsProducer() {
        try {
            producerTest.testTC005_ReapplyAsProducer();
        } finally {
            // After this test, we need to handle the WebDriver instance
            if (producerTest.driver != null && producerTest.driver != BaseTest.staticDriver) {
                try {
                    producerTest.driver.quit();
                } catch (Exception e) {
                    System.err.println("Error closing producerTest driver: " + e.getMessage());
                } finally {
                    // Update or reset static driver reference as needed
                    if (BaseTest.staticDriver == null) {
                        // If the test created a new driver and staticDriver is null, we need to
                        // reinitialize for future tests
                        baseTest.setUp();
                    }
                }
            }
        }
    }
    
    @Test
    @Order(6)
    @DisplayName("Review Test - Add a review for a product")
    public void testTC006_AddProductReview() throws InterruptedException {
        try {
            reviewTest.testTC006_AddProductReview();
        } finally {
            // Ensure any new WebDriver created by this test is properly tracked
            if (reviewTest.driver != null && reviewTest.driver != BaseTest.staticDriver) {
                BaseTest.staticDriver = reviewTest.driver;
            }
        }
    }
    
    @Test
    @Order(7)
    @DisplayName("Review Test - View pending reviews")
    public void testTC007_ViewPendingReviews() throws InterruptedException {
        // Ensure WebDriver is initialized
        if (reviewTest.driver == null) {
            reviewTest.setUp();
        }
        try {
            reviewTest.testTC007_ViewPendingReviews();
        } finally {
            // Ensure staticDriver is updated
            BaseTest.staticDriver = reviewTest.driver;
        }
    }
    
    @Test
    @Order(8)
    @DisplayName("Review Test - Approve a pending review")
    public void testTC008_ApproveReview() throws InterruptedException {
        // Ensure WebDriver is initialized
        if (reviewTest.driver == null) {
            reviewTest.setUp();
        }
        try {
            reviewTest.testTC008_ApproveReview();
        } finally {
            // Ensure staticDriver is updated
            BaseTest.staticDriver = reviewTest.driver;
        }
    }
    
    @Test
    @Order(9)
    @DisplayName("Review Test - Reject a review")
    public void testTC009_RejectReview() throws InterruptedException {
        // Ensure WebDriver is initialized
        if (reviewTest.driver == null) {
            reviewTest.setUp();
        }
        try {
            reviewTest.testTC009_RejectReview();
        } finally {
            // Ensure staticDriver is updated
            BaseTest.staticDriver = reviewTest.driver;
        }
    }
    
    @Test
    @Order(10)
    @DisplayName("Communication Test - Real-time communication between producer and admin")
    public void testTC010_RealTimeCommunication() throws InterruptedException {
        // Reset the session before running this test
        baseTest.resetSession();
        
        try {
            // This test creates its own WebDriver instances for producer and admin
            communicationTest.testTC010_RealTimeCommunication();
        } finally {
            // Ensure any WebDriver instances created by this test are properly closed
            if (communicationTest.driver != null && communicationTest.driver != BaseTest.staticDriver) {
                try {
                    communicationTest.driver.quit();
                } catch (Exception e) {
                    System.err.println("Error closing communicationTest driver: " + e.getMessage());
                } finally {
                    communicationTest.driver = null;
                }
            }
            
            // Reset static driver reference after the test
            if (BaseTest.staticDriver == null) {
                baseTest.setUp();
            }
        }
    }
} 