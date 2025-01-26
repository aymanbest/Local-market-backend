# Local Market API Documentation (NEEDS UPDATING)

## Authentication Endpoints
`/api/auth`

| Method | Endpoint    | Description                | Auth Required | Body                                    |
|--------|------------|----------------------------|---------------|----------------------------------------|
| POST   | /register  | Register a new user        | No           | `RegisterRequest` (email, password)     |
| POST   | /login     | Login to get access token  | No           | `AuthRequest` (email, password)         |
| POST   | /logout    | Logout and blacklist token | Yes          | None                                   |

## Products Endpoints
`/api/products`

| Method | Endpoint           | Description              | Auth Required | Role     | Request Type        | Body/Parameters    |
|--------|-------------------|--------------------------|---------------|----------|--------------------|--------------------|
| POST   | /                 | Create new product       | Yes          | Producer | Multipart Form     | See example below  |
| GET    | /                 | Get all products         | No           | -        | -                  | None               |
| GET    | /{id}            | Get product by ID        | No           | -        | -                  | None               |
| PUT    | /{id}            | Update product           | Yes          | Producer | Multipart Form     | See example below  |
| DELETE | /{id}            | Delete product           | Yes          | Producer | -                  | None               |
| GET    | /category/{id}   | Get products by category | No           | -        | -                  | None               |
| GET    | /images/{filename}| Get product image       | No           | -        | -                  | None               |

### Product Create/Update Request Example
```http
POST /api/products
Content-Type: multipart/form-data

name: "Fresh Apples"
description: "Organic fresh apples"
price: "2.99"
quantity: "100"
categoryIds: "1,2,3"
imageUrl: "https://example.com/apple.jpg" (optional)
image: [file] (optional, but either imageUrl or image must be provided)
```

[Rest of existing documentation remains unchanged...]

## Request/Response Types

### ProductRequest
```json
{
  "name": "string",
  "description": "string",
  "price": "decimal",
  "quantity": "integer",
  "categoryIds": "string (comma-separated)",
  "imageUrl": "string (optional)"
}
```

### ProductResponse
```json
{
  "productId": "long",
  "name": "string",
  "description": "string",
  "price": "decimal",
  "quantity": "integer",
  "imageUrl": "string",
  "categories": [
    {
      "categoryId": "long",
      "name": "string"
    }
  ],
  "producer": {
    "userId": "long",
    "username": "string"
  }
}
```
## Producer Application Endpoints
`/api/producer-applications`

| Method | Endpoint            | Description                | Auth Required | Role  | Body/Parameters                |
|--------|-------------------|----------------------------|---------------|-------|-------------------------------|
| POST   | /                 | Submit producer application| Yes          | User  | `ProducerApplicationRequest`  |
| GET    | /                 | Get all applications       | Yes          | Admin | None                          |
| GET    | /pending          | Get pending applications   | Yes          | Admin | None                          |
| POST   | /{id}/approve     | Approve application        | Yes          | Admin | `approveCC` (optional)        |
| POST   | /{id}/decline     | Decline application        | Yes          | Admin | `ApplicationDeclineRequest`   |

### ProducerApplicationRequest Example
```json
{
  "businessName": "string",
  "businessAddress": "string",
  "businessDescription": "string",
  "phoneNumber": "string",
  "categories": "string[] (optional)",
  "customCategory": "string (optional)",
  "cityRegion": "string (optional)",
  "customCityRegion": "string (optional)",
  "yearsOfExperience": "integer (optional)",
  "websiteOrSocialLink": "string (optional)"
}
```

## Order Endpoints
`/api/orders`

| Method | Endpoint            | Description           | Auth Required | Role  | Body/Parameters          |
|--------|-------------------|-----------------------|---------------|-------|------------------------|
| POST   | /                 | Create order          | No           | Any   | `OrderRequest`         |
| GET    | /                 | Get all orders        | Yes          | Admin | None                  |
| GET    | /{id}            | Get order by ID       | Yes          | User  | None                  |
| GET    | /guest/{id}?guestToken={token}   | Get guest order       | No           | Any   | None                  |

### OrderRequest Example
```json
{
  "items": [{
    "productId": "long",
    "quantity": "integer"
  }],
  "shippingAddress": "string",
  "phoneNumber": "string",
  "guestEmail": "string (optional)",
  "guestToken": "string (optional)",
  "accountCreation": {
    "createAccount": "boolean",
    "username": "string",
    "password": "string",
    "firstname": "string",
    "lastname": "string"
  },
  "payment": {
    "paymentMethod": "CARD|BITCOIN",
    "cardNumber": "string (for CARD)",
    "cardHolderName": "string (for CARD)",
    "expiryDate": "string (for CARD)",
    "cvv": "string (for CARD)",
    "transactionHash": "string (for BITCOIN)",
    "currency": "string"
  }
}
```

