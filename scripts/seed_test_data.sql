-- Test data seed script (NOT a Flyway migration).
-- Run manually against the dev database only, e.g.:
--   source .env && psql "$POSTGRES_URL" -f scripts/seed_test_data.sql
-- Safe to re-run: every insert is guarded so it won't create duplicates.
--
-- Creates 2 pilates apparatuses ("sprave"), 4 termini/day (16:00-20:00, hourly)
-- on every weekday from the 1st of last month through the end of next month
-- (relative to CURRENT_DATE), one appointment per (termin, pilates) pair,
-- mostly-booked appointments for last month and mostly-open ones for the
-- current/future months, appointment_reminder rows for booked appointments,
-- and a random 4/8/12 remaining_appointments credit for the 5 seed users.

BEGIN;

-- 1. Sprave (pilates apparatuses)
INSERT INTO pilates (position, name, status, created_by, created_at, updated_by, updated_at)
SELECT v.position, v.name, 1, 'seed-script', now(), 'seed-script', now()
FROM (VALUES
    ('Leva strana sale', 'Reformer 1'),
    ('Desna strana sale', 'Reformer 2')
) AS v(position, name)
WHERE NOT EXISTS (SELECT 1 FROM pilates p WHERE p.name = v.name);

-- 2. Termini: every weekday (Mon-Fri), 16:00-17:00, 17:00-18:00, 18:00-19:00, 19:00-20:00,
--    from the 1st of last month through the last day of next month.
INSERT INTO termin (date, start_time, end_time, status, created_by, created_at, updated_by, updated_at)
SELECT d.date, s.start_time, s.start_time + interval '1 hour', 1, 'seed-script', now(), 'seed-script', now()
FROM generate_series(
    date_trunc('month', CURRENT_DATE - interval '1 month')::date,
    (date_trunc('month', CURRENT_DATE + interval '2 months') - interval '1 day')::date,
    interval '1 day'
) AS d(date)
CROSS JOIN (VALUES ('16:00'::time), ('17:00'::time), ('18:00'::time), ('19:00'::time)) AS s(start_time)
WHERE EXTRACT(ISODOW FROM d.date) BETWEEN 1 AND 5
  AND NOT EXISTS (
    SELECT 1 FROM termin t WHERE t.date = d.date::date AND t.start_time = s.start_time
  );

-- 3. Appointments: one row per (termin, pilates) pair for the seeded sprave/date range.
--    Last month: ~80% BOOKED (random seed user). Current/next month: ~20% BOOKED, rest AVAILABLE.
WITH seed_users AS (
    SELECT id FROM users WHERE email IN (
        'adam.adamovic@fitme.com',
        'marko.markovic@fitme.com',
        'jovana.jovanovic@fitme.com',
        'petar.petrovic@fitme.com',
        'ana.anic@fitme.com'
    )
),
candidates AS (
    SELECT t.id AS termin_id, p.id AS pilates_id, t.date AS termin_date
    FROM termin t
    CROSS JOIN pilates p
    WHERE p.name IN ('Reformer 1', 'Reformer 2')
      AND t.date BETWEEN date_trunc('month', CURRENT_DATE - interval '1 month')::date
                      AND (date_trunc('month', CURRENT_DATE + interval '2 months') - interval '1 day')::date
      AND NOT EXISTS (
        SELECT 1 FROM appointment a WHERE a.termin_id = t.id AND a.pilates_id = p.id
      )
)
INSERT INTO appointment (termin_id, pilates_id, user_id, status, version)
SELECT
    c.termin_id,
    c.pilates_id,
    CASE WHEN r.roll < CASE WHEN c.termin_date < date_trunc('month', CURRENT_DATE)::date THEN 0.8 ELSE 0.2 END
         THEN u.id ELSE NULL END,
    CASE WHEN r.roll < CASE WHEN c.termin_date < date_trunc('month', CURRENT_DATE)::date THEN 0.8 ELSE 0.2 END
         THEN 'BOOKED' ELSE 'AVAILABLE' END,
    0
FROM candidates c
CROSS JOIN LATERAL (SELECT random() AS roll) r
CROSS JOIN LATERAL (SELECT id FROM seed_users ORDER BY random() LIMIT 1) u;

-- 4. Appointment reminders for booked appointments in the seeded range (DAY_BEFORE + HOUR_BEFORE).
--    sent_at is populated when the reminder time has already passed, NULL otherwise (still pending).
INSERT INTO appointment_reminder (appointment_id, type, scheduled_at, sent_at)
SELECT a.id, rt.type, (t.date + t.start_time) - rt.lead,
       CASE WHEN (t.date + t.start_time) - rt.lead <= now() THEN (t.date + t.start_time) - rt.lead ELSE NULL END
FROM appointment a
JOIN termin t ON t.id = a.termin_id
JOIN pilates p ON p.id = a.pilates_id
CROSS JOIN (VALUES ('DAY_BEFORE', interval '1 day'), ('HOUR_BEFORE', interval '1 hour')) AS rt(type, lead)
WHERE p.name IN ('Reformer 1', 'Reformer 2')
  AND a.status = 'BOOKED'
  AND t.date BETWEEN date_trunc('month', CURRENT_DATE - interval '1 month')::date
                  AND (date_trunc('month', CURRENT_DATE + interval '2 months') - interval '1 day')::date
  AND NOT EXISTS (
    SELECT 1 FROM appointment_reminder ar WHERE ar.appointment_id = a.id AND ar.type = rt.type
  );

-- 5. Give the 5 seed users a random 4/8/12 remaining_appointments credit.
UPDATE users
SET remaining_appointments = (ARRAY[4, 8, 12])[1 + floor(random() * 3)::int]
WHERE email IN (
    'adam.adamovic@fitme.com',
    'marko.markovic@fitme.com',
    'jovana.jovanovic@fitme.com',
    'petar.petrovic@fitme.com',
    'ana.anic@fitme.com'
);

COMMIT;
