CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE machines (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(32) NOT NULL UNIQUE,
    location        VARCHAR(128) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ONLINE'
                    CHECK (status IN ('ONLINE','OFFLINE','OUT_OF_SERVICE')),
    last_heartbeat  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku         VARCHAR(40) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    category    VARCHAR(20) NOT NULL CHECK (category IN ('SNACK','BEVERAGE')),
    price_paise BIGINT NOT NULL CHECK (price_paise > 0),
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE slots (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id       UUID NOT NULL REFERENCES machines(id),
    slot_no          VARCHAR(6) NOT NULL,
    product_id       UUID REFERENCES products(id),
    quantity         INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    capacity         INT NOT NULL CHECK (capacity > 0),
    status           VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'
                     CHECK (status IN ('ACTIVE','FAULTY','DISABLED')),
    fault_count      INT NOT NULL DEFAULT 0,
    version          BIGINT NOT NULL DEFAULT 0,
    UNIQUE (machine_id, slot_no),
    CHECK (quantity <= capacity)
);
CREATE INDEX idx_slots_machine_status ON slots(machine_id, status);

CREATE TABLE employees (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    emp_code    VARCHAR(20) NOT NULL UNIQUE,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(120) NOT NULL UNIQUE,
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE rfid_badges (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id  UUID NOT NULL REFERENCES employees(id),
    badge_hash   CHAR(64) NOT NULL UNIQUE,
    status       VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'
                 CHECK (status IN ('ACTIVE','BLOCKED','LOST')),
    offline_cap_paise BIGINT NOT NULL DEFAULT 20000
);
CREATE INDEX idx_badges_employee ON rfid_badges(employee_id);

CREATE TABLE wallets (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id   UUID NOT NULL UNIQUE REFERENCES employees(id),
    balance_paise BIGINT NOT NULL DEFAULT 0,
    credit_limit_paise BIGINT NOT NULL DEFAULT 0,
    version       BIGINT NOT NULL DEFAULT 0,
    CHECK (balance_paise + credit_limit_paise >= 0)
);

CREATE TABLE orders (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key  VARCHAR(64) NOT NULL UNIQUE,
    machine_id       UUID NOT NULL REFERENCES machines(id),
    slot_id          UUID NOT NULL REFERENCES slots(id),
    product_id       UUID NOT NULL REFERENCES products(id),
    employee_id      UUID REFERENCES employees(id),
    amount_paise     BIGINT NOT NULL CHECK (amount_paise > 0),
    state            VARCHAR(20) NOT NULL
                     CHECK (state IN ('IDLE','ITEM_SELECTED','PAYMENT_PENDING',
                       'DISPENSING','DISPENSED','REFUNDING','REFUNDED','OUT_OF_SERVICE')),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_orders_machine_state ON orders(machine_id, state);
CREATE INDEX idx_orders_state_updated ON orders(state, updated_at);
CREATE INDEX idx_orders_employee_time ON orders(employee_id, created_at DESC);

CREATE TABLE wallet_transactions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id     UUID NOT NULL REFERENCES wallets(id),
    order_id      UUID,
    type          VARCHAR(10) NOT NULL CHECK (type IN ('DEBIT','CREDIT','REFUND')),
    amount_paise  BIGINT NOT NULL CHECK (amount_paise > 0),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_wtx_wallet_time ON wallet_transactions(wallet_id, created_at DESC);
CREATE UNIQUE INDEX uq_wtx_order_type ON wallet_transactions(order_id, type) WHERE order_id IS NOT NULL;

CREATE TABLE payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL REFERENCES orders(id),
    method          VARCHAR(10) NOT NULL CHECK (method IN ('RFID','UPI','CARD')),
    status          VARCHAR(12) NOT NULL
                    CHECK (status IN ('INITIATED','SUCCESS','FAILED','REFUNDED')),
    gateway_txn_id  VARCHAR(64),
    amount_paise    BIGINT NOT NULL CHECK (amount_paise > 0),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (gateway_txn_id)
);
CREATE INDEX idx_payments_order ON payments(order_id);

CREATE TABLE dispense_commands (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID NOT NULL UNIQUE REFERENCES orders(id),
    machine_id  UUID NOT NULL REFERENCES machines(id),
    slot_no     VARCHAR(6) NOT NULL,
    status      VARCHAR(10) NOT NULL DEFAULT 'SENT'
                CHECK (status IN ('SENT','CONFIRMED','FAILED')),
    sent_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    result_at   TIMESTAMPTZ
);

CREATE TABLE order_state_audit (
    id          BIGSERIAL PRIMARY KEY,
    order_id    UUID NOT NULL REFERENCES orders(id),
    from_state  VARCHAR(20),
    to_state    VARCHAR(20) NOT NULL,
    event       VARCHAR(30) NOT NULL,
    at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_order ON order_state_audit(order_id, at);

CREATE TABLE offline_transactions (
    id           UUID PRIMARY KEY,
    machine_id   UUID NOT NULL REFERENCES machines(id),
    badge_hash   CHAR(64) NOT NULL,
    amount_paise BIGINT NOT NULL,
    occurred_at  TIMESTAMPTZ NOT NULL,
    reconciled   BOOLEAN NOT NULL DEFAULT FALSE
);
