# Local Market API Documentation

## Authentication Endpoints
`/api/auth`

| Method | Endpoint    | Description                | Auth Required | Body                                    |
|--------|------------|----------------------------|---------------|----------------------------------------|
| POST   | /register  | Register a new user        | No           | `RegisterRequest` (email, password)     |
| POST   | /login     | Login to get access token  | No           | `AuthRequest` (email, password)         |
| POST   | /logout    | Logout and blacklist token | Yes          | None                                   |

## Categories Endpoints
`/api/categories`

| Method | Endpoint    | Description              | Auth Required | Role  | Body                |
|--------|------------|--------------------------|---------------|-------|---------------------|
| POST   | /          | Create new category      | Yes          | Admin | `CategoryRequest`   |
| GET    | /          | Get all categories       | No           | -     | None                |
| GET    | /{id}      | Get category by ID       | No           | -     | None                |
| PUT    | /{id}      | Update category          | Yes          | Admin | `CategoryRequest`   |
| DELETE | /{id}      | Delete category          | Yes          | Admin | None                |

## Products Endpoints
`/api/products`

| Method | Endpoint           | Description              | Auth Required | Role     | Body             |
|--------|-------------------|--------------------------|---------------|----------|------------------|
| POST   | /                 | Create new product       | Yes          | Producer | `ProductRequest` |
| GET    | /                 | Get all products         | No           | -        | None             |
| GET    | /{id}            | Get product by ID        | No           | -        | None             |
| PUT    | /{id}            | Update product           | Yes          | Producer | `ProductRequest` |
| DELETE | /{id}            | Delete product           | Yes          | Producer | None             |
| GET    | /category/{id}   | Get products by category | No           | -        | None             |

## Producer Applications Endpoints
`/api/producer-applications`

| Method | Endpoint               | Description                    | Auth Required | Role     | Body/Params                   |
|--------|----------------------|--------------------------------|---------------|----------|-------------------------------|
| POST   | /                    | Submit producer application    | Yes          | Customer | `ProducerApplicationRequest`  |
| GET    | /                    | Get all applications          | Yes          | Admin    | None                          |
| GET    | /pending             | Get pending applications      | Yes          | Admin    | None                          |
| POST   | /{id}/approve        | Approve application           | Yes          | Admin    | `approveCC` (query param)     |
| POST   | /{id}/decline        | Decline application           | Yes          | Admin    | `reason`                      |
| GET    | /my-application      | Get user's application        | Yes          | Customer | None                          |
| GET    | /status              | Check application status      | Yes          | Customer | None                          |

## Orders Endpoints
`/api/orders`

| Method | Endpoint                    | Description                | Auth Required | Role  | Body            |
|--------|---------------------------|----------------------------|---------------|-------|-----------------|
| POST   | /                         | Create order               | Optional     | -     | `OrderRequest`  |
| POST   | /checkout                 | Create pending order       | Optional     | -     | `OrderRequest`  |
| POST   | /{orderId}/pay           | Process payment            | Optional     | -     | `PaymentInfo`   |
| GET    | /                         | Get user orders            | Yes          | -     | None            |
| GET    | /{orderId}               | Get specific order         | Optional     | -     | None            |
| GET    | /my-orders               | Get authenticated user orders| Yes         | -     | None            |
| GET    | /my-orders/status/{status}| Get orders by status      | Yes          | -     | None            |
| GET    | /my-orders/{orderId}     | Get specific user order    | Yes          | -     | None            |


