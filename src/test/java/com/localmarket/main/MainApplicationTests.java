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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.Collections;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

		// 2. Create Category
		logger.info("\n" + STEP_START + " STEP 2: Creating Category");
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

		// 3. Create Product
		logger.info("\n" + STEP_START + " STEP 3: Creating Product");
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

		// 4. Create Orders
		logger.info("\n" + STEP_START + " STEP 4: Creating Orders");
		// 6. Create customer order
		logger.info("6. Creating customer order...");
		OrderRequest customerOrderRequest = createOrderRequest(productId, 2);
		mockMvc.perform(post("/api/orders")
				.header("Authorization", "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(customerOrderRequest)))
				.andExpect(status().isOk());
		logger.info(STEP_SUCCESS + " Customer order created");

		// 7. Create guest order
		logger.info("7. Creating guest order...");
		OrderRequest guestOrderRequest = createGuestOrderRequest(productId);
		mockMvc.perform(post("/api/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(guestOrderRequest)))
				.andExpect(status().isOk());
		logger.info(STEP_SUCCESS + " Guest order created");

		// 8. Create guest order with account
		logger.info("8. Creating guest order with account...");
		OrderRequest guestWithAccountRequest = createGuestWithAccountRequest(productId);
		mockMvc.perform(post("/api/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(guestWithAccountRequest)))
				.andExpect(status().isOk());
		logger.info(STEP_SUCCESS + " Guest order with account created");

		logger.info(SEPARATOR + END_TEST + SEPARATOR);
	}

	private OrderRequest createOrderRequest(Long productId, int quantity) {
		OrderRequest request = new OrderRequest();
		OrderItemRequest itemRequest = new OrderItemRequest();
		itemRequest.setProductId(productId);
		itemRequest.setQuantity(quantity);
		request.setItems(Collections.singletonList(itemRequest));
		request.setShippingAddress("123 Customer St");
		request.setPhoneNumber("1234567890");

		PaymentInfo paymentInfo = new PaymentInfo();
		paymentInfo.setPaymentMethod(PaymentMethod.CARD);
		paymentInfo.setTransactionDetails("test-transaction");
		request.setPaymentInfo(paymentInfo);

		return request;
	}

	private OrderRequest createGuestOrderRequest(Long productId) {
		OrderRequest request = createOrderRequest(productId, 1);
		request.setGuestEmail("guest@test.com");
		request.setShippingAddress("456 Guest St");
		request.setPhoneNumber("0987654321");
		return request;
	}

	private OrderRequest createGuestWithAccountRequest(Long productId) {
		OrderRequest request = createOrderRequest(productId, 1);
		request.setGuestEmail("newcustomer@test.com");
		request.setShippingAddress("789 New Customer St");
		request.setPhoneNumber("1122334455");

		AccountCreationRequest accountCreation = new AccountCreationRequest();
		accountCreation.setCreateAccount(true);
		accountCreation.setUsername("newcustomer");
		accountCreation.setPassword("newcustomer123");
		request.setAccountCreation(accountCreation);

		return request;
	}
}
