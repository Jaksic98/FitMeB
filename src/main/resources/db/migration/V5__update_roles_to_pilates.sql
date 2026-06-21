-- Remove user_roles entries for JUDGE (role being deleted, no successor in the pilates domain)
DELETE FROM user_roles
WHERE role_id IN (
    SELECT id FROM roles WHERE code = 'JUDGE'
);

-- Delete the JUDGE role itself
DELETE FROM roles WHERE code = 'JUDGE';

-- Rename COMPETITOR → CLIENT (same pattern as V4's USER → COMPETITOR rename)
UPDATE roles
SET code        = 'CLIENT',
    name        = 'Klijent',
    description = 'Korisnik koji rezerviše termine za pilates sprave'
WHERE code = 'COMPETITOR';
