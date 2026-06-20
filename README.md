# fintech-api-gateway

API Gateway for the FinTech Core Platform.

The gateway is the public entrypoint for backend APIs. It routes `/api/**`
requests to domain services and handles cross-cutting concerns such as
JWT validation, correlation ID propagation, basic request logging, and health
checks.

## Responsibilities

- Route `/api/**` requests to backend services
- Validate Bearer access tokens for protected APIs
- Propagate `X-Correlation-Id`
- Apply basic request logging
- Expose health and gateway info endpoints
- Prepare for authorization, rate limiting, and observability

## Non-Responsibilities

The gateway must not contain business logic. It must not:

- Check wallet balance
- Approve KYC
- Post ledger transactions
- Execute payments
- Approve loans
- Run settlement or reconciliation

## Routes

| Public Path | Target Service | Default URI |
| --- | --- | --- |
| `/api/auth/**` | auth-service | `http://localhost:8081` |
| `/api/users/**` | user-service | `http://localhost:8082` |
| `/api/kyc/**` | kyc-service | `http://localhost:8083` |
| `/api/wallets/**` | wallet-service | `http://localhost:8084` |
| `/api/ledger/**` | ledger-service | `http://localhost:8085` |
| `/api/payments/**` | payment-service | `http://localhost:8086` |
| `/api/risk/**` | risk-service | `http://localhost:8087` |
| `/api/loans/**` | loan-service | `http://localhost:8088` |

Each route uses `StripPrefix=1`, so `/api/payments/ping` is forwarded to the
payment service as `/payments/ping`.

## Security

The gateway is an OAuth2 resource server using HS256 JWT validation. Public
endpoints are:

- `/api/auth/**`
- `/actuator/health`
- `/actuator/info`
- `/gateway/info`

All other `/api/**` routes require `Authorization: Bearer <access-token>`.
Use the same `JWT_ISSUER` and `JWT_SECRET` values in auth-service and gateway
so tokens issued by auth-service can be validated at the gateway.

## Configuration

Override service targets with environment variables:

```bash
JWT_ISSUER=fintech-auth-service
JWT_SECRET=change-me-to-a-long-random-secret-at-least-32-chars

AUTH_SERVICE_URI=http://localhost:8081
USER_SERVICE_URI=http://localhost:8082
KYC_SERVICE_URI=http://localhost:8083
WALLET_SERVICE_URI=http://localhost:8084
LEDGER_SERVICE_URI=http://localhost:8085
PAYMENT_SERVICE_URI=http://localhost:8086
RISK_SERVICE_URI=http://localhost:8087
LOAN_SERVICE_URI=http://localhost:8088
```

## Run Locally

```bash
./mvnw spring-boot:run
```

## Health Check

```bash
curl http://localhost:8080/actuator/health
```

## Gateway Info

```bash
curl http://localhost:8080/gateway/info
```

## Example Request

```bash
curl -H "X-Correlation-Id: test-corr-001" \
  -H "Authorization: Bearer <access-token>" \
  http://localhost:8080/api/payments/ping
```
