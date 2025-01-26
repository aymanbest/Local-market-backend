package com.localmarket.main.seeder;

import com.localmarket.main.entity.user.User;
import com.localmarket.main.entity.user.Role;
import com.localmarket.main.entity.category.Category;
import com.localmarket.main.entity.product.Product;
import com.localmarket.main.entity.order.Order;
import com.localmarket.main.entity.order.OrderItem;
import com.localmarket.main.entity.order.OrderStatus;
import com.localmarket.main.entity.payment.Payment;
import com.localmarket.main.entity.payment.PaymentMethod;
import com.localmarket.main.entity.payment.PaymentStatus;
import com.localmarket.main.repository.user.UserRepository;
import com.localmarket.main.repository.category.CategoryRepository;
import com.localmarket.main.repository.product.ProductRepository;
import com.localmarket.main.repository.order.OrderRepository;
import com.localmarket.main.repository.order.OrderItemRepository;
import com.localmarket.main.repository.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import java.util.Map;
import java.math.RoundingMode;
import java.util.HashMap;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    private static final LocalDateTime[] DECEMBER_DATES = new LocalDateTime[31];
    private static final LocalDateTime[] JANUARY_DATES = new LocalDateTime[31];

    // Initialize both date arrays in the static block
    static {
        Random random = new Random();
        // December dates
        for (int day = 1; day <= 31; day++) {
            int hour = 9 + random.nextInt(9);
            int minute = random.nextInt(60);
            int second = random.nextInt(60);
            DECEMBER_DATES[day - 1] = LocalDateTime.of(2024, 12, day, hour, minute, second);
        }
        
        // January dates
        for (int day = 1; day <= 31; day++) {
            int hour = 9 + random.nextInt(9);
            int minute = random.nextInt(60);
            int second = random.nextInt(60);
            JANUARY_DATES[day - 1] = LocalDateTime.of(2025, 1, day, hour, minute, second);
        }
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 1) { // Skip if data exists (besides admin)
            return;
        }

        // Seed Categories
        List<Category> categories = seedCategories();

        // Seed Producers
        List<User> producers = seedProducers();

        // Seed Products
        List<Product> products = seedProducts(producers, categories);

        // Seed Customers
        List<User> customers = seedCustomers();

        // Seed Orders
        seedOrders(customers, products);
    }

    private List<Category> seedCategories() {
        String[] categoryNames = { "Vegetables", "Fruits", "Dairy", "Meat", "Bakery", "Honey", "Eggs" };
        List<Category> categories = new ArrayList<>();

        for (String name : categoryNames) {
            Category category = new Category();
            category.setName(name);
            categories.add(categoryRepository.save(category));
        }

        return categories;
    }

    private List<User> seedProducers() {
        List<User> producers = new ArrayList<>();
        String[] firstNames = { "Michael", "Sarah", "David", "Emma", "James" };
        String[] lastNames = { "Anderson", "Williams", "Johnson", "Thompson", "Richardson" };

        for (int i = 0; i < 5; i++) {
            User producer = new User();
            producer.setUsername("producer" + (i + 1));
            producer.setEmail("producer" + (i + 1) + "@test.com");
            producer.setFirstname(firstNames[i]);
            producer.setLastname(lastNames[i]);
            producer.setPasswordHash(passwordEncoder.encode("producer123"));
            producer.setRole(Role.PRODUCER);
            producer.setLastLogin(LocalDateTime.now().minusDays(random.nextInt(30)));
            producers.add(userRepository.save(producer));
        }
        return producers;
    }

    private List<Product> seedProducts(List<User> producers, List<Category> categories) {
        List<Product> products = new ArrayList<>();
        String[] productNames = { "Fresh Tomatoes", "Organic Apples", "Farm Eggs", "Raw Honey", "Whole Milk" };

        for (User producer : producers) {
            for (int i = 0; i < 3; i++) {
                Product product = new Product();
                product.setName(productNames[random.nextInt(productNames.length)] + " by " + producer.getUsername());
                product.setDescription("Fresh local product");
                product.setPrice(BigDecimal.valueOf(10 + random.nextInt(90)));
                product.setQuantity(100 + random.nextInt(900));
                product.setProducer(producer);

                Set<Category> productCategories = new HashSet<>();
                productCategories.add(categories.get(random.nextInt(categories.size())));
                product.setCategories(productCategories);

                products.add(productRepository.save(product));
            }
        }
        return products;
    }

    private List<User> seedCustomers() {
        List<User> customers = new ArrayList<>();
        String[] firstNames = { "John", "Emily", "Robert", "Lisa", "Daniel", "Anna", "Peter", "Mary", "Thomas",
                "Sophie" };
        String[] lastNames = { "Smith", "Brown", "Davis", "Miller", "Wilson", "Moore", "Taylor", "Anderson", "Thomas",
                "Jackson" };

        for (int i = 0; i < 10; i++) {
            User customer = new User();
            customer.setUsername("customer" + (i + 1));
            customer.setEmail("customer" + (i + 1) + "@test.com");
            customer.setFirstname(firstNames[i]);
            customer.setLastname(lastNames[i]);
            customer.setPasswordHash(passwordEncoder.encode("customer123"));
            customer.setRole(Role.CUSTOMER);
            customer.setLastLogin(LocalDateTime.now().minusDays(random.nextInt(30)));
            customers.add(userRepository.save(customer));
        }
        return customers;
    }

    private void seedOrders(List<User> customers, List<Product> products) {
        Map<String, BigDecimal> decemberRevenue = new HashMap<>();
        Map<String, BigDecimal> januaryRevenue = new HashMap<>();

        // LocalDateTime decemberStart = LocalDateTime.of(2024, 12, 1, 0, 0);
        // LocalDateTime decemberEnd = LocalDateTime.of(2024, 12, 31, 0, 0);

        // December orders (previous period) - base volumes
        for (User customer : customers) {
            // Producer 1 - High volume in December
            createOrderForProducer(customer, products, "producer1",
                    null, null, 20, decemberRevenue);

            // Producer 2 - Medium volume in December
            createOrderForProducer(customer, products, "producer2",
            null, null, 15, decemberRevenue);

            // Producer 3 - Low volume in December
            createOrderForProducer(customer, products, "producer3",
            null, null, 10, decemberRevenue);

            // Producer 4 - Very low volume in December
            createOrderForProducer(customer, products, "producer4",
            null, null, 5, decemberRevenue);

            // Producer 5 - Minimal volume in December
            createOrderForProducer(customer, products, "producer5",
            null, null, 2, decemberRevenue);
        }

        // January orders (current period) - different growth patterns
        for (User customer : customers) {
            // Producer 1 - 50% growth
            createOrderForProducer(customer, products, "producer1",
                    LocalDateTime.of(2025, 1, 1, 0, 0),
                    LocalDateTime.of(2025, 1, 31, 23, 59), 30, januaryRevenue);

            // Producer 2 - 100% growth
            createOrderForProducer(customer, products, "producer2",
                    LocalDateTime.of(2025, 1, 1, 0, 0),
                    LocalDateTime.of(2025, 1, 31, 23, 59), 30, januaryRevenue);

            // Producer 3 - 200% growth
            createOrderForProducer(customer, products, "producer3",
                    LocalDateTime.of(2025, 1, 1, 0, 0),
                    LocalDateTime.of(2025, 1, 31, 23, 59), 30, januaryRevenue);

            // Producer 4 - 500% growth
            createOrderForProducer(customer, products, "producer4",
                    LocalDateTime.of(2025, 1, 1, 0, 0),
                    LocalDateTime.of(2025, 1, 31, 23, 59), 30, januaryRevenue);

            // Producer 5 - 1000% growth
            createOrderForProducer(customer, products, "producer5",
                    LocalDateTime.of(2025, 1, 1, 0, 0),
                    LocalDateTime.of(2025, 1, 31, 23, 59), 22, januaryRevenue);
        }

        // Log revenue and growth rates for verification
        System.out.println("\nProducer Performance Report:");
        System.out.println("============================");

        decemberRevenue.forEach((producer, decRevenue) -> {
            BigDecimal janRevenue = januaryRevenue.getOrDefault(producer, BigDecimal.ZERO);
            double growth = decRevenue.equals(BigDecimal.ZERO)
                    ? (janRevenue.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0)
                    : janRevenue.subtract(decRevenue)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(decRevenue, 2, RoundingMode.HALF_UP)
                            .doubleValue();

            System.out.printf("%s:\n", producer);
            System.out.printf("  December Revenue: $%,.2f\n", decRevenue);
            System.out.printf("  January Revenue: $%,.2f\n", januaryRevenue.getOrDefault(producer, BigDecimal.ZERO));
            System.out.printf("  Growth Rate: %.2f%%\n\n", growth);
        });

        // Handle producer5 separately (infinite growth case)
        if (!decemberRevenue.containsKey("producer5") && januaryRevenue.containsKey("producer5")) {
            System.out.printf("producer5:\n");
            System.out.printf("  December Revenue: $0.00\n");
            System.out.printf("  January Revenue: $%,.2f\n", januaryRevenue.get("producer5"));
            System.out.printf("  Growth Rate: 100.00%%\n\n");
        }
    }

    private void createOrderForProducer(User customer, List<Product> products, String producerUsername,
            LocalDateTime startDate, LocalDateTime endDate, int orderCount, Map<String, BigDecimal> revenueMap) {
        try {
            List<Product> producerProducts = products.stream()
                    .filter(p -> p.getProducer().getUsername().equals(producerUsername))
                    .collect(Collectors.toList());

            if (producerProducts.isEmpty()) {
                System.out.println("No products found for producer: " + producerUsername);
                return;
            }

            BigDecimal producerRevenue = BigDecimal.ZERO;

            for (int i = 0; i < orderCount; i++) {
                Order order = new Order();
                order.setCustomer(customer);
                order.setShippingAddress("123 Test St, City");
                order.setPhoneNumber("+1234567890");
                order.setStatus(OrderStatus.DELIVERED);

                // Choose date array based on period
                LocalDateTime orderDate;
                if (startDate != null && startDate.getYear() == 2025) {
                    orderDate = JANUARY_DATES[random.nextInt(JANUARY_DATES.length)];
                } else {
                    orderDate = DECEMBER_DATES[random.nextInt(DECEMBER_DATES.length)];
                }
                
                // System.out.println("Setting order date to: " + orderDate);
                order.setOrderDate(orderDate);

                // Create order items
                Set<OrderItem> orderItems = new HashSet<>();
                int itemCount = 1 + random.nextInt(3);

                for (int j = 0; j < itemCount; j++) {
                    Product product = producerProducts.get(random.nextInt(producerProducts.size()));
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setProduct(product);
                    orderItem.setQuantity(1 + random.nextInt(5));
                    orderItem.setPrice(product.getPrice());
                    orderItems.add(orderItem);
                }

                // Calculate total price first
                BigDecimal totalPrice = orderItems.stream()
                        .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                order.setTotalPrice(totalPrice);

                // Set items to order before saving
                List<OrderItem> savedItems = new ArrayList<>();
                for (OrderItem item : orderItems) {
                    item.setOrder(order);
                    savedItems.add(item);
                }
                order.setItems(savedItems);

                // Now save order with its items
                orderRepository.save(order);

                producerRevenue = producerRevenue.add(totalPrice);
                // System.out.println(
                //         "Saved order " + savedOrder.getOrderId() + " for " + producerUsername + " at " + orderDate);
            }

            revenueMap.merge(producerUsername, producerRevenue, BigDecimal::add);

        } catch (Exception e) {
            System.err.println("Error creating orders for " + producerUsername + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}