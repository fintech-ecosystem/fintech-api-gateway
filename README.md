# fintech-api-gateway

API Gateway for the FinTech Core Platform.

The gateway is the public entrypoint for backend APIs. It routes `/api/**`
requests to domain services and handles cross-cutting concerns such as
correlation ID propagation, basic request logging, health checks, and future
authentication/rate limiting.

## Responsibilities

- Route `/api/**` requests to backend services
- Propagate `X-Correlation-Id`
- Apply basic request logging
- Expose health and gateway info endpoints
- Prepare for authentication, authorization, rate limiting, and observability

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

## Configuration

Override service targets with environment variables:

```bash
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
  http://localhost:8080/api/payments/ping
```
