create database localmarket;
use localmarket;
-- Table: User
CREATE TABLE User (
    userId BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    passwordHash VARCHAR(255) NOT NULL,
    role  ENUM('CUSTOMER', 'PRODUCER', 'ADMIN') NOT NULL,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tokenVersion INT DEFAULT 0
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
    shippingAddress TEXT NOT NULL,
    phoneNumber VARCHAR(20) NOT NULL,
    orderDate DATETIME DEFAULT CURRENT_TIMESTAMP,
    status ENUM('PENDING', 'ACCEPTED', 'DECLINED', 'COMPLETED') DEFAULT 'PENDING',
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
    rating INT NOT NULL,
    comment TEXT,
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
    categories VARCHAR(255) NOT NULL,
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

-- Update PaymentInfo to add Order foreign key
ALTER TABLE PaymentInfo
ADD FOREIGN KEY (orderId) REFERENCES `Order`(orderId) ON DELETE CASCADE;
