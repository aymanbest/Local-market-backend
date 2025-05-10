package com.localmarket.main.selenium;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SeleniumTestSuite {
    
    private ShoppingFlowTest shoppingFlowTest;
    private ProducerTest producerTest;
    private ReviewTest reviewTest;
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
        
        System.out.println("All test classes initialized with WebDriver");
    }
    
    @AfterAll
    public void tearDown() {
        System.out.println("Cleaning up all WebDrivers after test suite");
        
        // Close any remaining WebDrivers
        if (reviewTest != null) {
            reviewTest.tearDown();
        }
        
        if (producerTest != null && producerTest.driver != null) {
            producerTest.driver.quit();
        }
        
        if (shoppingFlowTest != null && shoppingFlowTest.driver != null) {
            shoppingFlowTest.driver.quit();
        }
        
        // Final cleanup
        if (baseTest != null) {
            baseTest.tearDown();
        }
        
        // Make sure static driver is null
        BaseTest.staticDriver = null;
    }
    
    @Test
    @Order(1)
    @DisplayName("Shopping Flow Test - Complete shopping flow including payment")
    public void testTC001_CompleteShoppingFlowIncludingPayment() {
        // Ensure WebDriver is initialized
        if (shoppingFlowTest.driver == null) {
            shoppingFlowTest.setUp();
        }
        shoppingFlowTest.testTC001_CompleteShoppingFlowIncludingPayment();
    }
    
    @Test
    @Order(2)
    @DisplayName("Shopping Flow Test - Guest checkout with account creation")
    public void testTC002_GuestCheckoutWithAccountCreation() {
        // Reset session for a clean test
        baseTest.resetSession();
        shoppingFlowTest.setUp();
        shoppingFlowTest.testTC002_GuestCheckoutWithAccountCreation();
    }
    
    @Test
    @Order(3)
    @DisplayName("Shopping Flow Test - Order as a logged-in member")
    public void testTC003_OrderAsRegisteredMember() {
        // Reset session for a clean test
        baseTest.resetSession();
        shoppingFlowTest.setUp();
        shoppingFlowTest.testTC003_OrderAsRegisteredMember();
    }
    
    @Test
    @Order(4)
    @DisplayName("Producer Test - Become a Producer")
    public void testTC004_BecomeProducer() {
        // Reset session before starting Producer tests
        baseTest.resetSession();
        producerTest.setUp();
        producerTest.testTC004_BecomeProducer();
    }
    
    @Test
    @Order(5)
    @DisplayName("Producer Test - Reapply as Producer")
    public void testTC005_ReapplyAsProducer() {
        // For this test, we don't need to initialize the driver
        // as the test method will create its own WebDriver instance
        producerTest.testTC005_ReapplyAsProducer();
        
        // After this test, we need to reset the static driver reference
        // since the test creates its own driver
        BaseTest.staticDriver = null;
        
        // Reinitialize for future tests
        baseTest.setUp();
    }
    
    @Test
    @Order(6)
    @DisplayName("Review Test - Add a review for a product")
    public void testTC006_AddProductReview() throws InterruptedException {
        // For this test, we don't need to initialize the driver
        // as the test method will create its own WebDriver instance
        reviewTest.testTC006_AddProductReview();
    }
    
    @Test
    @Order(7)
    @DisplayName("Review Test - View pending reviews")
    public void testTC007_ViewPendingReviews() throws InterruptedException {
        // Ensure WebDriver is initialized
        if (reviewTest.driver == null) {
            reviewTest.setUp();
        }
        reviewTest.testTC007_ViewPendingReviews();
    }
    
    @Test
    @Order(8)
    @DisplayName("Review Test - Approve a pending review")
    public void testTC008_ApproveReview() throws InterruptedException {
        // Ensure WebDriver is initialized
        if (reviewTest.driver == null) {
            reviewTest.setUp();
        }
        reviewTest.testTC008_ApproveReview();
    }
    
    @Test
    @Order(9)
    @DisplayName("Review Test - Reject a review")
    public void testTC009_RejectReview() throws InterruptedException {
        // Ensure WebDriver is initialized
        if (reviewTest.driver == null) {
            reviewTest.setUp();
        }
        reviewTest.testTC009_RejectReview();
    }
} 