package com.localmarket.main;

import com.localmarket.main.dto.auth.RegisterRequest;
import com.localmarket.main.dto.category.CategoryRequest;
import com.localmarket.main.dto.order.OrderItemRequest;
import com.localmarket.main.dto.order.OrderRequest;
import com.localmarket.main.dto.payment.PaymentInfo;
import com.localmarket.main.dto.product.ProductRequest;
import com.localmarket.main.dto.user.AccountCreationRequest;
import com.localmarket.main.entity.payment.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.localmarket.main.dto.order.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.Collections;
import org.springframework.transaction.annotation.Transactional;
import com.localmarket.main.dto.producer.ProducerApplicationRequest;
import com.localmarket.main.dto.auth.LoginRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MainApplicationTests {

	private static final Logger logger = LoggerFactory.getLogger(MainApplicationTests.class);
	private static final String SEPARATOR = "\n========================================\n";
	private static final String START_TEST = "🚀 STARTING INTEGRATION TEST";
	private static final String END_TEST = "✨ TEST COMPLETED SUCCESSFULLY";
	private static final String STEP_SUCCESS = "✅";
	private static final String STEP_START = "📍";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void applicationFlowTest() throws Exception {
		String timestamp = String.valueOf(System.currentTimeMillis());

		logger.info(SEPARATOR + START_TEST + SEPARATOR);

		// 1. Register Users
		logger.info(STEP_START + " STEP 1: Creating Users");
		// Admin registration
		RegisterRequest adminRequest = new RegisterRequest();
		adminRequest.setUsername("admin" + timestamp);
		adminRequest.setEmail("admin" + timestamp + "@test.com");
		adminRequest.setPassword("admin123");
		adminRequest.setRole("ADMIN");

		MvcResult adminResult = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(adminRequest)))
				.andExpect(status().isOk())
				.andReturn();

		String adminToken = objectMapper.readTree(adminResult.getResponse().getContentAsString())
				.get("token").asText();
		logger.info(STEP_SUCCESS + " Admin created: " + adminRequest.getEmail());

		// Producer registration
		RegisterRequest producerRequest = new RegisterRequest();
		producerRequest.setUsername("producer" + timestamp);
		producerRequest.setEmail("producer" + timestamp + "@test.com");
		producerRequest.setPassword("producer123");
		producerRequest.setRole("PRODUCER");

		MvcResult producerResult = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(producerRequest)))
				.andExpect(status().isOk())
				.andReturn();

		String producerToken = objectMapper.readTree(producerResult.getResponse().getContentAsString())
				.get("token").asText();
		logger.info(STEP_SUCCESS + " Producer created: " + producerRequest.getEmail());

		// Customer registration
		RegisterRequest customerRequest = new RegisterRequest();
		customerRequest.setUsername("customer" + timestamp);
		customerRequest.setEmail("customer" + timestamp + "@test.com");
		customerRequest.setPassword("customer123");
		customerRequest.setRole("CUSTOMER");

		MvcResult customerResult = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(customerRequest)))
				.andExpect(status().isOk())
				.andReturn();

		String customerToken = objectMapper.readTree(customerResult.getResponse().getContentAsString())
				.get("token").asText();
		logger.info(STEP_SUCCESS + " Customer created: " + customerRequest.getEmail());

		// Check initial status (NOT_APPLIED)
		MvcResult initialStatus = mockMvc.perform(get("/api/producer-applications/status")
				.header("Authorization", "Bearer " + customerToken))
				.andExpect(status().isOk())
				.andReturn();
		String statusResponse = initialStatus.getResponse().getContentAsString();
		assert statusResponse.contains("NO_APPLICATION") : "Expected status to be NO_APPLICATION but was: " + statusResponse;
		logger.info(STEP_SUCCESS + " Initial status verified: NO_APPLICATION");

		// 2. Test Producer Application Flow
		logger.info("\n" + STEP_START + " STEP 2: Testing Producer Application Flow");

		// Submit application
		ProducerApplicationRequest applicationRequest = new ProducerApplicationRequest();
		applicationRequest.setBusinessName("Fresh Farm");
		applicationRequest.setBusinessDescription("Local organic farm");
		applicationRequest.setCategories(new String[] { "Fruits", "Vegetables" });
		applicationRequest.setBusinessAddress("123 Farm Road");
		applicationRequest.setCityRegion("Rural County");
		applicationRequest.setYearsOfExperience(5);
		applicationRequest.setWebsiteOrSocialLink("https://freshfarm.com");
		applicationRequest.setMessageToAdmin("Excited to join the platform!");

		MvcResult applicationResult = mockMvc.perform(post("/api/producer-applications")
				.header("Authorization", "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(applicationRequest)))
				.andExpect(status().isOk())
				.andReturn();

		Long applicationId = objectMapper.readTree(applicationResult.getResponse().getContentAsString())
				.get("applicationId").asLong();
		logger.info(STEP_SUCCESS + " Producer application submitted");

		// Admin declines first application
		mockMvc.perform(post("/api/producer-applications/" + applicationId + "/decline")
				.header("Authorization", "Bearer " + adminToken)
				.param("reason", "More experience needed")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
		logger.info(STEP_SUCCESS + " First application declined");

		// Submit second application with custom category
		ProducerApplicationRequest secondApplicationRequest = new ProducerApplicationRequest();
		secondApplicationRequest.setBusinessName("Fresh Farm");
		secondApplicationRequest.setBusinessDescription("Local organic farm");
		secondApplicationRequest.setCategories(new String[] {"Fruits", "Vegetables"});
		secondApplicationRequest.setCustomCategory("banan easy");
		secondApplicationRequest.setBusinessAddress("123 Farm Road");
		secondApplicationRequest.setCityRegion("Rural County");
		secondApplicationRequest.setYearsOfExperience(10);
		secondApplicationRequest.setWebsiteOrSocialLink("https://freshfarm.com");
		secondApplicationRequest.setMessageToAdmin("Reapplying with more experience");

		MvcResult secondApplicationResult = mockMvc.perform(post("/api/producer-applications")
				.header("Authorization", "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(secondApplicationRequest)))
				.andExpect(status().isOk())
				.andReturn();
		
		Long secondApplicationId = objectMapper.readTree(secondApplicationResult.getResponse().getContentAsString())
				.get("applicationId").asLong();
		logger.info(STEP_SUCCESS + " Second producer application submitted");

		// Admin approves second application
		mockMvc.perform(post("/api/producer-applications/" + secondApplicationId + "/approve")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
		logger.info(STEP_SUCCESS + " Second application approved");

		// Login again to get new token with updated role
		// Wait 6 seconds before logging in again to ensure token updates are processed
		Thread.sleep(6000);

		MvcResult newLoginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new LoginRequest(customerRequest.getEmail(), customerRequest.getPassword()))))
				.andExpect(status().isOk())
				.andReturn();

		String updatedCustomerToken = objectMapper.readTree(newLoginResult.getResponse().getContentAsString())
				.get("token").asText();
		logger.info(STEP_SUCCESS + " Re-logged in with updated role");

		// Verify the customer can now create products (with new token)
		ProductRequest testProductRequest = new ProductRequest();
		testProductRequest.setName("Fresh Apples");
		testProductRequest.setDescription("Organic fresh apples");
		testProductRequest.setPrice(new BigDecimal("2.99"));
		testProductRequest.setQuantity(100);
		testProductRequest.setImageUrl("https://example.com/apple.jpg");

		mockMvc.perform(post("/api/products")
				.header("Authorization", "Bearer " + updatedCustomerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(testProductRequest)))
				.andExpect(status().isOk());
		logger.info(STEP_SUCCESS + " Verified role change: Customer can now create products as Producer");

		// 3. Create Category
		logger.info("\n" + STEP_START + " STEP 3: Creating Category");
		CategoryRequest categoryRequest = new CategoryRequest();
		categoryRequest.setName("Fruits");

		MvcResult categoryResult = mockMvc.perform(post("/api/categories")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(categoryRequest)))
				.andExpect(status().isOk())
				.andReturn();

		Long categoryId = objectMapper.readTree(categoryResult.getResponse().getContentAsString())
				.get("categoryId").asLong();
		logger.info(STEP_SUCCESS + " Category created: " + categoryRequest.getName());

		// 4. Create Product
		logger.info("\n" + STEP_START + " STEP 4: Creating Product");
		ProductRequest productRequest = new ProductRequest();
		productRequest.setName("Fresh Apples");
		productRequest.setDescription("Organic fresh apples");
		productRequest.setPrice(new BigDecimal("2.99"));
		productRequest.setQuantity(100);
		productRequest.setImageUrl("https://example.com/apple.jpg");
		productRequest.setCategoryIds(Collections.singleton(categoryId));

		MvcResult productResult = mockMvc.perform(post("/api/products")
				.header("Authorization", "Bearer " + producerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(productRequest)))
				.andExpect(status().isOk())
				.andReturn();

		Long productId = objectMapper.readTree(productResult.getResponse().getContentAsString())
				.get("productId").asLong();
		logger.info(STEP_SUCCESS + " Product created: Fresh Apples");

		// 4. Test Customer Order Flow
		logger.info("\n" + STEP_START + " STEP 4: Testing Customer Order Flow");
		OrderRequest customerOrderRequest = createOrderRequest(productId, 2);
		MvcResult customerOrderResult = mockMvc.perform(post("/api/orders/checkout")
				.header("Authorization", "Bearer " + updatedCustomerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(customerOrderRequest)))
				.andExpect(status().isOk())
				.andReturn();

		Long customerOrderId = objectMapper.readTree(customerOrderResult.getResponse().getContentAsString())
				.get("orderId").asLong();

		PaymentInfo customerPayment = createCardPayment("customer-transaction");

		mockMvc.perform(post("/api/orders/" + customerOrderId + "/pay")
				.header("Authorization", "Bearer " + updatedCustomerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(customerPayment)))
				.andExpect(status().isOk());
		logger.info(STEP_SUCCESS + " Customer order completed");

		// 5. Test Guest Order Flow (No Registration)
		logger.info("\n" + STEP_START + " STEP 5: Testing Guest Order Flow");
		OrderRequest guestOrderRequest = createGuestOrderRequest(productId);
		MvcResult guestOrderResult = mockMvc.perform(post("/api/orders/checkout")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(guestOrderRequest)))
				.andExpect(status().isOk())
				.andReturn();

		Long guestOrderId = objectMapper.readTree(guestOrderResult.getResponse().getContentAsString())
				.get("orderId").asLong();
		logger.info(STEP_SUCCESS + " Guest order created (abandoned payment)");

		// Try to access abandoned order
		mockMvc.perform(get("/api/orders/" + guestOrderId)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
		logger.info(STEP_SUCCESS + " Guest cannot access abandoned order");

		// 6. Test Guest Order Flow (With Registration)
		logger.info("\n" + STEP_START + " STEP 6: Testing Guest Order with Registration");
		OrderRequest registerGuestRequest = createGuestWithAccountRequest(productId);
		MvcResult registerGuestResult = mockMvc.perform(post("/api/orders/checkout")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerGuestRequest)))
				.andExpect(status().isOk())
				.andReturn();

		OrderResponse response = objectMapper.readValue(
				registerGuestResult.getResponse().getContentAsString(),
				OrderResponse.class);
		String newCustomerToken = response.getToken();
		Long registerGuestOrderId = response.getOrder().getOrderId();

		PaymentInfo newCustomerPayment = createCardPayment("new-customer-transaction");

		mockMvc.perform(post("/api/orders/" + registerGuestOrderId + "/pay")
				.header("Authorization", "Bearer " + newCustomerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(newCustomerPayment)))
				.andExpect(status().isOk());
		logger.info(STEP_SUCCESS + " Guest order with registration completed");

		logger.info(SEPARATOR + END_TEST + SEPARATOR);
	}

	private OrderRequest createOrderRequest(Long productId, int quantity) {
		OrderRequest request = new OrderRequest();
		OrderItemRequest itemRequest = new OrderItemRequest();
		itemRequest.setProductId(productId);
		itemRequest.setQuantity(quantity);
		request.setItems(Collections.singletonList(itemRequest));
		request.setShippingAddress("123 Customer St");
		request.setPhoneNumber("+12345678901");
		return request;
	}

	private OrderRequest createGuestOrderRequest(Long productId) {
		OrderRequest request = createOrderRequest(productId, 1);
		request.setGuestEmail("guest@test.com");
		request.setShippingAddress("456 Guest St");
		request.setPhoneNumber("+12345678901");
		return request;
	}

	private OrderRequest createGuestWithAccountRequest(Long productId) {
		OrderRequest request = createOrderRequest(productId, 1);
		request.setGuestEmail("newcustomer@test.com");
		request.setShippingAddress("789 New Customer St");
		request.setPhoneNumber("+12345678901");

		AccountCreationRequest accountCreation = new AccountCreationRequest();
		accountCreation.setCreateAccount(true);
		accountCreation.setUsername("newcustomer");
		accountCreation.setPassword("newcustomer123");
		request.setAccountCreation(accountCreation);

		return request;
	}

	private PaymentInfo createCardPayment(String transactionDetails) {
		PaymentInfo payment = new PaymentInfo();
		payment.setPaymentMethod(PaymentMethod.CARD);
		payment.setTransactionDetails(transactionDetails);
		payment.setCardNumber("4111111111111111");
		payment.setCardHolderName("Test User");
		payment.setExpiryDate("12/25");
		payment.setCvv("123");
		payment.setCurrency("USD");
		return payment;
	}

	private PaymentInfo createBitcoinPayment(String transactionDetails) {
		PaymentInfo payment = new PaymentInfo();
		payment.setPaymentMethod(PaymentMethod.BITCOIN);
		payment.setTransactionDetails(transactionDetails);
		payment.setTransactionHash("0x123abc...");
		payment.setCurrency("BTC");
		return payment;
	}
}
