# In-Memory JWT Authentication + Product API (Spring Boot)

Backend task for OneDAO: JWT based authentication with **in-memory users**, OTP verification,
country-restricted sign-up, role based access (Administrator-only Settings), logout with token
revocation, and **PostgreSQL** product CRUD with pagination / sorting / filtering. Swagger UI included.

## Tech
Java 17 · Spring Boot 3.3 · Spring Security · jjwt 0.12 · Spring Data JPA · PostgreSQL · springdoc-openapi

## Run
```bash
docker compose up -d          # PostgreSQL on :5432 (db productdb, user/pass postgres)
mvn spring-boot:run           # app on http://localhost:8080
mvn test                      # unit tests
```
Swagger UI: http://localhost:8080/swagger-ui.html  (use **Authorize** and paste the JWT)

Configuration lives in `src/main/resources/application.properties`
(override with env vars `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`).

## Endpoints
| Method | URL | Access | Notes |
|---|---|---|---|
| POST | `/api/auth/signup` | public | username/email/password/userType; country check; returns OTP (demo) |
| POST | `/api/auth/verify-otp` | public | `{email, otp}` activates account |
| POST | `/api/auth/login` | public | returns JWT; failure → `401 "Login Failed"` (+ `/api/error`) |
| GET | `/api/error` | public | `Login Failed` |
| GET | `/api/dashboard` | any logged-in user | `Welcome <Username>` |
| GET | `/api/settings` | **Administrator only** | others get 403 |
| POST | `/api/auth/logout` | logged-in | `<Username> Logged Out`; token revoked |
| GET/POST | `/api/products` | logged-in | list supports `page,size,sortBy,direction,name,minPrice,maxPrice` |
| GET/PUT/DELETE | `/api/products/{id}` | logged-in | |

### Validation rules (sign-up)
* Username 8–50 chars
* Email must contain `@` and `_`
* Password 8–12 chars with ≥1 capital, ≥1 small, ≥1 digit, ≥1 special character
* User type: `Standard User` or `Administrator`

### Quick flow (curl)
```bash
curl -X POST localhost:8080/api/auth/signup -H 'Content-Type: application/json' \
  -d '{"username":"john_doe_01","email":"john_doe@example.com","password":"Passw0rd@1","userType":"Standard User"}'
# -> {"message":"...","otp":"123456"}
curl -X POST localhost:8080/api/auth/verify-otp -H 'Content-Type: application/json' \
  -d '{"email":"john_doe@example.com","otp":"123456"}'
curl -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"john_doe_01","password":"Passw0rd@1"}'
curl localhost:8080/api/dashboard -H "Authorization: Bearer <token>"      # Welcome john_doe_01
```
A verified Administrator is seeded in memory: **admin_user / Admin@123** (configurable) for testing `/api/settings`.

## Design notes
* **Passwords**: BCrypt hashed (never plain text). OTPs are also stored hashed with a 5 min expiry.
* **JWT**: HS256, claims `sub`, `role`, `jti`, `iat`, `exp` (60 min, configurable). Signature, expiry and revocation checked on every request by `JwtAuthFilter`.
* **Logout**: the token's `jti` goes into an in-memory blacklist → the token is rejected afterwards (401).
* **Users are in memory** using Spring Security's `InMemoryUserDetailsManager` (`AppConfig`) + `AuthenticationManager` → they disappear on restart. Sign-ups wait in `PendingSignupStore` until the OTP is verified, then are added to the manager. Products persist in PostgreSQL.
* **Country restriction**: `GeoLocationService` resolves the country via ip-api.com and blocks `SY, AF, IR, KP`
  (edit `app.geo.blocked-countries`). On localhost the provider uses the server's public IP.
  `app.geo.fail-open=false` rejects sign-up (503) if the lookup fails. Set `app.geo.enabled=false` to disable offline.
  Behind a proxy, make sure it sets/overwrites `X-Forwarded-For`.
* **OTP delivery**: no mail/SMS provider is wired; the OTP is logged and (demo flag `app.otp.expose-in-response=true`) returned in the response. Disable that flag in production.
* **Injection safety**: Bean Validation on all inputs, JPA Criteria (parameterised queries), whitelisted sort fields, uniform JSON errors without stack traces.

## Postman
Import `postman/JWT-Auth-Product-API.postman_collection.json`. Login requests store the token in a collection variable automatically.
Run order: Sign up → Verify OTP → Login → Dashboard / Products → Logout (then Dashboard returns 401).

## Frontend (Express + forms)
Minimal Express layer in `express-frontend/` serving three forms (Sign up, Verify OTP, Login) and proxying `/api/*` to Spring Boot.
```bash
cd express-frontend
npm install
npm start          # http://localhost:3000  (BACKEND_URL defaults to http://localhost:8080)
```
Start the Spring Boot app first. A successful login shows the Dashboard message ("Welcome <Username>").
