-- --- src/main/resources/data.sql ---

-- Customers
INSERT INTO customers (id, name, surname, credit_limit, used_credit_limit) VALUES
(1, 'John', 'Doe', 10000.00, 0.00),
(2, 'Jane', 'Smith', 15000.00, 0.00),
(3, 'Alice', 'Johnson', 12000.00, 0.00),
(4, 'Robert', 'Brown', 8000.00, 0.00),
(5, 'Emily', 'Davis', 20000.00, 0.00),
(6, 'Michael', 'Wilson', 17500.00, 0.00),
(7, 'Sarah', 'Miller', 9500.00, 0.00);

-- Users (Passwords MUST be BCRYPT HASHED)
-- Replace plain text passwords with their BCrypt hashes.
-- Example hash for 'adminpass': $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1cCTN8Xut.JbG
-- Example hash for 'customerpass': $2a$10$cMAmQI3330A4dTPOkLTVd.PoxsAg7687Xgm44L4fEZ8lX02xgh94a
-- Placeholder hash for new users: '$2a$10$REPLACEWITHBCRYPTHASH' (generate unique ones)

INSERT INTO app_users (username, password, role, customer_id) VALUES
('admin', '$2a$10$46/XZWsQd0u.8/XCfXKcDul9pmeORBwiFfEJsi9xC5nDxqhFo9ZyG', 'ROLE_ADMIN', NULL), -- Admin user, password: adminpass
('johndoe', '$2a$10$oaLKlSit.QZRi3OcT06D1uN5zrPmhB9MnKRhI1EjX73TUFtJC5TXq', 'ROLE_CUSTOMER', 1), -- Customer user, password: customerpass
('janesmith', '$2a$10$ZWMbwTS2j53.toNWDwHl6OC9dwHnHZzzsT2rcOC/.Gb57fU08InTW', 'ROLE_CUSTOMER', 2), -- Customer user, password: customerpass
('alicej', '$2a$10$ToNekRIYlyZWSx7g5vKc/.GrndNolPZk1IzojjKeNVm7rlmqbeco6', 'ROLE_CUSTOMER', 3),    -- Example password: newuserpass1
('robertb', '$2a$10$axQS9wcDnzTD8upFj47FwOY7jQinmEF51h58XywnItWmSOqpuCLRa', 'ROLE_CUSTOMER', 4),   -- Example password: newuserpass2
('emilyd', '$2a$10$QJ11rleNch.4aHga6B8JaO2maQv/y/VuDO6KM038fuX.F8x7BOuaC', 'ROLE_CUSTOMER', 5),    -- Example password: newuserpass3
('michaelw', '$2a$10$UBzAR.KgdAamlHv8csFwzOIlcBKC8Au3gws2a6hu3bRUwp8PTXhF2', 'ROLE_CUSTOMER', 6),  -- Example password: newuserpass4
('sarahm', '$2a$10$AylzW4pUl0aJlXjku0yZTObuIWZDOz.7w5j5Sb9dSd1CR3SXXuJwG', 'ROLE_CUSTOMER', 7);    -- Example password: newuserpass5


-- Make sure your application.properties or application.yaml has:
-- spring.sql.init.mode=always
-- spring.jpa.hibernate.ddl-auto=update (or create/create-drop for dev)