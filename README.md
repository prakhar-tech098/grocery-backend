# 🛒 Grocery Store Backend — Spring Boot

## Tech Stack
- **Java 17** + **Spring Boot 3.2**
- **Spring Security** + **JWT** (stateless auth)
- **Spring Data JPA** + **MySQL / TiDB Cloud**
- **Lombok** (boilerplate reduction)
- **Swagger UI** (auto-generated API docs)

---

## 📁 Project Structure

```
src/main/java/com/grocery/
├── GroceryBackendApplication.java   ← Entry point
├── config/
│   ├── SecurityConfig.java          ← JWT + CORS + route guards
│   └── DataInitializer.java         ← Seeds default admin/staff on startup
├── controller/
│   ├── AuthController.java          ← POST /api/auth/login, /register
│   ├── ProductController.java       ← CRUD  /api/products
│   ├── InventoryController.java     ← GET/PUT /api/inventory
│   ├── OrderController.java         ← POST /api/orders (billing)
│   └── DashboardController.java     ← GET /api/dashboard/stats
├── service/                         ← Business logic layer
├── repository/                      ← JPA data access layer
├── entity/                          ← JPA entities (DB tables)
├── dto/                             ← Request / Response objects
├── security/
│   ├── JwtUtils.java                ← Token generation & validation
│   ├── JwtAuthFilter.java           ← Intercepts every request
│   └── UserDetailsServiceImpl.java  ← Loads user from DB
└── exception/
    ├── ResourceNotFoundException.java
    └── GlobalExceptionHandler.java  ← Unified JSON error responses
```

---

## ⚙️ Setup & Run

### 1. Configure `application.properties`
```properties
spring.datasource.url=jdbc:mysql://<TIDB_HOST>:4000/<DB_NAME>?useSSL=true&serverTimezone=UTC
spring.datasource.username=<TIDB_USER>
spring.datasource.password=<TIDB_PASS>
```

### 2. Build & Run
```bash
./mvnw spring-boot:run
```
Server starts at → `http://localhost:8080`

### 3. Default Users (auto-created on first run)
| Username | Password  | Role  |
|----------|-----------|-------|
| admin    | admin123  | ADMIN |
| staff    | staff123  | STAFF |

> ⚠️ Change these passwords before deploying to production!

---

## 🔐 Authentication Flow

```
POST /api/auth/login
Body: { "username": "admin", "password": "admin123" }

Response:
{
  "success": true,
  "data": {
    "token": "eyJhbGci...",
    "username": "admin",
    "role": "ADMIN"
  }
}
```

**All protected requests** need the header:
```
Authorization: Bearer <your_token_here>
```

---

## 📋 Full API Reference

### Auth
| Method | Endpoint             | Auth     | Description       |
|--------|----------------------|----------|-------------------|
| POST   | /api/auth/login      | Public   | Login, get JWT    |
| POST   | /api/auth/register   | Public   | Register new user |

### Products
| Method | Endpoint                    | Auth       | Description             |
|--------|-----------------------------|------------|-------------------------|
| GET    | /api/products               | Any user   | List all products       |
| GET    | /api/products?search=milk   | Any user   | Search by name/category |
| GET    | /api/products?category=dairy| Any user   | Filter by category      |
| GET    | /api/products/{id}          | Any user   | Get one product         |
| POST   | /api/products               | ADMIN only | Create product          |
| PUT    | /api/products/{id}          | ADMIN only | Update product          |
| DELETE | /api/products/{id}          | ADMIN only | Delete product          |

### Inventory
| Method | Endpoint                         | Auth       | Description               |
|--------|----------------------------------|------------|---------------------------|
| GET    | /api/inventory                   | Any user   | All inventory records     |
| GET    | /api/inventory/low-stock         | Any user   | Items below threshold     |
| GET    | /api/inventory/product/{id}      | Any user   | Inventory for one product |
| PUT    | /api/inventory/product/{id}      | ADMIN only | Update stock / threshold  |

### Orders (Billing)
| Method | Endpoint                | Auth     | Description                   |
|--------|-------------------------|----------|-------------------------------|
| GET    | /api/orders             | Any user | All orders (newest first)     |
| GET    | /api/orders/{id}        | Any user | Single order with items       |
| POST   | /api/orders             | Any user | Create bill (deducts stock)   |
| PUT    | /api/orders/{id}/cancel | Any user | Cancel order (restores stock) |

### Dashboard
| Method | Endpoint              | Auth     | Description                        |
|--------|-----------------------|----------|------------------------------------|
| GET    | /api/dashboard/stats  | Any user | Products, orders, sales, low-stock |

---

## 💳 Create Order Example

```json
POST /api/orders
Authorization: Bearer <token>

{
  "customerName": "Ramesh Kumar",
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 3, "quantity": 1 }
  ]
}
```

**What happens automatically:**
1. Validates each product exists
2. Checks inventory — throws error if insufficient stock
3. Calculates total amount
4. Deducts stock from inventory
5. Saves order + line items
6. Returns full bill with subtotals

---

## 📊 Dashboard Response Example

```json
{
  "totalProducts": 42,
  "totalOrders": 158,
  "todaySales": 4320.50,
  "monthSales": 89750.00,
  "lowStockCount": 3,
  "lowStockItems": [
    { "productName": "Basmati Rice 5kg", "quantity": 4, "threshold": 10 }
  ]
}
```

---

## 🌐 Swagger UI
Once running, visit:  
`http://localhost:8080/swagger-ui.html`

---

## 🚀 Deployment (Render)

1. Push code to GitHub
2. Create a new **Web Service** on Render
3. Build command: `./mvnw clean package -DskipTests`
4. Start command: `java -jar target/grocery-backend-1.0.0.jar`
5. Add environment variables (DB credentials, JWT secret)

---

## 🏗️ Architecture (Layered)

```
HTTP Request
    ↓
Controller  (routes, input validation)
    ↓
Service     (business logic, calculations)
    ↓
Repository  (JPA queries to DB)
    ↓
MySQL / TiDB Cloud
```
