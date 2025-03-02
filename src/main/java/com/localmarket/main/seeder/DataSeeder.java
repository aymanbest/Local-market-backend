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
import com.localmarket.main.entity.review.Review;
import com.localmarket.main.entity.review.ReviewStatus;
import com.localmarket.main.repository.user.UserRepository;
import com.localmarket.main.repository.category.CategoryRepository;
import com.localmarket.main.repository.product.ProductRepository;
import com.localmarket.main.repository.order.OrderRepository;
import com.localmarket.main.repository.payment.PaymentRepository;
import com.localmarket.main.repository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.Map;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import com.localmarket.main.entity.product.ProductStatus;
import com.localmarket.main.service.coupon.CouponService;
import java.util.UUID;
import com.localmarket.main.service.auth.TokenService;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();
    private final CouponService couponService;
    private final TokenService tokenService;

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
        // Initialize welcome coupon regardless of existing data
        couponService.initializeWelcomeCoupon();
        
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
        
        // Seed Reviews
        seedReviews();
    }

    private List<Category> seedCategories() {
        String[] categoryNames = {
            "Fresh Produce",
            "Meat & Poultry",
            "Seafood",
            "Dairy & Eggs",
            "Baked Goods",
            "Specialty Foods",
            "Beverages",
            "Prepared Foods",
            "Snacks"
        };
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
        
        // Map of category name to list of products in that category
        Map<String, List<ProductInfo>> categoryProducts = createProductCatalog();
        
        String[] declineReasons = {
            "Product pricing exceeds market standards",
            "Insufficient product description and details",
            "Missing required certification documentation",
            "Product images don't meet quality guidelines",
            "Incomplete ingredient information"
        };

        for (User producer : producers) {
            // Create at least 6 products for each producer
            int productCount = 6 + random.nextInt(3);
            
            // Ensure each producer has products in different categories
            List<Category> shuffledCategories = new ArrayList<>(categories);
            Collections.shuffle(shuffledCategories);
            
            for (int i = 0; i < productCount; i++) {
                // Select a category for this product
                Category category = shuffledCategories.get(i % shuffledCategories.size());
                
                // Get products for this category
                List<ProductInfo> productsForCategory = categoryProducts.get(category.getName());
                if (productsForCategory == null || productsForCategory.isEmpty()) {
                    continue;
                }
                
                // Select a random product from this category
                ProductInfo productInfo = productsForCategory.get(random.nextInt(productsForCategory.size()));
                
                Product product = new Product();
                product.setName(productInfo.getName());
                product.setDescription(productInfo.getDescription());
                product.setPrice(BigDecimal.valueOf(productInfo.getMinPrice() + 
                                random.nextDouble() * (productInfo.getMaxPrice() - productInfo.getMinPrice()))
                                .setScale(2, RoundingMode.HALF_UP));
                product.setQuantity(10 + random.nextInt(91));
                product.setProducer(producer);
                product.setImageUrl(getProductImageUrl(category.getName(), productInfo.getName()));
                
                // Assign the primary category
                Set<Category> productCategories = new HashSet<>();
                productCategories.add(category);
                
                // Possibly add a secondary category that makes sense for this product
                if (random.nextBoolean()) {
                    // Define compatible category groups
                    Map<String, List<String>> compatibleCategories = new HashMap<>();
                    compatibleCategories.put("Fresh Produce", Arrays.asList("Prepared Foods", "Snacks"));
                    compatibleCategories.put("Meat & Poultry", Arrays.asList("Prepared Foods"));
                    compatibleCategories.put("Seafood", Arrays.asList("Prepared Foods"));
                    compatibleCategories.put("Dairy & Eggs", Arrays.asList("Baked Goods", "Prepared Foods"));
                    compatibleCategories.put("Baked Goods", Arrays.asList("Snacks", "Specialty Foods"));
                    compatibleCategories.put("Specialty Foods", Arrays.asList("Snacks", "Baked Goods"));
                    compatibleCategories.put("Beverages", Arrays.asList("Specialty Foods"));
                    compatibleCategories.put("Prepared Foods", Arrays.asList("Fresh Produce", "Meat & Poultry", "Seafood", "Dairy & Eggs"));
                    compatibleCategories.put("Snacks", Arrays.asList("Fresh Produce", "Baked Goods", "Specialty Foods"));
                    
                    // Get compatible categories for this product's primary category
                    List<String> compatibleCategoryNames = compatibleCategories.get(category.getName());
                    if (compatibleCategoryNames != null && !compatibleCategoryNames.isEmpty()) {
                        // Find a compatible category from the categories list
                        List<Category> compatibleCategoryObjects = categories.stream()
                            .filter(c -> compatibleCategoryNames.contains(c.getName()))
                            .collect(Collectors.toList());
                        
                        if (!compatibleCategoryObjects.isEmpty()) {
                            Category secondaryCategory = compatibleCategoryObjects.get(random.nextInt(compatibleCategoryObjects.size()));
                            productCategories.add(secondaryCategory);
                        }
                    }
                }
                product.setCategories(productCategories);

                // Assign random status with realistic distribution
                int statusRoll = random.nextInt(10);
                if (statusRoll < 6) { // 60% approved
                    product.setStatus(ProductStatus.APPROVED);
                } else if (statusRoll < 8) { // 20% pending
                    product.setStatus(ProductStatus.PENDING);
                } else { // 20% declined
                    product.setStatus(ProductStatus.DECLINED);
                    product.setDeclineReason(declineReasons[random.nextInt(declineReasons.length)]);
                }

                products.add(productRepository.save(product));
                
                // Remove the product from the list to avoid duplicates
                productsForCategory.remove(productInfo);
                if (productsForCategory.isEmpty()) {
                    productsForCategory.addAll(createProductsForCategory(category.getName()));
                }
            }
        }
        return products;
    }
    
    /**
     * Creates a catalog of products organized by category
     */
    private Map<String, List<ProductInfo>> createProductCatalog() {
        Map<String, List<ProductInfo>> catalog = new HashMap<>();
        
        // Fresh Produce
        catalog.put("Fresh Produce", createProductsForCategory("Fresh Produce"));
        
        // Meat & Poultry
        catalog.put("Meat & Poultry", createProductsForCategory("Meat & Poultry"));
        
        // Seafood
        catalog.put("Seafood", createProductsForCategory("Seafood"));
        
        // Dairy & Eggs
        catalog.put("Dairy & Eggs", createProductsForCategory("Dairy & Eggs"));
        
        // Baked Goods
        catalog.put("Baked Goods", createProductsForCategory("Baked Goods"));
        
        // Specialty Foods
        catalog.put("Specialty Foods", createProductsForCategory("Specialty Foods"));
        
        // Beverages
        catalog.put("Beverages", createProductsForCategory("Beverages"));
        
        // Prepared Foods
        catalog.put("Prepared Foods", createProductsForCategory("Prepared Foods"));
        
        // Snacks
        catalog.put("Snacks", createProductsForCategory("Snacks"));
        
        return catalog;
    }
    
    /**
     * Creates a list of products for a specific category
     */
    private List<ProductInfo> createProductsForCategory(String category) {
        List<ProductInfo> products = new ArrayList<>();
        
        switch (category) {
            case "Fresh Produce":
                products.add(new ProductInfo("Organic Heirloom Tomatoes", 
                    "Fresh, locally grown heirloom tomatoes in various colors. Perfect for salads and cooking.", 3.99, 6.99));
                products.add(new ProductInfo("Baby Spinach", 
                    "Tender organic baby spinach leaves, freshly harvested and ready to eat.", 2.99, 4.99));
                products.add(new ProductInfo("Avocados", 
                    "Perfectly ripe Hass avocados, rich and creamy.", 1.50, 2.50));
                products.add(new ProductInfo("Mixed Bell Peppers", 
                    "Colorful mix of red, yellow, and green bell peppers.", 3.99, 5.99));
                products.add(new ProductInfo("Organic Carrots", 
                    "Sweet and crunchy organic carrots, freshly harvested.", 2.49, 3.99));
                products.add(new ProductInfo("Brussels Sprouts", 
                    "Fresh Brussels sprouts, perfect for roasting.", 3.49, 4.99));
                products.add(new ProductInfo("Red Onions", 
                    "Medium-sized red onions with a mild, sweet flavor.", 1.99, 2.99));
                products.add(new ProductInfo("Organic Kale", 
                    "Fresh organic kale, rich in nutrients and perfect for salads or cooking.", 2.99, 4.49));
                products.add(new ProductInfo("Zucchini", 
                    "Fresh zucchini, perfect for grilling or sautéing.", 1.99, 3.49));
                products.add(new ProductInfo("Organic Mushrooms", 
                    "Assorted organic mushrooms, freshly harvested.", 3.99, 6.99));
                break;
                
            case "Meat & Poultry":
                products.add(new ProductInfo("Grass-Fed Ground Beef", 
                    "Premium grass-fed ground beef from local pastures. Lean and flavorful.", 7.99, 12.99));
                products.add(new ProductInfo("Free-Range Chicken Breast", 
                    "Boneless, skinless chicken breasts from free-range chickens.", 8.99, 13.99));
                products.add(new ProductInfo("Heritage Pork Chops", 
                    "Thick-cut pork chops from heritage breed pigs raised on pasture.", 9.99, 15.99));
                products.add(new ProductInfo("Lamb Shoulder", 
                    "Tender lamb shoulder from grass-fed, locally raised lambs.", 12.99, 18.99));
                products.add(new ProductInfo("Organic Turkey Breast", 
                    "Sliced organic turkey breast, perfect for sandwiches.", 7.99, 11.99));
                products.add(new ProductInfo("Grass-Fed Ribeye Steak", 
                    "Premium grass-fed ribeye steak, well-marbled and flavorful.", 15.99, 24.99));
                products.add(new ProductInfo("Duck Breast", 
                    "Farm-raised duck breast with crispy skin.", 13.99, 19.99));
                products.add(new ProductInfo("Venison Sausage", 
                    "Lean venison sausage with herbs and spices.", 9.99, 14.99));
                break;
                
            case "Seafood":
                products.add(new ProductInfo("Wild-Caught Salmon", 
                    "Fresh wild-caught salmon fillets, rich in omega-3 fatty acids.", 14.99, 22.99));
                products.add(new ProductInfo("Fresh Shrimp", 
                    "Large, fresh shrimp, perfect for grilling or sautéing.", 12.99, 18.99));
                products.add(new ProductInfo("Local Trout", 
                    "Freshly caught local trout, cleaned and ready to cook.", 9.99, 15.99));
                products.add(new ProductInfo("Fresh Scallops", 
                    "Large, fresh sea scallops, sweet and tender.", 16.99, 24.99));
                products.add(new ProductInfo("Whole Red Snapper", 
                    "Fresh whole red snapper, perfect for grilling or baking.", 13.99, 19.99));
                products.add(new ProductInfo("Fresh Mussels", 
                    "Fresh mussels, cleaned and ready to cook.", 7.99, 11.99));
                products.add(new ProductInfo("Tuna Steaks", 
                    "Fresh tuna steaks, perfect for grilling or searing.", 15.99, 22.99));
                products.add(new ProductInfo("Smoked Salmon", 
                    "House-smoked salmon with dill and lemon.", 12.99, 18.99));
                break;
                
            case "Dairy & Eggs":
                products.add(new ProductInfo("Free-Range Farm Eggs", 
                    "Fresh eggs from free-range chickens, collected daily.", 4.99, 7.99));
                products.add(new ProductInfo("Organic Whole Milk", 
                    "Fresh organic whole milk from grass-fed cows.", 4.49, 6.99));
                products.add(new ProductInfo("Artisanal Cheddar Cheese", 
                    "Aged cheddar cheese made in small batches.", 8.99, 14.99));
                products.add(new ProductInfo("Goat Cheese Log", 
                    "Creamy goat cheese log with herbs.", 6.99, 10.99));
                products.add(new ProductInfo("Greek Yogurt", 
                    "Thick and creamy Greek yogurt made from whole milk.", 4.99, 7.99));
                products.add(new ProductInfo("Cultured Butter", 
                    "European-style cultured butter with sea salt.", 5.99, 9.99));
                products.add(new ProductInfo("Blue Cheese", 
                    "Rich and creamy blue cheese, aged to perfection.", 9.99, 15.99));
                products.add(new ProductInfo("Fresh Mozzarella", 
                    "Soft, fresh mozzarella cheese made daily.", 7.99, 11.99));
                products.add(new ProductInfo("Duck Eggs", 
                    "Large, rich duck eggs from free-range ducks.", 6.99, 9.99));
                break;
                
            case "Baked Goods":
                products.add(new ProductInfo("Artisanal Sourdough Bread", 
                    "Traditional sourdough bread made with organic flour and a 100-year-old starter.", 5.99, 8.99));
                products.add(new ProductInfo("Rustic Baguette", 
                    "Crusty French baguette made with organic flour.", 3.99, 5.99));
                products.add(new ProductInfo("Whole Grain Loaf", 
                    "Hearty whole grain bread with seeds and nuts.", 5.49, 7.99));
                products.add(new ProductInfo("Croissants", 
                    "Buttery, flaky croissants made from scratch.", 2.99, 4.99));
                products.add(new ProductInfo("Cinnamon Rolls", 
                    "Freshly baked cinnamon rolls with cream cheese frosting.", 3.99, 6.99));
                products.add(new ProductInfo("Blueberry Muffins", 
                    "Moist blueberry muffins made with fresh local berries.", 2.99, 4.99));
                products.add(new ProductInfo("Focaccia", 
                    "Italian focaccia bread with rosemary and sea salt.", 4.99, 7.99));
                products.add(new ProductInfo("Chocolate Chip Cookies", 
                    "Homemade chocolate chip cookies with premium chocolate.", 3.49, 5.99));
                break;
                
            case "Specialty Foods":
                products.add(new ProductInfo("Wildflower Raw Honey", 
                    "Pure, unprocessed honey from local wildflowers.", 8.99, 14.99));
                products.add(new ProductInfo("Truffle Oil", 
                    "Premium olive oil infused with black truffles.", 12.99, 19.99));
                products.add(new ProductInfo("Aged Balsamic Vinegar", 
                    "12-year aged balsamic vinegar from Modena, Italy.", 14.99, 24.99));
                products.add(new ProductInfo("Artisanal Pasta", 
                    "Handmade pasta using traditional methods and organic flour.", 6.99, 10.99));
                products.add(new ProductInfo("Organic Maple Syrup", 
                    "Pure organic maple syrup harvested locally.", 9.99, 16.99));
                products.add(new ProductInfo("Gourmet Olive Tapenade", 
                    "Handcrafted olive tapenade with capers and herbs.", 7.99, 12.99));
                products.add(new ProductInfo("Spice Blend Collection", 
                    "Set of handcrafted spice blends for various cuisines.", 11.99, 18.99));
                products.add(new ProductInfo("Artisanal Mustard", 
                    "Small-batch mustard with whole grain and herbs.", 5.99, 9.99));
                break;
                
            case "Beverages":
                products.add(new ProductInfo("Cold Brew Coffee", 
                    "Smooth cold brew coffee made with organic beans.", 4.99, 7.99));
                products.add(new ProductInfo("Kombucha", 
                    "Fermented tea with probiotics in various flavors.", 3.99, 6.99));
                products.add(new ProductInfo("Fresh Apple Cider", 
                    "Freshly pressed apple cider from local orchards.", 4.49, 7.49));
                products.add(new ProductInfo("Herbal Tea Blend", 
                    "Custom blend of organic herbs and flowers for tea.", 6.99, 11.99));
                products.add(new ProductInfo("Craft Root Beer", 
                    "Small-batch root beer made with natural ingredients.", 3.99, 5.99));
                products.add(new ProductInfo("Fresh Orange Juice", 
                    "Freshly squeezed orange juice from organic oranges.", 4.99, 7.99));
                products.add(new ProductInfo("Sparkling Lemonade", 
                    "Refreshing sparkling lemonade made with real lemons.", 3.49, 5.99));
                products.add(new ProductInfo("Ginger Beer", 
                    "Spicy ginger beer made with fresh ginger root.", 3.99, 6.49));
                break;
                
            case "Prepared Foods":
                products.add(new ProductInfo("Vegetable Lasagna", 
                    "Homemade vegetable lasagna with seasonal vegetables and ricotta cheese.", 9.99, 15.99));
                products.add(new ProductInfo("Chicken Pot Pie", 
                    "Traditional chicken pot pie with free-range chicken and vegetables.", 8.99, 14.99));
                products.add(new ProductInfo("Quinoa Salad", 
                    "Refreshing quinoa salad with vegetables and herbs.", 6.99, 10.99));
                products.add(new ProductInfo("Beef Stew", 
                    "Hearty beef stew with root vegetables and herbs.", 9.99, 15.99));
                products.add(new ProductInfo("Vegetable Curry", 
                    "Flavorful vegetable curry with coconut milk and spices.", 8.49, 13.99));
                products.add(new ProductInfo("Smoked Salmon Quiche", 
                    "Creamy quiche with smoked salmon and dill.", 7.99, 12.99));
                products.add(new ProductInfo("Roasted Vegetable Soup", 
                    "Hearty soup with roasted seasonal vegetables.", 5.99, 9.99));
                products.add(new ProductInfo("Stuffed Bell Peppers", 
                    "Bell peppers stuffed with quinoa, vegetables, and cheese.", 7.49, 11.99));
                break;
                
            case "Snacks":
                products.add(new ProductInfo("Mixed Nuts", 
                    "Assortment of roasted nuts with sea salt.", 7.99, 12.99));
                products.add(new ProductInfo("Dried Fruit Mix", 
                    "Mix of organic dried fruits without added sugar.", 6.99, 10.99));
                products.add(new ProductInfo("Kale Chips", 
                    "Crispy kale chips with various seasonings.", 4.99, 7.99));
                products.add(new ProductInfo("Artisanal Beef Jerky", 
                    "Handcrafted beef jerky with natural spices.", 8.99, 14.99));
                products.add(new ProductInfo("Cheese Straws", 
                    "Flaky cheese straws made with aged cheddar.", 5.99, 9.99));
                products.add(new ProductInfo("Hummus", 
                    "Creamy hummus with olive oil and spices.", 4.49, 7.99));
                products.add(new ProductInfo("Granola", 
                    "Homemade granola with nuts, seeds, and dried fruits.", 6.99, 11.99));
                products.add(new ProductInfo("Vegetable Chips", 
                    "Crispy chips made from various root vegetables.", 4.99, 8.99));
                break;
                
            default:
                // Add some generic products if category doesn't match
                products.add(new ProductInfo("Organic Product", 
                    "High-quality organic product from local producers.", 5.99, 12.99));
                products.add(new ProductInfo("Artisanal Food Item", 
                    "Handcrafted food item made with traditional methods.", 7.99, 15.99));
                break;
        }
        
        return products;
    }
    
    /**
     * Gets a realistic image URL for a product based on its category and name
     */
    private String getProductImageUrl(String category, String productName) {
        // Using Pexels API for reliable food images
        // We'll use a collection of curated food images that are guaranteed to exist
        
        Map<String, List<String>> categoryImages = new HashMap<>();
        
        // Fresh Produce images
        categoryImages.put("Fresh Produce", Arrays.asList(
            "https://images.pexels.com/photos/1656663/pexels-photo-1656663.jpeg", // tomatoes
            "https://images.pexels.com/photos/2255935/pexels-photo-2255935.jpeg", // spinach
            "https://images.pexels.com/photos/557659/pexels-photo-557659.jpeg",   // avocados
            "https://images.pexels.com/photos/1435904/pexels-photo-1435904.jpeg", // bell peppers
            "https://images.pexels.com/photos/143133/pexels-photo-143133.jpeg",   // carrots
            "https://images.pexels.com/photos/47347/broccoli-vegetable-food-healthy-47347.jpeg", // brussels sprouts
            "https://images.pexels.com/photos/4197444/pexels-photo-4197444.jpeg", // red onions
            "https://images.pexels.com/photos/977903/pexels-photo-977903.jpeg",   // kale
            "https://images.pexels.com/photos/128420/pexels-photo-128420.jpeg",   // zucchini
            "https://images.pexels.com/photos/2255511/pexels-photo-2255511.jpeg"  // mushrooms
        ));
        
        // Meat & Poultry images
        categoryImages.put("Meat & Poultry", Arrays.asList(
            "https://images.pexels.com/photos/618775/pexels-photo-618775.jpeg",   // ground beef
            "https://images.pexels.com/photos/616354/pexels-photo-616354.jpeg",   // chicken breast
            "https://images.pexels.com/photos/1927377/pexels-photo-1927377.jpeg", // pork chops
            "https://images.pexels.com/photos/8308126/pexels-photo-8308126.jpeg", // lamb
            "https://images.pexels.com/photos/5774154/pexels-photo-5774154.jpeg", // turkey
            "https://images.pexels.com/photos/1881336/pexels-photo-1881336.jpeg", // steak
            "https://images.pexels.com/photos/2233729/pexels-photo-2233729.jpeg", // duck
            "https://images.pexels.com/photos/4963958/pexels-photo-4963958.jpeg"  // sausage
        ));
        
        // Seafood images
        categoryImages.put("Seafood", Arrays.asList(
            "https://images.pexels.com/photos/3296395/pexels-photo-3296395.jpeg", // salmon
            "https://images.pexels.com/photos/566345/pexels-photo-566345.jpeg",   // shrimp
            "https://images.pexels.com/photos/2871757/pexels-photo-2871757.jpeg", // trout
            "https://images.pexels.com/photos/8969237/pexels-photo-8969237.jpeg", // scallops
            "https://images.pexels.com/photos/3296393/pexels-photo-3296393.jpeg", // red snapper
            "https://images.pexels.com/photos/8969192/pexels-photo-8969192.jpeg", // mussels
            "https://images.pexels.com/photos/8340968/pexels-photo-8340968.jpeg", // tuna
            "https://images.pexels.com/photos/8340891/pexels-photo-8340891.jpeg"  // smoked salmon
        ));
        
        // Dairy & Eggs images
        categoryImages.put("Dairy & Eggs", Arrays.asList(
            "https://images.pexels.com/photos/162712/egg-white-food-protein-162712.jpeg", // eggs
            "https://images.pexels.com/photos/248412/pexels-photo-248412.jpeg",   // milk
            "https://images.pexels.com/photos/773253/pexels-photo-773253.jpeg",   // cheddar
            "https://images.pexels.com/photos/4198370/pexels-photo-4198370.jpeg", // goat cheese
            "https://images.pexels.com/photos/373882/pexels-photo-373882.jpeg",   // yogurt
            "https://images.pexels.com/photos/236010/pexels-photo-236010.jpeg",   // butter
            "https://images.pexels.com/photos/821365/pexels-photo-821365.jpeg",   // blue cheese (changed to unique image)
            "https://images.pexels.com/photos/4198365/pexels-photo-4198365.jpeg", // mozzarella
            "https://images.pexels.com/photos/7129153/pexels-photo-7129153.jpeg"  // duck eggs
        ));
        
        // Baked Goods images
        categoryImages.put("Baked Goods", Arrays.asList(
            "https://images.pexels.com/photos/1387070/pexels-photo-1387070.jpeg", // sourdough
            "https://images.pexels.com/photos/1775043/pexels-photo-1775043.jpeg", // baguette
            "https://images.pexels.com/photos/1586947/pexels-photo-1586947.jpeg", // whole grain
            "https://images.pexels.com/photos/3892469/pexels-photo-3892469.jpeg", // croissants
            "https://images.pexels.com/photos/267308/pexels-photo-267308.jpeg",   // cinnamon rolls
            "https://images.pexels.com/photos/1657343/pexels-photo-1657343.jpeg", // muffins
            "https://images.pexels.com/photos/1566837/pexels-photo-1566837.jpeg", // focaccia
            "https://images.pexels.com/photos/230325/pexels-photo-230325.jpeg"    // cookies
        ));
        
        // Specialty Foods images
        categoryImages.put("Specialty Foods", Arrays.asList(
            "https://images.pexels.com/photos/162825/honey-sweet-syrup-organic-162825.jpeg", // honey (changed to unique image)
            "https://images.pexels.com/photos/33783/olive-oil-salad-dressing-cooking-olive.jpg", // truffle oil
            "https://images.pexels.com/photos/5946081/pexels-photo-5946081.jpeg", // balsamic
            "https://images.pexels.com/photos/1279330/pexels-photo-1279330.jpeg", // pasta
            "https://images.pexels.com/photos/162786/maple-syrup-sweet-syrup-canada-162786.jpeg", // maple syrup
            "https://images.pexels.com/photos/5946083/pexels-photo-5946083.jpeg", // tapenade
            "https://images.pexels.com/photos/6941080/pexels-photo-6941080.jpeg", // spices (changed to unique image)
            "https://images.pexels.com/photos/6941010/pexels-photo-6941010.jpeg"  // mustard
        ));
        
        // Beverages images
        categoryImages.put("Beverages", Arrays.asList(
            "https://images.pexels.com/photos/312418/pexels-photo-312418.jpeg",   // coffee
            "https://images.pexels.com/photos/5945640/pexels-photo-5945640.jpeg", // kombucha (changed to unique image)
            "https://images.pexels.com/photos/1536868/pexels-photo-1536868.jpeg", // cider
            "https://images.pexels.com/photos/1417945/pexels-photo-1417945.jpeg", // tea
            "https://images.pexels.com/photos/2983100/pexels-photo-2983100.jpeg", // root beer
            "https://images.pexels.com/photos/96974/pexels-photo-96974.jpeg",     // orange juice (changed to unique image)
            "https://images.pexels.com/photos/2109099/pexels-photo-2109099.jpeg", // lemonade
            "https://images.pexels.com/photos/1194030/pexels-photo-1194030.jpeg"  // ginger beer
        ));
        
        // Prepared Foods images
        categoryImages.put("Prepared Foods", Arrays.asList(
            "https://images.pexels.com/photos/5949902/pexels-photo-5949902.jpeg", // lasagna
            "https://images.pexels.com/photos/6210747/pexels-photo-6210747.jpeg", // pot pie
            "https://images.pexels.com/photos/1640777/pexels-photo-1640777.jpeg", // quinoa salad
            "https://images.pexels.com/photos/539451/pexels-photo-539451.jpeg",   // beef stew
            "https://images.pexels.com/photos/2474661/pexels-photo-2474661.jpeg", // curry
            "https://images.pexels.com/photos/6210959/pexels-photo-6210959.jpeg", // quiche
            "https://images.pexels.com/photos/1731535/pexels-photo-1731535.jpeg", // soup (changed to unique image)
            "https://images.pexels.com/photos/8969470/pexels-photo-8969470.jpeg"  // stuffed peppers (changed to unique image)
        ));
        
        // Snacks images
        categoryImages.put("Snacks", Arrays.asList(
            "https://images.pexels.com/photos/1310777/pexels-photo-1310777.jpeg", // mixed nuts
            "https://images.pexels.com/photos/1268122/pexels-photo-1268122.jpeg", // dried fruit
            "https://images.pexels.com/photos/5945599/pexels-photo-5945599.jpeg", // kale chips (changed to unique image)
            "https://images.pexels.com/photos/6941058/pexels-photo-6941058.jpeg", // beef jerky (changed to unique image)
            "https://images.pexels.com/photos/1373915/pexels-photo-1373915.jpeg", // cheese straws
            "https://images.pexels.com/photos/1618955/pexels-photo-1618955.jpeg", // hummus
            "https://images.pexels.com/photos/1099680/pexels-photo-1099680.jpeg", // granola
            "https://images.pexels.com/photos/1583884/pexels-photo-1583884.jpeg"  // vegetable chips
        ));
        
        // Get the list of images for this category
        List<String> images = categoryImages.get(category);
        if (images == null || images.isEmpty()) {
            // Default image if category not found
            return "https://images.pexels.com/photos/1660030/pexels-photo-1660030.jpeg";
        }
        
        // Create a map of product keywords to their corresponding image indices
        Map<String, Integer> keywordToImageIndex = new HashMap<>();
        
        // Fresh Produce
        keywordToImageIndex.put("tomato", 0);
        keywordToImageIndex.put("spinach", 1);
        keywordToImageIndex.put("avocado", 2);
        keywordToImageIndex.put("pepper", 3);
        keywordToImageIndex.put("carrot", 4);
        keywordToImageIndex.put("brussels", 5);
        keywordToImageIndex.put("onion", 6);
        keywordToImageIndex.put("kale", 7);
        keywordToImageIndex.put("zucchini", 8);
        keywordToImageIndex.put("mushroom", 9);
        
        // Meat & Poultry
        keywordToImageIndex.put("ground beef", 0);
        keywordToImageIndex.put("chicken", 1);
        keywordToImageIndex.put("pork", 2);
        keywordToImageIndex.put("lamb", 3);
        keywordToImageIndex.put("turkey", 4);
        keywordToImageIndex.put("steak", 5);
        keywordToImageIndex.put("duck", 6);
        keywordToImageIndex.put("sausage", 7);
        
        // Seafood
        keywordToImageIndex.put("salmon", 0);
        keywordToImageIndex.put("shrimp", 1);
        keywordToImageIndex.put("trout", 2);
        keywordToImageIndex.put("scallop", 3);
        keywordToImageIndex.put("snapper", 4);
        keywordToImageIndex.put("mussel", 5);
        keywordToImageIndex.put("tuna", 6);
        keywordToImageIndex.put("smoked salmon", 7);
        
        // Dairy & Eggs
        keywordToImageIndex.put("egg", 0);
        keywordToImageIndex.put("milk", 1);
        keywordToImageIndex.put("cheddar", 2);
        keywordToImageIndex.put("goat cheese", 3);
        keywordToImageIndex.put("yogurt", 4);
        keywordToImageIndex.put("butter", 5);
        keywordToImageIndex.put("blue cheese", 6);
        keywordToImageIndex.put("mozzarella", 7);
        keywordToImageIndex.put("duck egg", 8);
        
        // Baked Goods
        keywordToImageIndex.put("sourdough", 0);
        keywordToImageIndex.put("baguette", 1);
        keywordToImageIndex.put("whole grain", 2);
        keywordToImageIndex.put("croissant", 3);
        keywordToImageIndex.put("cinnamon roll", 4);
        keywordToImageIndex.put("muffin", 5);
        keywordToImageIndex.put("focaccia", 6);
        keywordToImageIndex.put("cookie", 7);
        
        // Specialty Foods
        keywordToImageIndex.put("honey", 0);
        keywordToImageIndex.put("truffle oil", 1);
        keywordToImageIndex.put("balsamic", 2);
        keywordToImageIndex.put("pasta", 3);
        keywordToImageIndex.put("maple syrup", 4);
        keywordToImageIndex.put("tapenade", 5);
        keywordToImageIndex.put("spice", 6);
        keywordToImageIndex.put("mustard", 7);
        
        // Beverages
        keywordToImageIndex.put("coffee", 0);
        keywordToImageIndex.put("kombucha", 1);
        keywordToImageIndex.put("cider", 2);
        keywordToImageIndex.put("tea", 3);
        keywordToImageIndex.put("root beer", 4);
        keywordToImageIndex.put("orange juice", 5);
        keywordToImageIndex.put("lemonade", 6);
        keywordToImageIndex.put("ginger beer", 7);
        
        // Prepared Foods
        keywordToImageIndex.put("lasagna", 0);
        keywordToImageIndex.put("pot pie", 1);
        keywordToImageIndex.put("quinoa", 2);
        keywordToImageIndex.put("stew", 3);
        keywordToImageIndex.put("curry", 4);
        keywordToImageIndex.put("quiche", 5);
        keywordToImageIndex.put("soup", 6);
        keywordToImageIndex.put("stuffed pepper", 7);
        
        // Snacks
        keywordToImageIndex.put("nut", 0);
        keywordToImageIndex.put("dried fruit", 1);
        keywordToImageIndex.put("kale chip", 2);
        keywordToImageIndex.put("jerky", 3);
        keywordToImageIndex.put("cheese straw", 4);
        keywordToImageIndex.put("hummus", 5);
        keywordToImageIndex.put("granola", 6);
        keywordToImageIndex.put("vegetable chip", 7);
        
        // Find the best matching image based on product name
        String productNameLower = productName.toLowerCase();
        
        // Try to find a direct match using the keyword map
        for (Map.Entry<String, Integer> entry : keywordToImageIndex.entrySet()) {
            if (productNameLower.contains(entry.getKey()) && 
                entry.getValue() < images.size()) {
                return images.get(entry.getValue());
            }
        }
        
        // If no direct match, use a deterministic but random image from the category
        // Using the hash code of the product name ensures the same product always gets the same image
        return images.get(Math.abs(productName.hashCode()) % images.size());
    }
    
    /**
     * Inner class to hold product information
     */
    private static class ProductInfo {
        private final String name;
        private final String description;
        private final double minPrice;
        private final double maxPrice;
        
        public ProductInfo(String name, String description, double minPrice, double maxPrice) {
            this.name = name;
            this.description = description;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public double getMinPrice() {
            return minPrice;
        }
        
        public double getMaxPrice() {
            return maxPrice;
        }
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

        log.info("Starting to seed orders...");
        
        for (User customer : customers) {
            // Producer 1 - High volume in December
            createOrderForProducer(customer, products, "producer1",
                    DECEMBER_DATES, null, 20, decemberRevenue);

            // Producer 2 - Medium volume in December
            createOrderForProducer(customer, products, "producer2",
                    DECEMBER_DATES, null, 15, decemberRevenue);

            // Producer 3 - Low volume in December
            createOrderForProducer(customer, products, "producer3",
                    DECEMBER_DATES, null, 10, decemberRevenue);

            // Producer 4 - Very low volume in December
            createOrderForProducer(customer, products, "producer4",
                    DECEMBER_DATES, null, 5, decemberRevenue);

            // Producer 5 - Minimal volume in December
            createOrderForProducer(customer, products, "producer5",
                    DECEMBER_DATES, null, 2, decemberRevenue);

            // January orders with different growth patterns
            // Producer 1 - Moderate growth (20%)
            createOrderForProducer(customer, products, "producer1",
                    JANUARY_DATES, null, 24, januaryRevenue);

            // Producer 2 - High growth (50%)
            createOrderForProducer(customer, products, "producer2",
                    JANUARY_DATES, null, 23, januaryRevenue);

            // Producer 3 - Negative growth (-20%)
            createOrderForProducer(customer, products, "producer3",
                    JANUARY_DATES, null, 8, januaryRevenue);

            // Producer 4 - Explosive growth (100%)
            createOrderForProducer(customer, products, "producer4",
                    JANUARY_DATES, null, 10, januaryRevenue);

            // Producer 5 - From zero to hero
            createOrderForProducer(customer, products, "producer5",
                    JANUARY_DATES, null, 8, januaryRevenue);
        }

        // Log revenue and growth rates for verification
        log.info("\nProducer Performance Report:");
        log.info("============================");

        decemberRevenue.forEach((producer, decRevenue) -> {
            BigDecimal janRevenue = januaryRevenue.getOrDefault(producer, BigDecimal.ZERO);
            double growth = decRevenue.equals(BigDecimal.ZERO)
                    ? (janRevenue.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0)
                    : janRevenue.subtract(decRevenue)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(decRevenue, 2, RoundingMode.HALF_UP)
                            .doubleValue();

            log.info("{}:", producer);
            log.info("  December Revenue: ${}", decRevenue);
            log.info("  January Revenue: ${}", januaryRevenue.getOrDefault(producer, BigDecimal.ZERO));
            log.info("  Growth Rate: {}%\n", growth);
        });

        // Handle producer5 separately (infinite growth case)
        if (!decemberRevenue.containsKey("producer5") && januaryRevenue.containsKey("producer5")) {
            log.info("producer5:");
            log.info("  December Revenue: $0.00");
            log.info("  January Revenue: ${}", januaryRevenue.get("producer5"));
            log.info("  Growth Rate: 100.00%\n");
        }
    }

    private void createOrderForProducer(User customer, List<Product> products, String producerUsername,
            LocalDateTime[] dateRange, LocalDateTime endDate, int orderCount, Map<String, BigDecimal> revenueMap) {
        try {
            List<Product> producerProducts = products.stream()
                    .filter(p -> p.getProducer().getUsername().equals(producerUsername) && 
                                p.getStatus() == ProductStatus.APPROVED)
                    .collect(Collectors.toList());

            if (producerProducts.isEmpty()) {
                log.warn("No approved products found for producer: {}", producerUsername);
                return;
            }

            BigDecimal producerRevenue = BigDecimal.ZERO;

            for (int i = 0; i < orderCount; i++) {
                // Generate a unique accessToken for this order using TokenService
                String accessToken = tokenService.createCheckoutToken(customer.getEmail());
                
                Order order = new Order();
                order.setCustomer(customer);
                order.setShippingAddress(customer.getFirstname() + "'s Address, " + 
                                       random.nextInt(1000) + " Main St, City");
                order.setPhoneNumber("+1" + (random.nextInt(900) + 100) + 
                                   (random.nextInt(900) + 100) + 
                                   (random.nextInt(9000) + 1000));
                order.setPaymentMethod(PaymentMethod.CARD);
                // Set the accessToken
                order.setAccessToken(accessToken);
                // Set expiration date for the token
                order.setExpiresAt(LocalDateTime.now().plusDays(7));

                // Randomize order status with realistic distribution
                int statusRoll = random.nextInt(100);
                if (statusRoll < 60) { // 60% delivered
                    order.setStatus(OrderStatus.DELIVERED);
                } else if (statusRoll < 75) { // 15% shipped
                    order.setStatus(OrderStatus.SHIPPED);
                } else if (statusRoll < 85) { // 10% processing
                    order.setStatus(OrderStatus.PROCESSING);
                } else if (statusRoll < 90) { // 5% pending payment
                    order.setStatus(OrderStatus.PENDING_PAYMENT);
                } else if (statusRoll < 95) { // 5% payment failed
                    order.setStatus(OrderStatus.PAYMENT_FAILED);
                } else { // 5% cancelled
                    order.setStatus(OrderStatus.CANCELLED);
                }

                // Set order date from the provided date range
                LocalDateTime orderDate = dateRange[random.nextInt(dateRange.length)];
                order.setOrderDate(orderDate);

                // Create order items (1-4 items per order)
                Set<OrderItem> orderItems = new HashSet<>();
                int itemCount = 1 + random.nextInt(4);
                
                // Shuffle the products to get a random selection
                List<Product> shuffledProducts = new ArrayList<>(producerProducts);
                Collections.shuffle(shuffledProducts);
                
                // Take the first itemCount products or as many as available
                int actualItemCount = Math.min(itemCount, shuffledProducts.size());
                
                for (int j = 0; j < actualItemCount; j++) {
                    Product product = shuffledProducts.get(j);
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setProduct(product);
                    orderItem.setQuantity(1 + random.nextInt(3));
                    orderItem.setPrice(product.getPrice());
                    orderItems.add(orderItem);
                }

                BigDecimal totalPrice = orderItems.stream()
                        .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                order.setTotalPrice(totalPrice);

                // Create payment only for appropriate order statuses
                if (order.getStatus() != OrderStatus.PENDING_PAYMENT && 
                    order.getStatus() != OrderStatus.PAYMENT_FAILED) {
                    Payment payment = new Payment();
                    payment.setPaymentMethod(PaymentMethod.CARD);
                    payment.setPaymentStatus(PaymentStatus.COMPLETED);
                    payment.setTransactionId("TR" + System.currentTimeMillis() + random.nextInt(1000));
                    payment.setAmount(totalPrice);
                    payment.setCreatedAt(orderDate);
                    Payment savedPayment = paymentRepository.save(payment);
                    order.setPayment(savedPayment);
                }

                List<OrderItem> savedItems = new ArrayList<>();
                for (OrderItem item : orderItems) {
                    item.setOrder(order);
                    savedItems.add(item);
                }
                order.setItems(savedItems);

                Order savedOrder = orderRepository.save(order);
                if (order.getPayment() != null) {
                    order.getPayment().setOrderId(savedOrder.getOrderId());
                    paymentRepository.save(order.getPayment());
                    producerRevenue = producerRevenue.add(totalPrice);
                }
            }

            revenueMap.merge(producerUsername, producerRevenue, BigDecimal::add);

        } catch (Exception e) {
            log.error("Error creating orders for {}: {}", producerUsername, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Seeds reviews for products, creating both verified and unverified reviews.
     * Verified reviews are created for products that customers have ordered.
     * Unverified reviews are created for popular products that customers haven't ordered.
     */
    @Transactional
    private void seedReviews() {
        log.info("Seeding reviews...");
        
        // Get all delivered orders to create verified reviews
        List<Order> deliveredOrders = orderRepository.findByStatus(OrderStatus.DELIVERED);
        
        // Map to track which customers have ordered which products
        Map<Long, Set<Long>> customerOrderedProducts = new HashMap<>();
        
        // Utiliser une approche plus directe pour éviter les problèmes de lazy loading
        for (Order order : deliveredOrders) {
            Long customerId = order.getCustomer().getUserId();
            
            // Initialize set if not present
            customerOrderedProducts.putIfAbsent(customerId, new HashSet<>());
            
            // Charger les items de commande de manière explicite avec la méthode dédiée
            List<OrderItem> orderItems = orderRepository.findOrderItemsByOrderId(order.getOrderId());
            
            // Add all products from this order to the customer's set
            for (OrderItem item : orderItems) {
                customerOrderedProducts.get(customerId).add(item.getProduct().getProductId());
            }
        }
        
        // Create verified reviews (for products customers have ordered)
        int verifiedReviewCount = 0;
        Random random = new Random();
        
        for (Map.Entry<Long, Set<Long>> entry : customerOrderedProducts.entrySet()) {
            Long customerId = entry.getKey();
            Set<Long> productIds = entry.getValue();
            
            // Create reviews for approximately 70% of the products ordered by each customer
            for (Long productId : productIds) {
                if (random.nextDouble() <= 0.7) { // 70% chance to create a review
                    Review review = createReview(customerId, productId, true);
                    reviewRepository.save(review);
                    verifiedReviewCount++;
                }
            }
        }
        
        // Create unverified reviews for popular products
        int unverifiedReviewCount = 0;
        List<Product> popularProducts = productRepository.findByStatus(ProductStatus.APPROVED);
            
        Collections.shuffle(popularProducts);
        
        // Take top 30% of products for unverified reviews
        int unverifiedReviewProductCount = Math.max(5, popularProducts.size() / 3);
        
        for (int i = 0; i < unverifiedReviewProductCount && i < popularProducts.size(); i++) {
            Product product = popularProducts.get(i);
            
            // 1-3 unverified reviews per product
            int numUnverifiedReviews = 1 + random.nextInt(3);
            
            for (int j = 0; j < numUnverifiedReviews; j++) {
                // Pick a random customer who hasn't ordered this product
                List<User> potentialReviewers = userRepository.findByRole(Role.CUSTOMER).stream()
                    .filter(c -> !customerOrderedProducts.containsKey(c.getUserId()) || 
                                !customerOrderedProducts.get(c.getUserId()).contains(product.getProductId()))
                    .collect(Collectors.toList());
                    
                if (potentialReviewers.isEmpty()) {
                    continue;
                }
                
                User customer = potentialReviewers.get(random.nextInt(potentialReviewers.size()));
                
                // Skip if already reviewed by this customer
                if (reviewRepository.existsByProductAndCustomer(product.getProductId(), customer.getUserId())) {
                    continue;
                }
                
                // Create an unverified review
                Review review = createReview(customer.getUserId(), product.getProductId(), false);
                reviewRepository.save(review);
                unverifiedReviewCount++;
            }
        }
        
        log.info("Seeded {} reviews ({} verified, {} unverified)", 
                verifiedReviewCount + unverifiedReviewCount, verifiedReviewCount, unverifiedReviewCount);
    }
    
    /**
     * Creates a review with appropriate rating and comment.
     * 
     * @param customerId The ID of the customer creating the review
     * @param productId The ID of the product being reviewed
     * @param isVerified Whether this is a verified purchase review
     * @return A new Review entity
     */
    private Review createReview(Long customerId, Long productId, boolean isVerified) {
        Random random = new Random();
        User customer = userRepository.findById(customerId).orElseThrow();
        Product product = productRepository.findById(productId).orElseThrow();
        
        // Generate a weighted random rating (verified purchases tend to have higher ratings)
        int rating;
        if (isVerified) {
            // Weighted distribution favoring 4-5 stars for verified purchases
            double rand = random.nextDouble();
            if (rand < 0.6) rating = 5;      // 60% chance of 5 stars
            else if (rand < 0.85) rating = 4; // 25% chance of 4 stars
            else if (rand < 0.95) rating = 3; // 10% chance of 3 stars
            else if (rand < 0.98) rating = 2; // 3% chance of 2 stars
            else rating = 1;                  // 2% chance of 1 star
        } else {
            // More uniform distribution for unverified purchases
            double rand = random.nextDouble();
            if (rand < 0.4) rating = 5;      // 40% chance of 5 stars
            else if (rand < 0.65) rating = 4; // 25% chance of 4 stars
            else if (rand < 0.85) rating = 3; // 20% chance of 3 stars
            else if (rand < 0.95) rating = 2; // 10% chance of 2 stars
            else rating = 1;                  // 5% chance of 1 star
        }
        
        // Generate a comment based on the rating
        String comment = generateComment(rating, product.getName());
        
        // Create the review
        Review review = new Review();
        review.setCustomer(customer);
        review.setProduct(product);
        review.setRating(rating);
        review.setComment(comment);
        review.setStatus(isVerified ? ReviewStatus.APPROVED : ReviewStatus.PENDING);
        review.setVerifiedPurchase(isVerified);
        
        // Set review date
        LocalDateTime reviewDate;
        if (isVerified) {
            // For verified reviews, set date after the most recent order date
            LocalDateTime orderDate = orderRepository.findMostRecentOrderDateForCustomerAndProduct(
                    customerId, productId);
            
            // Add 1-14 days to the order date
            int daysToAdd = random.nextInt(14) + 1;
            reviewDate = orderDate.plusDays(daysToAdd);
            
            // Ensure review date is not in the future
            if (reviewDate.isAfter(LocalDateTime.now())) {
                reviewDate = LocalDateTime.now().minusDays(random.nextInt(3));
            }
        } else {
            // For unverified reviews, use a random date in the last 3 months
            int daysAgo = random.nextInt(90) + 1;
            reviewDate = LocalDateTime.now().minusDays(daysAgo);
        }
        
        review.setCreatedAt(reviewDate);
        
        return review;
    }
    
    /**
     * Generates a realistic comment based on the rating and product name.
     * 
     * @param rating The star rating (1-5)
     * @param productName The name of the product
     * @return A comment appropriate for the rating
     */
    private String generateComment(int rating, String productName) {
        Random random = new Random();
        List<String> comments;
        
        switch (rating) {
            case 5:
                comments = Arrays.asList(
                    "Absolutely love this " + productName + "! Best I've ever had.",
                    "Exceptional quality! This " + productName + " exceeded all my expectations.",
                    "Wow! This " + productName + " is incredible. Will definitely buy again.",
                    "Perfect in every way. This " + productName + " is a game-changer.",
                    "Couldn't be happier with this " + productName + ". Fresh and delicious!",
                    "This " + productName + " is outstanding. Highly recommend to everyone.",
                    "The " + productName + " was fresh and of superior quality. 5 stars!",
                    "Fantastic product! The " + productName + " was perfect."
                );
                break;
            case 4:
                comments = Arrays.asList(
                    "Really good " + productName + ". Very satisfied with my purchase.",
                    "Great quality " + productName + ". Just a tiny bit pricey.",
                    "Very fresh " + productName + ". Would buy again.",
                    "The " + productName + " was excellent. Just a small issue with packaging.",
                    "Solid product. The " + productName + " was better than most I've tried.",
                    "Very happy with this " + productName + ". Almost perfect!",
                    "Good " + productName + ", delivered promptly and in good condition.",
                    "The " + productName + " was very good quality. Will order again."
                );
                break;
            case 3:
                comments = Arrays.asList(
                    "Decent " + productName + ". Nothing special but satisfactory.",
                    "The " + productName + " was okay. Expected a bit better for the price.",
                    "Average quality " + productName + ". Might try a different variety next time.",
                    "Not bad, but not great either. The " + productName + " was just okay.",
                    "Acceptable " + productName + " but I've had better elsewhere.",
                    "Middle of the road " + productName + ". Some good aspects, some not so good.",
                    "The " + productName + " was fresh enough, but lacked flavor.",
                    "Mediocre " + productName + ". Probably wouldn't order again."
                );
                break;
            case 2:
                comments = Arrays.asList(
                    "Disappointed with this " + productName + ". Below average quality.",
                    "The " + productName + " wasn't very fresh. Wouldn't recommend.",
                    "Not worth the price. The " + productName + " was subpar.",
                    "Had issues with this " + productName + ". Wouldn't buy again.",
                    "The " + productName + " didn't meet my expectations at all.",
                    "Poor quality " + productName + ". Expected much better.",
                    "Wouldn't recommend this " + productName + ". Too many issues.",
                    "The " + productName + " arrived in poor condition. Very disappointing."
                );
                break;
            case 1:
                comments = Arrays.asList(
                    "Terrible experience with this " + productName + ". Avoid at all costs!",
                    "The worst " + productName + " I've ever purchased. Complete waste of money.",
                    "Extremely disappointed. The " + productName + " was inedible.",
                    "Awful quality. The " + productName + " was nothing like described.",
                    "Would give zero stars if possible. The " + productName + " was terrible.",
                    "Never buying this " + productName + " again. Horrible quality.",
                    "The " + productName + " was spoiled when it arrived. Terrible experience.",
                    "Completely unsatisfied with this " + productName + ". Don't waste your money."
                );
                break;
            default:
                comments = Arrays.asList("No comment provided for this " + productName + ".");
        }
        
        return comments.get(random.nextInt(comments.size()));
    }
}