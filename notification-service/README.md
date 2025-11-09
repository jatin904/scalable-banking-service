# Notification Service

Spring Boot microservice that sends customer email notifications for banking events such as high-value transactions, account status changes, and important account updates (loan lifecycle, document uploads, bill payments, and contact information changes). The service is designed for a microservices environment with database-per-service boundaries and accepts event payloads over HTTP.

## Features
- RESTful endpoints to trigger notifications from upstream transaction/account services.
- Configurable high-value transaction thresholds with per-request overrides.
- Dual delivery channel: rich emails plus concise SMS alerts via 2Factor.in (real or mockable).
- In-memory notification audit log with a history endpoint.
- Spring Profiles & `.env` files to keep environment-specific settings isolated.
- Docker container for consistent deployment.
- Actuator health endpoint for basic observability.

## Project Layout

```
src/main/java/com/bank/notificationservice
├── controller      # REST controllers and exception handling
├── dto             # Request/response payloads
├── model           # Domain enums
├── service         # Notification composition & dispatch logic
├── support         # Shared helper records/exceptions
└── config          # Typed configuration properties
src/main/resources
├── application.yml         # Shared defaults
└── application-local.yml   # Local profile (mock delivery on)
env/
└── local.env               # Sample environment variables
```

## Configuration

Key properties live under the `notification` prefix:

| Property | Description |
| --- | --- |
| `notification.mail.from` | Default `from` address for outbound emails |
| `notification.mail.mock-delivery` | When `true`, emails are logged instead of sent |
| `notification.thresholds.high-value-transaction` | Default minimum amount that qualifies as "high value" |
| `notification.sms.api-key` | API key for calling the 2Factor.in SMS API |
| `notification.sms.base-url` | Base URL for the 2Factor.in REST API |
| `notification.sms.mock-delivery` | When `true`, SMS payloads are logged instead of sent |
| `notification.docs.server-url` | Server URL advertised inside the generated Swagger UI (defaults to `http://localhost:8080`) |

### Profiles
- `local`: Default development profile. Logs email contents instead of sending. Configured via `application-local.yml`.

Activate a profile through the `SPRING_PROFILES_ACTIVE` environment variable (e.g., `local`).

### Environment Files
Use the supplied `.env` template with Docker or your process manager:

- `env/local.env`: Uses the `local` profile with mock email & SMS delivery enabled.
- Important SMTP variables (consumed by Spring Boot): `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_SMTP_AUTH`, and `MAIL_SMTP_STARTTLS_ENABLE`.

> **Tip:** Adjust `notification.mail.mock-delivery` and `notification.sms.mock-delivery` in the env file if you want to toggle between mock and real delivery without changing property files.

### SMS Delivery (2Factor.in)
- Provide a valid `notification.sms.api-key` from your 2Factor.in account.
- Override `notification.sms.base-url` if 2Factor.in issues a different endpoint.
- SMS payloads are submitted via `GET https://2factor.in/API/V1/{apiKey}/SMS/{phone}/{message}`.
- Keep messages short (the service trims content to 140 chars automatically).

### SMS Delivery

This service supports two SMS delivery modes:

- 2Factor.in (legacy/default): when `notification.sms.api-key` and `notification.sms.base-url` are configured the service will send via the 2Factor.in API (existing behaviour).
- Twilio (preferred when configured): when you provide Twilio credentials the service will use the Twilio Java SDK to send SMS messages.

How Twilio is configured

You can provide Twilio credentials via environment variables or `env/local.env`. The application maps the following environment variable names to the configuration properties used by Spring Boot:

| Env var | Bound Spring property |
|---|---|
| `TWILIO_ACCOUNT_SID` | `notification.sms.accountSid` |
| `TWILIO_AUTH_TOKEN` | `notification.sms.authToken` |
| `TWILIO_FROM_NUMBER` | `notification.sms.fromNumber` |

The loader also accepts `NOTIFICATION_SMS_*` names for backward compatibility (for example `NOTIFICATION_SMS_ACCOUNT_SID`). Both uppercase and lowercase env keys are accepted in `env/local.env`.

Example `env/local.env` entries (placeholders - do NOT commit real secrets):

```dotenv
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_FROM_NUMBER=+1234567890

# legacy 2factor values (optional)
NOTIFICATION_SMS_API_KEY=dc474765-...
NOTIFICATION_SMS_BASE_URL=https://2factor.in/API/V1
NOTIFICATION_SMS_SENDER_ID=TFCTOR
```

Behaviour at runtime

- If `notification.sms.mock-delivery` is `true` the service will log SMS content instead of sending it.
- If `mock-delivery` is `false` and Twilio credentials (`accountSid`, `authToken`, `fromNumber`) are present the service uses Twilio.
- If `mock-delivery` is `false` and Twilio credentials are missing the service will attempt to use 2Factor.in when `notification.sms.api-key` is present, otherwise it throws a `NotificationDeliveryException` indicating missing configuration.

Message length
- SMS messages are trimmed to 140 characters by the composer. Ensure the `customerPhone` field is present in request payloads when expecting SMS delivery.

## Building

### With Docker (recommended)
```bash
docker build -t notification-service:local .
```

The multi-stage build compiles the Spring Boot application and produces a runnable container.

### With Maven (if installed locally)
```bash
mvn clean package
```

The runnable JAR is placed at `target/notification-service-0.1.0-SNAPSHOT.jar`.

## Running

### Docker
```bash
# Local/dev mode (mock delivery)
docker run --rm -p 8080:8080 --env-file env/local.env notification-service:local
```

### JVM
```bash
SPRING_PROFILES_ACTIVE=local java -jar target/notification-service-0.1.0-SNAPSHOT.jar
```

## API

Base path: `/api/notifications`

| Endpoint | Description |
| --- | --- |
| `POST /transactions/high-value` | Trigger high value transaction email |
| `POST /accounts/status-change` | Notify customers about status transitions |
| `POST /accounts/events` | Notify customers about account events (contact/documents/loan/bill changes) |
| `GET /history` | Return all email/SMS notifications sent in the last 7 days |

### API Documentation & Swagger UI

Run the service and navigate to `http://localhost:8080/swagger-ui/index.html` for an interactive view of every endpoint. The raw OpenAPI document is served at `http://localhost:8080/v3/api-docs`. If you deploy the service elsewhere, override `notification.docs.server-url` so the "Try it out" buttons point to the correct host.

Supported `eventType` values:
- `ACCOUNT_NUMBER_UPDATED`
- `CONTACT_INFORMATION_UPDATED`
- `DOCUMENT_UPDATED`
- `LOAN_TAKEN`
- `LOAN_CLEARED`
- `BILL_CLEARED`

### Sample Requests

#### High-Value Transaction
```bash
curl -X POST http://localhost:8080/api/notifications/transactions/high-value \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "123-456-789",
    "customerName": "Jane Doe",
    "customerEmail": "noreply2024tm93003@gmail.com",
    "customerPhone": "+91-9000000000",
    "txnType": "DEBIT",
    "amount": 15000,
    "currency": "INR",
    "counterparty": "Acme Stores",
    "reference": "Invoice 8821"
  }'
```

#### Account Status Change
```bash
curl -X POST http://localhost:8080/api/notifications/accounts/status-change \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "987-654-321",
    "customerName": "John Smith",
    "customerEmail": "noreply2024tm93003@gmail.com",
    "customerPhone": "+91-9000000000",
    "previousStatus": "Pending KYC",
    "currentStatus": "Active",
    "remarks": "Documents verified"
  }'
```

#### Account Event Notifications
Below are sample payloads for each supported `eventType`.

```bash
# Account number updated
curl -X POST http://localhost:8080/api/notifications/accounts/events \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "555-666-777",
    "customerName": "Mary Major",
    "customerEmail": "noreply2024tm93003@gmail.com",
    "customerPhone": "+91-9000000000",
    "eventType": "ACCOUNT_NUMBER_UPDATED",
    "description": "Your new account number is 555-666-999"
  }'

# Contact information updated
curl -X POST http://localhost:8080/api/notifications/accounts/events \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "555-666-777",
    "customerName": "Mary Major",
    "customerEmail": "noreply2024tm93003@gmail.com",
    "customerPhone": "+91-9000000000",
    "eventType": "CONTACT_INFORMATION_UPDATED",
    "description": "Mobile number updated to +91-9000000000"
  }'

# Document updated
curl -X POST http://localhost:8080/api/notifications/accounts/events \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "555-666-777",
    "customerName": "Mary Major",
    "customerEmail": "noreply2024tm93003@gmail.com",
    "customerPhone": "+91-9000000000",
    "eventType": "DOCUMENT_UPDATED",
    "description": "Latest bank statement uploaded"
  }'

# Loan taken
curl -X POST http://localhost:8080/api/notifications/accounts/events \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "555-666-777",
    "customerName": "Mary Major",
    "customerEmail": "noreply2024tm93003@gmail.com",
    "customerPhone": "+91-9000000000",
    "eventType": "LOAN_TAKEN",
    "description": "Personal loan of INR 500,000 disbursed"
  }'

# Loan cleared
curl -X POST http://localhost:8080/api/notifications/accounts/events \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "555-666-777",
    "customerName": "Mary Major",
    "customerEmail": "noreply2024tm93003@gmail.com",
    "customerPhone": "+91-9000000000",
    "eventType": "LOAN_CLEARED",
    "description": "Final EMI received on 2024-02-20"
  }'

# Bill cleared
curl -X POST http://localhost:8080/api/notifications/accounts/events \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "555-666-777",
    "customerName": "Mary Major",
    "customerEmail": "noreply2024tm93003@gmail.com",
    "customerPhone": "+91-9000000000",
    "eventType": "BILL_CLEARED",
    "description": "Electricity bill payment of INR 2,300 confirmed"
  }'
```

### Notification History
```bash
curl -X GET http://localhost:8080/api/notifications/history
```

The response is an array of entries from the past 7 days. Each object contains:
- `id`: unique identifier
- `channel`: `EMAIL` or `SMS`
- `recipient`: email address or phone number
- `subject`: populated for emails
- `preview`: truncated body preview
- `notificationType`: `HIGH_VALUE_TRANSACTION`, `ACCOUNT_STATUS_CHANGE`, or `ACCOUNT_EVENT`
- `timestamp`: ISO-8601 instant when the notification was queued

### Responses
- `202 Accepted` when the notification is queued for delivery.
- `200 OK` for high-value requests that fell below the configured threshold (email skipped).
- `400 Bad Request` when validation fails (field-level errors included).

## Testing

When Maven is available:
```bash
mvn test
```

The test suite currently validates the email template composition logic.

## Health Check
- `GET /actuator/health` returns service health.
- `GET /actuator/info` can be extended with build metadata if desired.

## Next Steps
- Integrate with a real SMTP server or email provider.
- Publish events to a message broker to decouple request handling from delivery.
- Add structured logging/observability hooks for production use.
