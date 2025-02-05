    create database localmarket;
    use localmarket;
    -- Table: User
    CREATE TABLE User (
        userId BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(255) NOT NULL,
        email VARCHAR(255) NOT NULL UNIQUE,
        passwordHash VARCHAR(255) NOT NULL,
        firstname VARCHAR(255) NOT NULL,
        lastname VARCHAR(255) NOT NULL,
        role  ENUM('CUSTOMER', 'PRODUCER', 'ADMIN') NOT NULL,
        createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
        updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        tokenVersion INT DEFAULT 0,
        lastLogin DATETIME
    );

    -- Table: Product
    CREATE TABLE Product (
        productId BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        producerId BIGINT NOT NULL,
        name VARCHAR(255) NOT NULL,
        description TEXT,
        price DECIMAL(10, 2) NOT NULL,
        quantity INT NOT NULL,
        imageUrl VARCHAR(2083),
        createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
        updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        status ENUM('PENDING', 'APPROVED', 'DECLINED') DEFAULT 'PENDING',
        declineReason TEXT,
        FOREIGN KEY (producerId) REFERENCES User(userId) ON DELETE CASCADE
    );

    -- Table: Category
    CREATE TABLE Category (
        categoryId BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(255) NOT NULL UNIQUE
    );

    -- Table: ProductCategory
    CREATE TABLE ProductCategory (
        productId BIGINT NOT NULL,
        categoryId BIGINT NOT NULL,
        PRIMARY KEY (productId, categoryId),
        FOREIGN KEY (productId) REFERENCES Product(productId) ON DELETE CASCADE,
        FOREIGN KEY (categoryId) REFERENCES Category(categoryId) ON DELETE CASCADE
    );

    -- Table: PaymentInfo
    CREATE TABLE PaymentInfo (
        paymentId BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        orderId BIGINT,
        paymentMethod ENUM('CARD', 'GOOGLE_PAY', 'APPLE_PAY', 'CASH', 'BITCOIN') NOT NULL,
        paymentStatus ENUM('PENDING', 'COMPLETED', 'FAILED') NOT NULL,
        transactionId VARCHAR(255),
        amount DECIMAL(10, 2) NOT NULL,
        createdAt DATETIME DEFAULT CURRENT_TIMESTAMP
    );

-- Table: Order
CREATE TABLE `Order` (
    orderId BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    customerId BIGINT,
    guestEmail VARCHAR(255),
    accessToken VARCHAR(255),
    expiresAt DATETIME,
    shippingAddress TEXT NOT NULL,
    phoneNumber VARCHAR(20) NOT NULL,
    paymentMethod ENUM('CARD', 'BITCOIN') NOT NULL,
    orderDate DATETIME DEFAULT CURRENT_TIMESTAMP,
    status ENUM(
        'PENDING_PAYMENT',
        'PAYMENT_FAILED',
        'PAYMENT_COMPLETED',
        'PROCESSING',
        'SHIPPED',
        'DELIVERED',
        'CANCELLED',
        'RETURNED'
    ) DEFAULT 'PENDING_PAYMENT',
    totalPrice DECIMAL(10, 2) NOT NULL,
    paymentId BIGINT,
    FOREIGN KEY (customerId) REFERENCES User(userId) ON DELETE SET NULL,
    FOREIGN KEY (paymentId) REFERENCES PaymentInfo(paymentId)
);

    -- Table: OrderItem
    CREATE TABLE OrderItem (
        orderItemId BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        orderId BIGINT NOT NULL,
        productId BIGINT NOT NULL,
        quantity INT NOT NULL,
        price DECIMAL(10, 2) NOT NULL,
        FOREIGN KEY (orderId) REFERENCES `Order`(orderId) ON DELETE CASCADE,
        FOREIGN KEY (productId) REFERENCES Product(productId) ON DELETE CASCADE
    );

    -- Table: Review
    CREATE TABLE Review (
        reviewId BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        productId BIGINT NOT NULL,
        customerId BIGINT NOT NULL,
        rating ENUM('0', '1', '2', '3', '4' ,'5') NOT NULL,
        comment TEXT,
        status ENUM('PENDING', 'APPROVED', 'DECLINED') DEFAULT 'PENDING',
        verifiedPurchase BOOLEAN DEFAULT FALSE,
        createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (productId) REFERENCES Product(productId) ON DELETE CASCADE,
        FOREIGN KEY (customerId) REFERENCES User(userId) ON DELETE CASCADE
);

    -- Table: ProducerApplication
    CREATE TABLE ProducerApplication (
        applicationId BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        customerId BIGINT NOT NULL,
        businessName VARCHAR(255),
        businessDescription VARCHAR(500) NOT NULL,
        category_ids TEXT NOT NULL,
        customCategory VARCHAR(255),
        businessAddress TEXT NOT NULL,
        cityRegion VARCHAR(255) NOT NULL,
        yearsOfExperience INT,
        websiteOrSocialLink VARCHAR(255),
        messageToAdmin TEXT,
        status ENUM('PENDING', 'APPROVED', 'DECLINED') DEFAULT 'PENDING',
        declineReason TEXT,
        createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
        updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (customerId) REFERENCES User(userId) ON DELETE CASCADE
    );

-- Table: StockReservation
CREATE TABLE StockReservation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    productId BIGINT NOT NULL,
    orderId BIGINT NOT NULL,
    quantity INT NOT NULL,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    expiresAt DATETIME NOT NULL,
    FOREIGN KEY (productId) REFERENCES Product(productId) ON DELETE CASCADE,
    FOREIGN KEY (orderId) REFERENCES `Order`(orderId) ON DELETE CASCADE
);

-- Table: AccessToken
CREATE TABLE AccessToken (
    token VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    expiresAt DATETIME NOT NULL,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_expires (expiresAt)
);

-- Update PaymentInfo to add Order foreign key
ALTER TABLE PaymentInfo
ADD FOREIGN KEY (orderId) REFERENCES `Order`(orderId) ON DELETE CASCADE;

-- Add indexes for analytics queries
ALTER TABLE User ADD INDEX idx_created_at (createdAt);
ALTER TABLE User ADD INDEX idx_role_created_at (role, createdAt);
ALTER TABLE `Order` ADD INDEX idx_order_date (orderDate);
ALTER TABLE `Order` ADD INDEX idx_status_date (status, orderDate);
ALTER TABLE PaymentInfo ADD INDEX idx_created_at (createdAt);
ALTER TABLE PaymentInfo ADD INDEX idx_status_created (paymentStatus, createdAt);

-- Add index for expired reservations cleanup
ALTER TABLE StockReservation ADD INDEX idx_expires_at (expiresAt);

-- Table: Coupon
CREATE TABLE Coupon (
    couponId BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    discountType ENUM('PERCENTAGE', 'FIXED_AMOUNT') NOT NULL,
    discountValue DECIMAL(10, 2) NOT NULL,
    minimumPurchaseAmount DECIMAL(10, 2),
    maximumDiscountAmount DECIMAL(10, 2),
    validFrom DATETIME NOT NULL,
    validUntil DATETIME NOT NULL,
    usageLimit INT,
    timesUsed INT DEFAULT 0,
    isActive BOOLEAN DEFAULT TRUE,
    INDEX idx_code (code),
    INDEX idx_valid_dates (validFrom, validUntil)
);
