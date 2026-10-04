# Corporate Vending Machine Backend

Spring Boot 3.3 / Java 21 backend for an office vending machine: touchscreen or mobile-QR ordering, payment by RFID badge wallet, UPI dynamic QR, or card, motor dispensing confirmed by an IR sensor, automatic refunds, and per-slot fault isolation.

**Stack:** Spring Data JPA + PostgreSQL (Flyway), Spring Security (JWT resource server), Spring StateMachine, Spring Integration MQTT (Paho), Redis + Redisson, MapStruct, Lombok, springdoc OpenAPI.

> **Status:** this source was written and syntax-checked but has **not been compiled or run** against the real dependencies (no Maven Central access where it was generated). Expect to fix small compile or wiring issues on the first `mvn verify`.

## Run locally

Prerequisites: JDK 21, Maven 3.9+, Docker.

```bash
docker compose up -d          # PostgreSQL, Redis, Mosquitto
mvn spring-boot:run           # dev profile: Flyway also loads db/seed (1 machine, 3 slots, 1 employee)
```

Swagger UI: http://localhost:8080/swagger-ui.html

## Try the flow (dev profile)

```bash
TOKEN=$(curl -s -X POST "localhost:8080/dev/token?subject=asha&roles=EMPLOYEE,ADMIN" | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
M=11111111-1111-1111-1111-111111111111

# 1. select slot A1 (idempotent on the key)
curl -s -X POST localhost:8080/api/v1/orders -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: demo-1" -H "Content-Type: application/json" \
  -d "{\"machineId\":\"$M\",\"slotNo\":\"A1\"}"

# 2. pay with the seeded badge (hash = sha256 of BADGE-1001)
curl -s -X POST localhost:8080/api/v1/orders/<ORDER_ID>/pay -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"method":"RFID","badgeHash":"3dbb5151b832d233abf1c58a66f89c6beec3a529d0bf52b1f48d2b87b19cf9c2"}'

# 3. watch the dispense command, then simulate the IR sensor
mosquitto_sub -h localhost -t 'vm/+/cmd/#' -v
mosquitto_pub -h localhost -t "vm/$M/events/drop" -m '{"orderId":"<ORDER_ID>","result":"OK"}'
# result TIMEOUT or JAM triggers refund + slot fault accounting (3 faults disables the slot)
```

If no sensor reply arrives within `vending.order.dispense-timeout-seconds` (20 s), the watchdog treats it as a failed drop and refunds.

## Tests

`mvn test` runs `PaymentStrategyTest`, `OrderServiceTest` (Mockito) and `StateMachineFlowTest` (`@SpringBootTest` with the real state machine and mocked guards/actions). No database or broker is needed.

## Package layout

```
domain/         enums, JPA entities (domain.model), events, exceptions
application/    dto, payment (strategies), hardware (commands), service, statemachine, port.out
adapter/in/     rest (controllers, ProblemDetail advice), mqtt (inbound + observer), ws (STOMP push)
adapter/out/    persistence (repositories, wallet), mqtt (publisher), gateway (UPI/card REST clients)
config/         security, OpenAPI
```

## Deviations from the Section 2 sketch

- `REFUNDED` was added as a terminal state (REFUNDING -> REFUNDED), matching the DB check constraint.
- `OrderStateMachineService.fire(...)` returns the resulting state; a rejected event or guard returns the unchanged state.
- `RefundAction` calls a `RefundHandler` (not `OrderService`), so there is no circular dependency.
- The MQTT dispense command is published **after the DB commit** (`HardwareDispatcherService`), so the edge can never reply before the order shows DISPENSING.
- The slot "busy" marker TTL is 5 minutes (covers the payment window), not 15 s.

## Known gaps

- Offline RFID replay: the `offline_transactions` table exists, but no ingestion code is included.
- Dispense commands are not HMAC-signed; use broker TLS + per-machine ACLs.
- Gateway adapters assume a generic REST shape (`/v1/qr`, `/v1/charges`, `/v1/refunds`, webhook `{orderId, txnId, status}` with an HMAC-SHA256 `X-Signature` header). Adapt them to your PSP.
- JWT uses a shared HS256 secret. For a corporate IdP, replace the `JwtDecoder` bean with an issuer-based one.
- One item per order.

## Docker

```bash
docker build -t corporate-vending-machine .
docker run -p 8080:8080 -e DB_URL=jdbc:postgresql://host/vending -e DB_USERNAME=... -e DB_PASSWORD=... \
  -e JWT_SECRET=<32+ chars> -e UPI_WEBHOOK_SECRET=... -e REDIS_HOST=... -e MQTT_BROKER_URL=ssl://... corporate-vending-machine
```
