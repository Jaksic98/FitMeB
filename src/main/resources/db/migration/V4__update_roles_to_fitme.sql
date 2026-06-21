-- Remove user_roles entries for roles that will be deleted or renamed
DELETE FROM user_roles
WHERE role_id IN (
    SELECT id FROM roles WHERE code IN ('BUSINESS_ADMIN', 'DEPOSIT_ADMIN', 'HOV_ADMIN')
);

-- Delete unused roles
DELETE FROM roles WHERE code IN ('BUSINESS_ADMIN', 'DEPOSIT_ADMIN', 'HOV_ADMIN');

-- Rename SUPER_ADMIN → ADMIN
UPDATE roles
SET code        = 'ADMIN',
    name        = 'Administrator',
    description = 'Potpuni pristup sistemu – upravljanje korisnicima, kompeticijama i konfiguracijama'
WHERE code = 'SUPER_ADMIN';

-- Rename USER → COMPETITOR
UPDATE roles
SET code        = 'COMPETITOR',
    name        = 'Takmičar',
    description = 'Učesnik kompeticije'
WHERE code = 'USER';

-- Insert new JUDGE role
INSERT INTO roles (code, name, description)
VALUES ('JUDGE', 'Sudija', 'Bodovanje takmičara u dodeljenim kategorijama');

-- Re-seed user_roles for existing seed users
-- adam.adamovic → ADMIN + COMPETITOR
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'ADMIN'
WHERE u.username = 'adam.adamovic'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- marko.markovic → JUDGE + COMPETITOR
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'JUDGE'
WHERE u.username = 'marko.markovic'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'COMPETITOR'
WHERE u.username = 'marko.markovic'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- jovana.jovanovic, petar.petrovic, ana.anic already have COMPETITOR from V2 (USER→COMPETITOR rename)
-- adam.adamovic already has COMPETITOR from V2 as well
