# Mari Wallet — API Documentation

## Service Overview

| Service | Port | Base URL |
|---|---|---|
| **Gateway** | 8080 | `http://localhost:8080` |
| **Auth Service** | 8093 | `http://localhost:8093` |
| **Wallet Service** | 8090 | `http://localhost:8090` |
| **Wallet User Management** | 8091 | `http://localhost:8091` |
| **Payment Service** | 8092 | `http://localhost:8092` |

**All requests through the Gateway** are routed as:
- `/api/v1/auth/**` → Auth Service
- `/api/v2/users/**` → Wallet User Management
- `/api/v2/**` → Wallet Service
- `/api/v1/**` → Payment Service

---

## Common Patterns

### Universal Request Wrapper
Most protected endpoints wrap request data:
```json
{
  "data": { },
  "channelDetails": {
    "deviceId": "device-abc123",
    "channel": "MOBILE | WEB | USSD | API",
    "name": "Samsung Galaxy S23",
    "osVersion": "14",
    "appVersion": "1.0.0",
    "ipAddress": "192.168.1.1",
    "os": "Android"
  }
}
```

### Standard Response Format
```json
{
  "status": "00",
  "message": "Optional message",
  "data": { }
}
```
- `"00"` = Success
- `"01"` = Error

### Authentication Header (all protected endpoints)
```
Authorization: Bearer <jwt-token>
```

---

---

# 1. Auth Service — Port 8093

**Public endpoints** (no token required):
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/integrator/login`
- `POST /api/v1/auth/admin/login`
- `GET /.well-known/jwks.json`

---

## POST `/api/v1/auth/login`
Authenticate a wallet user with phone number and PIN.

### Request
```http
POST /api/v1/auth/login
Content-Type: application/json
```
```json
{
  "data": {
    "phoneNumber": "+254712345678",
    "pin": "1234"
  },
  "channelDetails": {
    "deviceId": "device-abc123",
    "channel": "MOBILE",
    "name": "Samsung Galaxy S23",
    "osVersion": "14",
    "appVersion": "1.0.0",
    "ipAddress": "192.168.1.1",
    "os": "Android"
  }
}
```

### Responses

**200 OK — Login successful**
```json
{
  "status": "00",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  }
}
```

**401 Unauthorized — Wrong PIN**
```json
{
  "status": "01",
  "error": "Invalid credentials"
}
```

**404 Not Found — User does not exist**
```json
{
  "status": "01",
  "error": "User not found"
}
```

**400 Bad Request — Missing required fields**
```json
{
  "status": "01",
  "error": "phoneNumber is required"
}
```

---

## POST `/api/v1/auth/integrator/login`
Authenticate a third-party integrator using access key and secret.

### Request
```http
POST /api/v1/auth/integrator/login
Content-Type: application/json
```
```json
{
  "data": {
    "accessKey": "INTGR-KEY-abc123",
    "accessSecret": "INTGR-SECRET-xyz789"
  },
  "channelDetails": {
    "deviceId": "server-001",
    "channel": "API"
  }
}
```

### Responses

**200 OK**
```json
{
  "status": "00",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  }
}
```

**401 Unauthorized — Invalid credentials**
```json
{
  "status": "01",
  "error": "Invalid integrator credentials"
}
```

---

## POST `/api/v1/auth/admin/login`
Authenticate a system administrator.

### Request
```http
POST /api/v1/auth/admin/login
Content-Type: application/json
```
```json
{
  "data": {
    "username": "admin",
    "password": "Admin@1234!"
  },
  "channelDetails": {
    "deviceId": "admin-portal",
    "channel": "WEB"
  }
}
```

### Responses

**200 OK**
```json
{
  "status": "00",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  }
}
```

**401 Unauthorized**
```json
{
  "status": "01",
  "error": "Invalid admin credentials"
}
```

---

## POST `/api/v1/auth/integrator`
Create a new integrator. Requires `ROLE_SYSTEM_ADMIN`.

### Request
```http
POST /api/v1/auth/integrator
Authorization: Bearer <admin-jwt>
Content-Type: application/json
```
```json
{
  "data": {
    "name": "Acme Corp",
    "description": "Acme payment integrator"
  },
  "channelDetails": {
    "deviceId": "admin-portal",
    "channel": "WEB"
  }
}
```

### Responses

**200 OK — Integrator created**
```json
{
  "status": "00",
  "data": {
    "id": "b2c3d4e5-f678-90ab-cdef-1234567890ab",
    "name": "Acme Corp",
    "accessKey": "INTGR-KEY-abc123",
    "accessSecret": "INTGR-SECRET-xyz789"
  }
}
```

**401 Unauthorized — Missing token**
```json
{
  "status": "01",
  "error": "Unauthorized"
}
```

**403 Forbidden — Not a system admin**
```json
{
  "status": "01",
  "error": "Access denied"
}
```

---

## GET `/.well-known/jwks.json`
Returns the public JWK Set used by all services to validate JWT tokens.

### Request
```http
GET /.well-known/jwks.json
```

### Response — 200 OK
```json
{
  "keys": [
    {
      "kty": "RSA",
      "use": "sig",
      "n": "xjk...",
      "e": "AQAB",
      "kid": "1"
    }
  ]
}
```

---

---

# 2. Wallet User Management — Port 8091

**Public endpoints**: `/register`, `/verify-otp`, `/resend-otp`

---

## POST `/api/v2/users/register`
Register a new wallet user with KYC documents.

### Request
```http
POST /api/v2/users/register
Content-Type: multipart/form-data
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `data` | JSON part | Yes | See below |
| `id_front` | File | Yes | Front of ID document |
| `id_back` | File | No | Back of ID document |

**`data` field:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "254712345678",
  "email": "john.doe@example.com",
  "idType": "NATIONAL_ID",
  "idNumber": "12345678",
  "deviceId": "device-abc123",
  "deviceType": "ANDROID",
  "channel": "MOBILE",
  "deviceModel": "Samsung Galaxy S23",
  "appVersion": "1.0.0"
}
```

**Validation rules:**
- `idType`: `NATIONAL_ID | PASSPORT | DRIVING_LICENSE`
- `phoneNumber`: International format without `+` (e.g., `254712345678`)
- `firstName` / `lastName`: 2–100 chars
- `email`: valid email, max 255 chars

### Responses

**201 Created**
```json
{
  "status": "00",
  "message": "Registration successful. OTP sent to 254712345678",
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+254712345678",
    "email": "john.doe@example.com",
    "status": "PENDING_VERIFICATION",
    "createdAt": "2024-01-01T12:00:00Z"
  }
}
```

**400 Bad Request — Validation error**
```json
{
  "status": "01",
  "error": "phoneNumber must be in international format"
}
```

**409 Conflict — Phone already registered**
```json
{
  "status": "01",
  "error": "User with this phone number already exists"
}
```

**500 Internal Server Error**
```json
{
  "status": "01",
  "error": "Registration failed. Please try again"
}
```

---

## POST `/api/v2/users/verify-otp`
Verify phone number with OTP received after registration.

### Request
```http
POST /api/v2/users/verify-otp
Content-Type: application/json
```
```json
{
  "data": {
    "phoneNumber": "+254712345678",
    "otp": "123456",
    "purpose": "REGISTRATION"
  },
  "channelDetails": {
    "deviceId": "device-abc123",
    "channel": "MOBILE"
  }
}
```

**`purpose` values**: `REGISTRATION | PIN_RESET | LOGIN`

### Responses

**200 OK**
```json
{
  "status": "00",
  "message": "Phone number verified successfully",
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+254712345678",
    "email": "john.doe@example.com",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T12:00:00Z"
  }
}
```

**400 Bad Request — OTP format invalid**
```json
{
  "status": "01",
  "error": "OTP must be exactly 6 digits"
}
```

**400 Bad Request — Wrong OTP**
```json
{
  "status": "01",
  "error": "Invalid or expired OTP"
}
```

**404 Not Found — User not found**
```json
{
  "status": "01",
  "error": "User not found"
}
```

---

## POST `/api/v2/users/resend-otp`
Resend OTP to the user's phone number.

### Request
```http
POST /api/v2/users/resend-otp
Content-Type: application/json
```
```json
{
  "data": {
    "phoneNumber": "+254712345678",
    "purpose": "REGISTRATION"
  },
  "channelDetails": {
    "deviceId": "device-abc123",
    "channel": "MOBILE"
  }
}
```

### Responses

**200 OK**
```json
{
  "status": "00",
  "message": "OTP resent successfully"
}
```

**404 Not Found**
```json
{
  "status": "01",
  "error": "User not found"
}
```

**429 Too Many Requests**
```json
{
  "status": "01",
  "error": "OTP resend limit exceeded. Please wait before retrying"
}
```

---

## POST `/api/v2/users/set-pin`
Set or reset a user's wallet PIN. **Requires JWT.**

### Request
```http
POST /api/v2/users/set-pin
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "data": {
    "pin": "1234",
    "customerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  },
  "channelDetails": {
    "deviceId": "device-abc123",
    "channel": "MOBILE"
  }
}
```

**Validation**: PIN must be 4–6 digits.

### Responses

**200 OK**
```json
{
  "status": "00",
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "firstName": "John",
    "lastName": "Doe",
    "status": "ACTIVE"
  }
}
```

**400 Bad Request — Weak PIN**
```json
{
  "status": "01",
  "error": "PIN must be between 4 and 6 digits"
}
```

**401 Unauthorized**
```json
{
  "status": "01",
  "error": "Unauthorized"
}
```

---

## GET `/api/v2/users/{customerId}`
Retrieve a customer's profile. **Requires JWT.**

### Request
```http
GET /api/v2/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890
Authorization: Bearer <jwt-token>
```

### Responses

**200 OK**
```json
{
  "status": "00",
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+254712345678",
    "email": "john.doe@example.com",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T12:00:00Z"
  }
}
```

**404 Not Found**
```json
{
  "status": "01",
  "error": "Customer not found"
}
```

---

## POST `/api/v2/users/lookup`
Find a customer by phone number. **Requires JWT.**

### Request
```http
POST /api/v2/users/lookup
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "phoneNumber": "+254712345678"
}
```

### Responses

**200 OK**
```json
{
  "status": "00",
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+254712345678",
    "status": "ACTIVE"
  }
}
```

**404 Not Found**
```json
{
  "status": "01",
  "error": "No user found with that phone number"
}
```

---

## GET `/api/v2/users/{customerId}/kyc/documents`
Retrieve a customer's uploaded KYC documents. **Requires JWT.**

### Request
```http
GET /api/v2/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890/kyc/documents
Authorization: Bearer <jwt-token>
```

### Responses

**200 OK**
```json
{
  "status": "00",
  "data": [
    {
      "id": "doc-uuid-1",
      "customerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "idType": "NATIONAL_ID",
      "idNumber": "12345678",
      "frontImageUrl": "https://cdn.example.com/docs/front.jpg",
      "backImageUrl": "https://cdn.example.com/docs/back.jpg",
      "verificationStatus": "PENDING"
    }
  ]
}
```

**`verificationStatus` values**: `PENDING | APPROVED | REJECTED`

---

## GET `/api/v2/users/{customerId}/devices`
List all devices for a customer. **Requires JWT.**

### Request
```http
GET /api/v2/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890/devices
Authorization: Bearer <jwt-token>
```

### Response — 200 OK
```json
{
  "status": "00",
  "data": [
    {
      "id": "device-uuid-1",
      "deviceId": "device-abc123",
      "name": "Samsung Galaxy S23",
      "osVersion": "14",
      "deviceType": "ANDROID",
      "appVersion": "1.0.0",
      "status": "ACTIVE",
      "registeredAt": "2024-01-01T12:00:00Z",
      "lastSeenAt": "2024-01-02T08:30:00Z"
    }
  ]
}
```

---

## PATCH `/api/v2/users/{customerId}/devices/{deviceId}/status`
Block or unblock a device. **Requires JWT.**

### Request
```http
PATCH /api/v2/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890/devices/device-uuid-1/status
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "status": "BLOCKED"
}
```

**`status` values**: `ACTIVE | BLOCKED | PENDING`

### Responses

**200 OK**
```json
{
  "status": "00",
  "data": {
    "id": "device-uuid-1",
    "deviceId": "device-abc123",
    "status": "BLOCKED"
  }
}
```

**404 Not Found**
```json
{
  "status": "01",
  "error": "Device not found"
}
```

---

---

# 3. Wallet Service — Port 8090

All endpoints require JWT. Service-to-service calls use Basic Auth (`wallet-svc`).

---

## POST `/api/v2/accounts`
Open a new wallet account for a user.

### Request
```http
POST /api/v2/accounts
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "data": {
    "currency": "KES",
    "accountPrefix": "WLT",
    "phoneNumber": "+254712345678",
    "accountName": "John Doe",
    "openingBalance": 0.00
  },
  "channelDetails": {
    "deviceId": "device-abc123",
    "channel": "MOBILE",
    "ipAddress": "192.168.1.1"
  }
}
```

### Responses

**200 OK**
```json
{
  "status": "00",
  "data": {
    "success": true,
    "accountNumber": "WLT0000001",
    "statusCode": "00",
    "message": "Account opened successfully"
  }
}
```

**400 Bad Request — Invalid currency**
```json
{
  "status": "01",
  "error": "Unsupported currency: USD"
}
```

**409 Conflict — Account already exists**
```json
{
  "status": "01",
  "error": "Account already exists for this user"
}
```

---

## POST `/api/v2/transactions/ft`
Perform a fund transfer between two wallet accounts.

### Request
```http
POST /api/v2/transactions/ft
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "data": {
    "debitAccount": "WLT0000001",
    "creditAccount": "WLT0000002",
    "amount": 500.00,
    "presentment": false,
    "transactionType": "FT",
    "currency": "KES",
    "phoneNumber": "+254712345678"
  },
  "channelDetails": {
    "deviceId": "device-abc123",
    "channel": "MOBILE",
    "ipAddress": "192.168.1.1"
  }
}
```

### Responses

**200 OK — Transfer successful**
```json
{
  "status": "00",
  "data": {
    "transactionRef": "TXN-20240101-001",
    "responseCode": "00",
    "responseMessage": "Transaction successful"
  }
}
```

**400 Bad Request — Insufficient funds**
```json
{
  "status": "01",
  "error": "Insufficient balance on debit account"
}
```

**400 Bad Request — Daily limit exceeded**
```json
{
  "status": "01",
  "error": "Transaction exceeds daily limit of 100,000 KES"
}
```

**404 Not Found — Account not found**
```json
{
  "status": "01",
  "error": "Debit account WLT0000001 not found"
}
```

**500 Internal Server Error**
```json
{
  "status": "01",
  "error": "Transaction failed. Please try again"
}
```

---

## POST `/api/v2/transactions/card-topup`
Record a card top-up as a wallet transaction.

### Request
```http
POST /api/v2/transactions/card-topup
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "data": {
    "debitAccount": "CARD-POOL-001",
    "creditAccount": "WLT0000001",
    "amount": 1000.00,
    "presentment": false,
    "transactionType": "CARD_TOPUP",
    "currency": "KES",
    "phoneNumber": "+254712345678"
  },
  "channelDetails": {
    "deviceId": "device-abc123",
    "channel": "MOBILE"
  }
}
```

### Response — 200 OK
```json
{
  "status": "00",
  "data": {
    "transactionRef": "TXN-20240101-002",
    "responseCode": "00",
    "responseMessage": "Card topup transaction recorded"
  }
}
```

---

## POST `/api/v2/topup/card/initiate`
Initiate a card top-up through the payment gateway.

### Request
```http
POST /api/v2/topup/card/initiate
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "data": {
    "creditAccount": "WLT0000001",
    "amount": 1000.00,
    "currency": "KES",
    "phoneNumber": "+254712345678",
    "card": {
      "pan": "4111111111111111",
      "cvv": "123",
      "expiry": "12/26"
    }
  },
  "channelDetails": {
    "deviceId": "device-abc123",
    "channel": "MOBILE",
    "ipAddress": "192.168.1.1"
  }
}
```

### Responses

**200 OK**
```json
{
  "status": "00",
  "data": {
    "esbRef": "ESB-20240101-001",
    "transactionRef": "TXN-20240101-003",
    "status": "INITIATED",
    "message": "Topup initiated successfully"
  }
}
```

**400 Bad Request — Invalid card**
```json
{
  "status": "01",
  "error": "Invalid card number"
}
```

---

## POST `/api/v2/topup/card/callback`
Callback endpoint for card payment provider to notify topup result.

### Request
```http
POST /api/v2/topup/card/callback
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "data": {
    "esbRef": "ESB-20240101-001",
    "responseCode": "00",
    "responseMessage": "Approved",
    "receiptNumber": "RCP123456"
  },
  "channelDetails": {
    "deviceId": "gateway-server",
    "channel": "API"
  }
}
```

### Response — 200 OK
```json
{
  "status": "00"
}
```

---

## POST `/api/v2/config/services`
Create a service configuration. **Requires JWT.**

### Request
```http
POST /api/v2/config/services
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "data": {
    "serviceId": 1,
    "externalServiceId": "EXT-001",
    "channelId": 1,
    "serviceCode": "FT",
    "isExternal": false,
    "accountId": 1001,
    "senderNarration": "Transfer from {{account}}",
    "receiverNarration": "Transfer to {{account}}",
    "dailyLimit": 100000.00,
    "weeklyLimit": 500000.00,
    "monthlyLimit": 2000000.00,
    "description": "Fund Transfer Service",
    "createdBy": "admin"
  },
  "channelDetails": {
    "deviceId": "admin-portal",
    "channel": "WEB"
  }
}
```

### Responses

**200 OK**
```json
{
  "status": "00",
  "data": {
    "id": 1,
    "serviceCode": "FT",
    "description": "Fund Transfer Service",
    "dailyLimit": 100000.00,
    "weeklyLimit": 500000.00,
    "monthlyLimit": 2000000.00
  }
}
```

**409 Conflict — Duplicate service code**
```json
{
  "status": "01",
  "error": "Service code FT already exists"
}
```

---

## GET `/api/v2/config/services`
List all service configurations.

### Request
```http
GET /api/v2/config/services
Authorization: Bearer <jwt-token>
```

### Response — 200 OK
```json
{
  "status": "00",
  "data": [
    {
      "id": 1,
      "serviceCode": "FT",
      "description": "Fund Transfer Service",
      "dailyLimit": 100000.00
    },
    {
      "id": 2,
      "serviceCode": "CARD_TOPUP",
      "description": "Card Top-up",
      "dailyLimit": 50000.00
    }
  ]
}
```

---

## GET `/api/v2/config/services/{id}`
Get a service configuration by ID.

### Request
```http
GET /api/v2/config/services/1
Authorization: Bearer <jwt-token>
```

### Responses

**200 OK**
```json
{
  "status": "00",
  "data": {
    "id": 1,
    "serviceCode": "FT",
    "description": "Fund Transfer Service",
    "dailyLimit": 100000.00,
    "weeklyLimit": 500000.00,
    "monthlyLimit": 2000000.00
  }
}
```

**404 Not Found**
```json
{
  "status": "01",
  "error": "Service config with id 99 not found"
}
```

---

## GET `/api/v2/config/services/by-code/{serviceCode}`
Get a service configuration by its code.

### Request
```http
GET /api/v2/config/services/by-code/FT
Authorization: Bearer <jwt-token>
```

### Response — 200 OK (same structure as by ID)

---

## POST `/api/v2/config/charges`
Create a charge configuration for a service.

### Request
```http
POST /api/v2/config/charges
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "data": {
    "serviceManagementId": 1,
    "minAmount": 0.00,
    "maxAmount": 10000.00,
    "chargeValue": 30.00,
    "valueType": "FLAT",
    "chargeType": "SENDER",
    "accountId": 2001,
    "senderNarration": "Charge for transfer",
    "receiverNarration": "Fee credit",
    "createdBy": "admin"
  },
  "channelDetails": {
    "deviceId": "admin-portal",
    "channel": "WEB"
  }
}
```

**`valueType` values**: `FLAT | PERCENTAGE`
**`chargeType` values**: `SENDER | RECEIVER`

### Response — 200 OK
```json
{
  "status": "00",
  "data": {
    "id": 1,
    "serviceManagementId": 1,
    "chargeValue": 30.00,
    "valueType": "FLAT",
    "chargeType": "SENDER",
    "minAmount": 0.00,
    "maxAmount": 10000.00
  }
}
```

---

## GET `/api/v2/config/charges/service/{serviceManagementId}`
List all charge configs for a service.

### Request
```http
GET /api/v2/config/charges/service/1
Authorization: Bearer <jwt-token>
```

### Response — 200 OK
```json
{
  "status": "00",
  "data": [
    {
      "id": 1,
      "serviceManagementId": 1,
      "chargeValue": 30.00,
      "valueType": "FLAT",
      "chargeType": "SENDER",
      "minAmount": 0.00,
      "maxAmount": 10000.00
    },
    {
      "id": 2,
      "serviceManagementId": 1,
      "chargeValue": 0.5,
      "valueType": "PERCENTAGE",
      "chargeType": "SENDER",
      "minAmount": 10000.01,
      "maxAmount": 100000.00
    }
  ]
}
```

---

## GET `/api/v2/config/charges/{id}`
Get a charge config by ID.

### Request
```http
GET /api/v2/config/charges/1
Authorization: Bearer <jwt-token>
```

### Response — 200 OK (single charge object as above)

---

---

# 4. Payment Service — Port 8092

**Public endpoints**: `/api/v1/card/callback`
All others require JWT.

---

## POST `/api/v1/card/device-fingerprint`
Obtain a device fingerprint / data collection token before card payment.

### Request
```http
POST /api/v1/card/device-fingerprint
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "tranid": "TRN-001",
  "amount": "1000",
  "currency": "KES",
  "country": "KE",
  "firstname": "John",
  "secondname": "Doe",
  "phone": "+254712345678",
  "email": "john@example.com",
  "cardNumber": "4111111111111111",
  "cardExpiryMonth": "12",
  "cardExpiryYear": "2026",
  "cardCvv": "123",
  "cardType": "VISA"
}
```

### Responses

**200 OK**
```json
{
  "status": "00",
  "data": {
    "statuscode": "200",
    "tranid": "TRN-001",
    "statusmessage": "Success",
    "id": "fingerprint-data-id",
    "accessToken": "device-token-abc",
    "deviceDataCollectionUrl": "https://centinelapistag.cardinalcommerce.com/V1/Cruise/Collect",
    "referenceId": "REF-123456"
  }
}
```

**400 Bad Request — Invalid card**
```json
{
  "status": "01",
  "error": "Invalid card number"
}
```

**401 Unauthorized**
```json
{
  "status": "01",
  "error": "Unauthorized"
}
```

**502 Bad Gateway — Upstream error**
```json
{
  "status": "01",
  "error": "Payment gateway unavailable"
}
```

---

## POST `/api/v1/card/payment`
Process a card payment (includes 3DS browser data for fraud prevention).

### Request
```http
POST /api/v1/card/payment
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "tranid": "TRN-001",
  "amount": "1000",
  "currency": "KES",
  "referenceId": "REF-123456",
  "firstname": "John",
  "phone": "+254712345678",
  "email": "john@example.com",
  "cardNumber": "4111111111111111",
  "cardExpiryMonth": "12",
  "cardExpiryYear": "2026",
  "cardCvv": "123",
  "cardType": "VISA",
  "ipAddress": "192.168.1.1",
  "httpAcceptContent": "text/html,application/json",
  "httpBrowserLanguage": "en-US",
  "httpBrowserJavaEnabled": "true",
  "httpBrowserJavaScriptEnabled": "true",
  "httpBrowserColorDepth": "24",
  "httpBrowserScreenHeight": "1080",
  "httpBrowserScreenWidth": "1920",
  "httpBrowserTimeDifference": "-180",
  "userAgentBrowserValue": "Mozilla/5.0 (Linux; Android 14)"
}
```

### Responses

**200 OK — Payment initiated (3DS challenge)**
```json
{
  "status": "00",
  "data": {
    "pareq": "eJxVUltvgjAU...",
    "pastepup": "ACS-response-value",
    "accessToken": "auth-token",
    "message": "3DS authentication required",
    "statusCode": "200"
  }
}
```

**200 OK — Payment approved directly**
```json
{
  "status": "00",
  "data": {
    "message": "Payment approved",
    "statusCode": "00"
  }
}
```

**402 Payment Required — Card declined**
```json
{
  "status": "01",
  "error": "Card declined by issuer"
}
```

**400 Bad Request — Expired card**
```json
{
  "status": "01",
  "error": "Card is expired"
}
```

---

## POST `/api/v1/card/callback`
Callback from the payment gateway after card transaction completes. **Public endpoint.**

### Request
```http
POST /api/v1/card/callback
Content-Type: application/json
```
```json
{
  "statuscode": "00",
  "tranid": "TRN-001",
  "status": "APPROVED"
}
```

### Response — 200 OK
```json
{
  "statuscode": "00",
  "tranid": "TRN-001"
}
```

---

## POST `/api/v1/billing/bpc/confirm-meter`
Confirm a BPC electricity meter before vending.

### Request
```http
POST /api/v1/billing/bpc/confirm-meter
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "meterNumber": "123456789"
}
```

### Responses

**200 OK — Valid meter**
```json
{
  "canVend": "YES",
  "meterNumber": "123456789",
  "maxVendAmount": "10000",
  "minVendAmount": "100",
  "kyc": {
    "name": "John Doe",
    "address": "123 Main Street, Nairobi",
    "contact": "+254712345678",
    "utilityType": "ELECTRICITY",
    "daysLastPurchase": "5"
  },
  "status": "ACTIVE",
  "fault": null,
  "message": "Meter confirmed successfully"
}
```

**200 OK — Faulty meter**
```json
{
  "canVend": "NO",
  "meterNumber": "123456789",
  "status": "FAULTY",
  "fault": "Tamper detected",
  "message": "Meter cannot vend"
}
```

**404 Not Found — Unknown meter**
```json
{
  "status": "01",
  "error": "Meter number not found"
}
```

---

## POST `/api/v1/billing/vend`
Purchase electricity token for a meter.

### Request
```http
POST /api/v1/billing/vend
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "serviceCode": "BPC_TRANSACTION",
  "accountNo": "123456789",
  "amount": "1000",
  "currency": "BWP",
  "phoneNumber": "+26771234567",
  "transactionId": "TXN-20240101-001",
  "email": "john@example.com"
}
```

### Responses

**200 OK — Vend successful**
```json
{
  "message": "Vend successful",
  "responseCode": "00",
  "amount": "1000",
  "currency": "BWP",
  "accountNo": "123456789",
  "phoneNumber": "+26771234567",
  "serviceId": "1005",
  "transactionId": "TXN-20240101-001",
  "description": "BPC Electricity Token Purchase",
  "clientId": "CLIENT-001",
  "meterNumber": "123456789",
  "tariffIndex": "1",
  "receiptNumber": "RCP-20240101-001",
  "token": [
    {
      "amount": "1000",
      "token": "1234-5678-9012-3456"
    }
  ]
}
```

**400 Bad Request — Amount below minimum**
```json
{
  "status": "01",
  "error": "Amount is below minimum vend amount of 100"
}
```

**402 Payment Required — Insufficient wallet balance**
```json
{
  "status": "01",
  "error": "Insufficient funds for this transaction"
}
```

**500 Internal Server Error — Provider failure**
```json
{
  "status": "01",
  "error": "Vend failed. Provider unavailable"
}
```

---

## GET `/api/v1/billing/transaction/{referenceId}/status`
Check the status of a billing transaction.

### Request
```http
GET /api/v1/billing/transaction/TXN-20240101-001/status
Authorization: Bearer <jwt-token>
```

### Responses

**200 OK**
```json
{
  "status": "00",
  "data": "COMPLETED"
}
```

**`data` values**: `COMPLETED | PENDING | FAILED`

**404 Not Found**
```json
{
  "status": "01",
  "error": "Transaction not found"
}
```

---

## GET `/api/v1/intercape/bus-stops`
List all available Intercape bus stops.

### Request
```http
GET /api/v1/intercape/bus-stops
Authorization: Bearer <jwt-token>
```

### Response — 200 OK
```json
{
  "status": "00",
  "data": [
    { "code": "NBO", "name": "Nairobi", "country": "KE" },
    { "code": "MBA", "name": "Mombasa", "country": "KE" },
    { "code": "GBE", "name": "Gaborone", "country": "BW" }
  ]
}
```

---

## POST `/api/v1/intercape/trips`
Search for available bus trips.

### Request
```http
POST /api/v1/intercape/trips
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "depPlace": "NBO",
  "arrPlace": "MBA",
  "transactionId": "TXN-SEARCH-001"
}
```

### Responses

**200 OK**
```json
{
  "status": "00",
  "data": [
    {
      "tripId": 12345,
      "departureTime": "08:00",
      "arrivalTime": "14:00",
      "price": 2000,
      "availableSeats": 30,
      "coachName": "Coach 001"
    },
    {
      "tripId": 12346,
      "departureTime": "14:00",
      "arrivalTime": "20:00",
      "price": 2000,
      "availableSeats": 12,
      "coachName": "Coach 002"
    }
  ]
}
```

**404 Not Found — No trips found**
```json
{
  "status": "01",
  "error": "No trips available for the selected route"
}
```

---

## POST `/api/v1/intercape/booking`
Create a bus ticket booking (adds to basket).

### Request
```http
POST /api/v1/intercape/booking
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "transactionId": "TXN-BOOK-001",
  "tripId": 12345,
  "coachSerial": 1,
  "depPlace": "NBO",
  "arrPlace": "MBA",
  "travelClass": "ECONOMY",
  "numTickets": 2,
  "price": 4000
}
```

### Responses

**200 OK**
```json
{
  "status": "00",
  "data": {
    "content": {
      "trip": {
        "tripId": 12345,
        "priceCheck": true,
        "availabilityCheck": true,
        "discounts": {
          "discount": [
            {
              "discountCode": "EARLY",
              "discountName": "Early Bird",
              "discountPercentage": 10,
              "discountedPrice": 3600
            }
          ]
        }
      },
      "basketId": 999
    }
  }
}
```

**409 Conflict — Seats no longer available**
```json
{
  "status": "01",
  "error": "Selected seats are no longer available"
}
```

---

## POST `/api/v1/intercape/booking/total`
Get the total amount for a basket before payment.

### Request
```http
POST /api/v1/intercape/booking/total
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "basketId": 999,
  "transactionId": "TXN-BOOK-001"
}
```

### Response — 200 OK
```json
{
  "status": "00",
  "data": {
    "basketId": 999,
    "totalAmount": 3600,
    "currency": "BWP",
    "breakdown": [
      { "description": "Ticket x2", "amount": 4000 },
      { "description": "Early Bird Discount", "amount": -400 }
    ]
  }
}
```

---

## POST `/api/v1/intercape/booking/paid`
Mark a basket as paid after successful payment.

### Request
```http
POST /api/v1/intercape/booking/paid
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "basketId": 999,
  "transactionId": "TXN-BOOK-001",
  "amount": 3600
}
```

### Response — 200 OK
```json
{
  "status": "00",
  "data": {
    "basketId": 999,
    "bookingRef": "INTCP-BK-20240101",
    "tickets": [
      {
        "ticketNumber": "TKT-001",
        "seat": "12A",
        "passengerName": "John Doe"
      }
    ],
    "status": "CONFIRMED"
  }
}
```

---

## POST `/api/v1/intercape/payment-status`
Update the payment status of a basket.

### Request
```http
POST /api/v1/intercape/payment-status
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
```json
{
  "basketId": 999,
  "transactionId": "TXN-BOOK-001",
  "paymentStatus": "PAID"
}
```

### Response — 200 OK
```json
{
  "status": "00",
  "data": {
    "basketId": 999,
    "paymentStatus": "PAID",
    "updatedAt": "2024-01-01T14:00:00Z"
  }
}
```

---

## GET `/api/v1/third-party/send-sms/{phoneNumber}/{message}`
Send an SMS to a phone number.

### Request
```http
GET /api/v1/third-party/send-sms/%2B254712345678/Your%20OTP%20is%20123456
Authorization: Bearer <jwt-token>
```

### Response — 200 OK
(No response body — SMS sent)

**400 Bad Request — Invalid phone**
```json
{
  "status": "01",
  "error": "Invalid phone number format"
}
```

---

---

# Authentication Flow

```
1. Register        POST /api/v2/users/register          (Public)
      ↓
2. Verify OTP      POST /api/v2/users/verify-otp        (Public)
      ↓
3. Login           POST /api/v1/auth/login              (Public)
      ↓ receives JWT
4. Set PIN         POST /api/v2/users/set-pin           (JWT Required)
      ↓
5. All other calls use:   Authorization: Bearer <jwt>
```

---

# Common HTTP Status Code Reference

| Code | Meaning | When it occurs |
|---|---|---|
| `200` | OK | Successful request |
| `201` | Created | Resource created (register) |
| `400` | Bad Request | Validation failure, invalid input |
| `401` | Unauthorized | Missing or invalid JWT token |
| `402` | Payment Required | Insufficient funds, card declined |
| `403` | Forbidden | Valid token but insufficient role |
| `404` | Not Found | Resource doesn't exist |
| `409` | Conflict | Duplicate resource (already registered) |
| `429` | Too Many Requests | OTP resend rate limit |
| `500` | Internal Server Error | Unexpected server failure |
| `502` | Bad Gateway | Upstream provider unavailable |

---

# Environment Variables Summary

| Variable | Service | Default |
|---|---|---|
| `JWT_EXPIRY_HOURS` | Auth | `24` |
| `ADMIN_USERNAME` | Auth | `admin` |
| `ADMIN_PASSWORD` | Auth | `Admin@1234!` |
| `AUTH_SERVICE_URL` | All others | `http://localhost:8093` |
| `WALLET_SERVICE_URL` | User Mgmt | — |
| `FILE_UPLOAD_DIR` | User Mgmt | `./uploads` |
| `TCP_BASE_URL` | Payment | — |
| `INTERCAPE_BASE_URL` | Payment | `https://www.paybills.co.bw:8601/ecommerce/intercape` |
| `BILLING_BASE_URL` | Payment | — |
| `SERVICE_USERNAME` | Wallet | `wallet-svc` |
