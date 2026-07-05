INSERT INTO users (username, full_name, email, password, status) VALUES
('adam.adamovic', 'Adam Adamović', 'adam.adamovic@fitme.com', '$2b$10$mIO6USmhOnzy4aJT3RCUyO34fWSJnOYHPPUw8frWlzP2J7gS3ORoO', 1),
('marko.markovic', 'Marko Marković', 'marko.markovic@fitme.com', '$2b$10$NDrXpXbuIaPXlF/LkoGs8.OqI9dsm8HYfkkW3OscCZdkMrFTvpbOe', 1),
('jovana.jovanovic', 'Jovana Jovanović', 'jovana.jovanovic@fitme.com', '$2b$10$LcgxkrTS1i4IsvRq5dk3WOgMYEPfeBrsLFyPSiG.Qu4QXST1sT8fy', 1),
('petar.petrovic', 'Petar Petrović', 'petar.petrovic@fitme.com', '$2b$10$ZvqE4RBc/AGHnpS5Fzdjleb7KeIUtOqlOMq3vv6g07o6gXDO1.YO.', 1),
('ana.anic', 'Ana Anić', 'ana.anic@fitme.com', '$2b$10$Fk9xL1Dv2w52/tTLd9VLceeBBEzUznqMuj.jLukPu1qIWoBK/g4/e', 1);

INSERT INTO roles (code, name, description) VALUES
('SUPER_ADMIN', 'Administrator sistema', 'Potpuni pristup sistemu'),
('BUSINESS_ADMIN', 'Biznis admin sistema', 'Šifarnici, depoziti, HoV, kursna lista, izveštaji'),
('DEPOSIT_ADMIN', 'Admin depozita', 'Unos i izmena podataka o depozitima'),
('HOV_ADMIN', 'Admin HoV', 'Unos i izmena podataka o HoV'),
('USER', 'Pregled podataka i izveštaja', 'Pregled šifarnika i pokretanje izveštaja');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'USER'
WHERE u.status <> 2;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'SUPER_ADMIN'
WHERE u.username = 'adam.adamovic'
  AND u.status <> 2;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'BUSINESS_ADMIN'
WHERE u.username = 'marko.markovic'
  AND u.status <> 2;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'DEPOSIT_ADMIN'
WHERE u.username = 'jovana.jovanovic'
  AND u.status <> 2;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'HOV_ADMIN'
WHERE u.username = 'petar.petrovic'
  AND u.status <> 2;
