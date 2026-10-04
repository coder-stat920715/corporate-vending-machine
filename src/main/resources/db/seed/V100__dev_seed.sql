INSERT INTO machines(id, code, location)
VALUES ('11111111-1111-1111-1111-111111111111', 'VM-BLR-01', 'Bengaluru Tower A, Floor 3 Pantry');

INSERT INTO products(id, sku, name, category, price_paise) VALUES
 ('aaaaaaaa-0000-0000-0000-000000000001', 'SKU-COFFEE', 'Cold Coffee',   'BEVERAGE', 6000),
 ('aaaaaaaa-0000-0000-0000-000000000002', 'SKU-CHIPS',  'Salted Chips',  'SNACK',    2000),
 ('aaaaaaaa-0000-0000-0000-000000000003', 'SKU-COOKIE', 'Choco Cookies', 'SNACK',    3000);

INSERT INTO slots(machine_id, slot_no, product_id, quantity, capacity) VALUES
 ('11111111-1111-1111-1111-111111111111', 'A1', 'aaaaaaaa-0000-0000-0000-000000000001', 10, 12),
 ('11111111-1111-1111-1111-111111111111', 'A2', 'aaaaaaaa-0000-0000-0000-000000000002', 10, 12),
 ('11111111-1111-1111-1111-111111111111', 'A3', 'aaaaaaaa-0000-0000-0000-000000000003', 10, 12);

INSERT INTO employees(id, emp_code, full_name, email)
VALUES ('bbbbbbbb-0000-0000-0000-000000000001', 'E1001', 'Asha Rao', 'asha.rao@corp.example');

INSERT INTO rfid_badges(employee_id, badge_hash)
VALUES ('bbbbbbbb-0000-0000-0000-000000000001', encode(digest('BADGE-1001', 'sha256'), 'hex'));

INSERT INTO wallets(employee_id, balance_paise, credit_limit_paise)
VALUES ('bbbbbbbb-0000-0000-0000-000000000001', 50000, 10000);
