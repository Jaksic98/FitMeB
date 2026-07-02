# PLAN.md — Plan implementacije (Pilates rezervacije)

Prati implementaciju modula opisanih u `SPEC.md`. Ažurirati status kako se modul završava.

Status legenda: `[ ]` nije početo, `[~]` u toku, `[x]` završeno.

## Modul 0 — Pivot postojećeg koda

- [x] `Role` enum: ukloniti `JUDGE`/`COMPETITOR`, dodati `CLIENT`. Provući kroz seed (`V5__update_roles_to_pilates.sql`), `UserService.syncUserRoles`, testove koji referenciraju stare role.
- [x] `ApiPaths`: ukloniti `COMPETITIONS`/`CATEGORIES`/`SCORES`/`LEADERBOARD`, dodati `PILATES`/`TERMINI`/`APPOINTMENTS`.
- [x] Provera `ErrorCode` za competition-domen unose koji se vezuju na uklonjene path-ove/role — potvrđeno: nema takvih unosa, nula izmena (van scope-a je heritage `PRAVNO_LICE_*`/`TIP_PRAVNOG_LICA_*`, ostaje netaknuto).
- [x] Nova Flyway migracija `V5__update_roles_to_pilates.sql` za role tabelu (brisanje JUDGE, rename COMPETITOR→CLIENT).
- [x] Uzgredno (otkriveno tokom verifikacije, van obima Role/ApiPaths ali blokiralo sve testove koji kreiraju korisnika): `AuditLog`/`AuditLogService` koristili Jackson 3 (`tools.jackson.*`) tipove za JSONB kolone, a Hibernate ORM 7.2.1 (bundlovan sa Spring Boot 4.0.2) podržava samo Jackson 2 (`com.fasterxml.jackson.*`) format mapper — usklađeno na Jackson 2 u oba fajla.
- [x] Uzgredno: popravljena 3 preegzistentna bug-a u `UserService` otkrivena tokom test verifikacije — `getUser` nije filtrirao `DELETED` status, `deleteUser` poruka u testu nije odgovarala stvarnoj (srpskoj) poruci, i jedan test (`givenExistingActiveUsername_whenCreateUserWithSameUsername...`) je bio u kontradikciji sa dva druga testa i sa dokumentovanim partial-unique-index pravilom — uklonjen kao outlier.

## Modul 1 — User proširenje + registracija/aktivacija

- [x] `User` entitet: `phoneNumber`, `remainingAppointments` (Integer, default 0), `emailNotifications` (Boolean, default true), `calendarNotifications` (Boolean, default true).
- [x] Flyway migracija za nove kolone (`V6__add_user_profile_fields.sql`).
- [x] DTO-i (`CreateUserRequestDTO`/`UpdateUserRequestDTO`/response `UserDTO`, `phoneNumber` podignut u `BaseUserDTO`) — `UserPatchMapper` nije trebalo dirati, MapStruct već mapira nova polja po imenu.
- [x] Public registracija endpoint `POST /api/auth/register` (`RegisterRequestDTO`) — interno mapira na `UserService.createUser` sa praznom listom rola (`syncUserRoles` već uvek dodaje `CLIENT`), pa nema duplirane logike.
- [x] Email verifikacija: bez nove kolone/tabele — `ActivationTokenService` generiše stateless JWT (potpisan istim `SECRET_KEY`, claim `purpose=ACCOUNT_ACTIVATION`, 24h važenje); `GET /api/auth/activate?token=...` prebacuje `INACTIVE → ACTIVE` (idempotentno, bez greške ako je nalog već aktivan). Slanje mejla je log-only stub (`AuthService.register` loguje aktivacioni link) — stvarna SMTP integracija ostaje van scope-a (Modul 7 / §6 notification servis).
- [x] Potvrđeno postojećim testom (`UserServiceIT`/`AuthServiceIT`) da admin `PUT /api/users/{id}` i dalje radi kao alternativni put do `ACTIVE`.

## Modul 2 — Pilates CRUD

- [x] Entitet `Pilates` (`id`, `position`, `name`, `status`), proširuje `BaseAuditableEntity`.
- [x] Flyway migracija `pilates` tabele (`V7__create_pilates.sql`).
- [x] `PilatesRepository`, `PilatesService`, `PilatesController` (admin-only CRUD), DTO-i (`PilatesDTO`/`CreatePilatesRequestDTO`/`UpdatePilatesRequestDTO` + `PilatesPatchMapper`). Bez Querydsl dinamičke pretrage/paginacije — lista sprava je mala, admin-managed, pa je prost `getAllPilates()` dovoljan (YAGNI).
- [x] Soft delete kroz `Status` (isti pattern kao `UserService.deleteUser`).
- [x] Trigger generisanja Appointment slotova na create — `PilatesService.createPilates` zove `AppointmentGenerationService.generateForPilates` (vidi Modul 4).

## Modul 3 — Termin CRUD

- [x] Entitet `Termin` (`id`, `date`, `startTime`, `endTime`, `status`), proširuje `BaseAuditableEntity`.
- [x] Flyway migracija `termin` tabele (`V8__create_termin.sql`, sa `chk_termin_time_order CHECK (end_time > start_time)` kao defense-in-depth uz app-level validaciju).
- [x] Validacija preklapanja vremenskih intervala istog dana (create/update) — `TerminOverlapException`/`ErrorCode.TERMIN_OVERLAP`; dodatno i `InvalidTerminTimeRangeException`/`ErrorCode.TERMIN_INVALID_TIME_RANGE` za `endTime <= startTime` (nije bilo eksplicitno traženo, ali sprečava nelogične termine koji bi pokvarili overlap-check i buduće generisanje slotova).
- [x] `TerminRepository`, `TerminService`, `TerminController` (admin-only CRUD), DTO-i (`TerminDTO`/`CreateTerminRequestDTO`/`UpdateTerminRequestDTO` + `TerminPatchMapper`).
- [x] Soft delete kroz `Status`.
- [x] Trigger generisanja Appointment slotova na create — `TerminService.createTermin` zove `AppointmentGenerationService.generateForTermin` (vidi Modul 4).

## Modul 4 — Appointment generisanje slotova

- [x] Entitet `Appointment` (`id`, `terminId`, `pilatesId`, `userId` nullable, `status` enum AVAILABLE/BOOKED/CANCELED). Ne proširuje `BaseAuditableEntity` (nije traženo u SPEC.md — istorija booking/cancel akcija ide kroz `AuditLogService` u Modulu 5, ne kroz entity-level created/updated kolone).
- [x] Flyway migracija `appointment` tabele (`V9__create_appointment.sql`) + unique constraint `uk_appointment_termin_pilates (termin_id, pilates_id)` + FK na `termin`/`pilates`/`users`.
- [x] `AppointmentGenerationService.generateForTermin`/`generateForPilates`: za dati Termin ili Pilates, insertuje AVAILABLE redove za sve ACTIVE kombinacije sa druge strane koje fale (`existsByTerminIdAndPilatesId` provera pre insert-a). Pozvano iz `PilatesService.createPilates` i `TerminService.createTermin` (samo kad je novosačuvani red ACTIVE, što je trenutno uvek slučaj na create).
- [x] Idempotencija — `AppointmentGenerationServiceIT` testira da uzastopni pozivi `generateForTermin`/`generateForPilates` ne prave duplikate (osigurano i DB unique constraint-om kao defense-in-depth).

## Modul 5 — Appointment booking/cancel/reschedule

- [x] `AppointmentRepository`/`AppointmentService`/`AppointmentController`.
- [x] `getByUserId` — vraća appointmente ulogovanog korisnika (CLIENT, ownership provera) ili bilo kog korisnika (ADMIN). `GET /api/appointments/user/{userId}`.
- [x] `create` (booking): provera `remainingAppointments > 0`, slot je AVAILABLE → BOOKED + userId, `remainingAppointments -= 1`. Novi `ErrorCode`-ovi: `NO_REMAINING_APPOINTMENTS`, `APPOINTMENT_NOT_AVAILABLE`. `POST /api/appointments` sa `BookAppointmentRequestDTO { appointmentId, userId? }` — `userId` je obavezan za ADMIN (nema "self" koncept), ignorisan/zamenjen sa sopstvenim ID-om za CLIENT.
- [x] `update` (cancel/reschedule): 12h-pre-termina provera (`APPOINTMENT_CANCEL_WINDOW_EXPIRED`), cancel → AVAILABLE/null bez refund-a, reschedule → atomska zamena starog/novog slota bez trošenja kredita. `PUT /api/appointments/{id}` sa `UpdateAppointmentRequestDTO { targetAppointmentId? }` — null = cancel, postavljeno = reschedule. Dodatno: `APPOINTMENT_NOT_BOOKED` (logička invarijanta, važi za oba role-a — ne može se cancel/reschedule-ovati appointment koji nije BOOKED).
- [x] Admin full CRUD bez ograničenja gore navedenih pravila (bez 12h, bez credit logike) — `@PreAuthorize("hasAnyRole('ADMIN','CLIENT')")` na getByUserId/create/update, CLIENT akcije provere ownership-a kroz `AppointmentOwnershipException`. Dva dizajn-pitanja rešena u dogovoru s korisnikom: (1) dodat `GET /api/appointments/available` (CLIENT-dostupan, opcioni `date` filter) jer SPEC.md inače ne ostavlja klijentu nikakav način da dobije `appointmentId` za booking; (2) dodat `DELETE /api/appointments/{id}` (admin-only, trajno briše red) iako Appointment model normalno pretpostavlja da pre-generisani slot uvek postoji — admin eksplicitno može da ga ukloni za ručnu korekciju.
- [x] `AuditLogService.logCreate/logUpdate` pozivi za booking/cancel/reschedule (audit trail, isti pattern kao User). Uzgredno otkriven i popravljen pre-egzistentni bug: `AuditLogService` je koristio ručno instanciran `ObjectMapper` bez JSR-310 modula, pa je serijalizacija bilo kog DTO-a sa `LocalDate`/`LocalTime` poljem (npr. novog `AppointmentDTO`) pucala — dodat `jackson-datatype-jsr310` (2.20.2, uskladjen sa Jackson 2 verzijom koju Spring Boot 4/Jackson 3 setup već povlači transitivno preko `jjwt-jackson`) i registrovan `JavaTimeModule`.

## Modul 6 — Testovi

- [ ] `*IT` testovi za sve nove servise (Postgres via docker compose), pattern kao `UserServiceIT`/`AuthServiceIT`.
- [ ] Controller testovi sa `@MockitoBean` + `@WithMockUser(roles = "...")` za `@PreAuthorize` provere (403 za CLIENT na admin-only akcijama).
- [x] Test 12h cancel pravila (granica: tačno 12h, 11h59m, 12h01m) — `AppointmentServiceIT` (`givenBookedAppointmentExactly12hAway...`, `givenBookedAppointment11h59mAway...`, `givenBookedAppointment12h01mAway...`).
- [ ] Test termin-overlap validacije.
- [ ] Test idempotencije generisanja slotova.

## Dopune postojećih modula (Talas 2)

### Dopuna Modula 1 — Zamena email verifikacije SMS-om (§9 SPEC.md)

- [ ] `phoneNumber` postaje obavezno polje pri registraciji (`@NotBlank` u `RegisterRequestDTO`, Flyway migracija `NOT NULL` ako kolona nije već NOT NULL).
- [ ] Novo polje `phoneVerified` (boolean, default false) na `User` entitetu + Flyway migracija.
- [ ] `ActivationTokenService` / email-link flow ostaje u kodu ali se **ne poziva** — komentariše se poziv u `AuthService.register` uz napomenu da ga zamenjuje SMS OTP (Modul 9).
- [ ] `INACTIVE → ACTIVE` tranzicija pri uspešnoj SMS verifikaciji (poziva se iz `SmsVerificationService`, Modul 9) — admin fallback `PUT /api/users/{id}` ostaje kao i ranije.

### Dopuna Modula 5 — Membership 35 dana (§10 SPEC.md)

- [ ] Novo polje `membershipExpiresAt` (LocalDate, nullable) na `User` entitetu + Flyway migracija.
- [ ] U `AppointmentService.bookAppointment`: ako `membershipExpiresAt != null && membershipExpiresAt.isBefore(today)` → odbiti sa `MEMBERSHIP_EXPIRED` (novi `ErrorCode` u 21xx opsegu).
- [ ] Na prvom uspešnom bookingu (pre toga `membershipExpiresAt == null`) → setovati `membershipExpiresAt = today + 35 dana`.
- [ ] `UserDTO` proširiti sa `membershipExpiresAt` poljem.

### Dopuna Modula 4/5 — AppointmentDTO izmene (§11 SPEC.md)

- [ ] Proveriti da li `User` ima `firstName`/`lastName` ili samo `fullName` — trenutno je `fullName` jedino polje; dodati `userFullName` u `AppointmentDTO` (join na `users` tabelu).
- [ ] `AppointmentRepository` query za `getAvailableAppointments` i `getByUserId` da uključi `userFullName` — verovatno kroz custom `@Query` sa JOIN ili projekciju.
- [ ] Admin "Rezervacije" i CLIENT "Moji termini" endpointi po defaultu vraćaju samo `status = BOOKED` (dodati filter u service ili opcioni `status` query param).

### Dopuna Modula 4/5 — Admin pretraga (§12 SPEC.md)

- [ ] `UserController` — search endpoint (`GET /api/users/search?q=...`) koji pretražuje po `username`, `email`, `phoneNumber` (simple `ILIKE` ili Querydsl `ContainsIgnoreCase`).
- [ ] `AppointmentController` (admin) — filter parametri na `GET /api/appointments`: `userId`, `pilatesId`, `dateFrom`, `dateTo` (opciono). Implementirati kao Querydsl ili dinamičke `Specification<Appointment>`.

## Modul 8 — TerminTemplate i generisanje Termina (§7 SPEC.md)

- [ ] Entitet `TerminTemplate` (`id`, `dayOfWeek`, `startTime`, `endTime`, `status`), proširuje `BaseAuditableEntity`.
- [ ] Flyway migracija `termin_template` tabele.
- [ ] Flyway migracija: dodati `template_id` (nullable FK → `termin_template`) na `termin` tabelu.
- [ ] Validacija preklapanja šablona: isti `dayOfWeek` + vremenski interval → `TerminTemplateOverlapException` (novi `ErrorCode` u 28xx opsegu).
- [ ] `TerminTemplateRepository`, `TerminTemplateService`, `TerminTemplateController` (admin CRUD), DTO-i. Soft delete kroz `Status`.
- [ ] `TerminGenerationService.generateForTemplate(template)`: za svaki budući datum u rolling horizonu koji pada na `template.dayOfWeek` → insert `Termin` ako `(templateId, date)` već ne postoji. Poziva `AppointmentGenerationService.generateForTermin` za svaki insertovan termin (isti lanac kao Modul 3/4).
- [ ] Rolling horizon = `today + 90 dana`; vrednost konfigurisati kao `application.scheduling.termin-horizon-days` u `application.properties`.
- [ ] `@Scheduled` daily job (`TerminScheduler`, 02:00) — iterira sve ACTIVE `TerminTemplate` i poziva `generateForTemplate` (idempotentno popunjava klizni horizont).
- [ ] Kaskadno brisanje na `deleteTerminTemplate`: svi **budući** `Termin` vezani za `templateId` bez BOOKED Appointmenta → status DELETED; prošli termini i termini sa BOOKED slotovima ostaju nepromenjeni.
- [ ] Dopuna `TerminService.deleteTermin`: pre soft-delete provera BOOKED Appointmenta na tom terminu (ne brisati ako postoje BOOKED — ili upozoriti admina, odluka za review).

## Modul 9 — SMS/WhatsApp notifikacije (Infobip) (§8 SPEC.md)

- [ ] Dodati Infobip Java SDK (ili koristiti `RestClient` direktno na Infobip REST API bez SDK-a — odlučiti tokom implementacije).
- [ ] Konfiguracija: `infobip.api-key`, `infobip.base-url` iz `.env` / `application.properties`.
- [ ] **SMS OTP verifikacija** (`SmsVerificationService`):
  - `POST /api/auth/phone/send-otp` — generiše 6-cifreni OTP (SecureRandom), šalje SMS putem Infobip, pamti hash OTP-a + expiry (5 min) kao kolone na `users` tabeli; vraća 204.
  - `POST /api/auth/phone/verify-otp { code }` — proverava kod, na success: `user.phoneVerified = true`, `user.status = ACTIVE` → `AuthService.activateByPhone(userId)`.
  - Flyway migracija: dodati `otp_hash`, `otp_expires_at` kolone na `users`.
- [ ] **WhatsApp podsetnici** (`AppointmentReminderService`):
  - Entitet `AppointmentReminder` (`id`, `appointmentId`, `type` [DAY_BEFORE/HOUR_BEFORE], `scheduledAt`, `sentAt` nullable) + Flyway migracija.
  - Na `bookAppointment` → insert dva `AppointmentReminder` reda (24h i 1h pre `Termin.startTime`).
  - Na cancel/reschedule → obrisati neslan-e `AppointmentReminder` redove za taj appointment.
  - `@Scheduled` job svakih 5 min (`ReminderScheduler`): pronađi `AppointmentReminder` gde `scheduledAt <= now AND sentAt IS NULL` → pošalji WhatsApp poruku putem Infobip → setuj `sentAt = now`.

## Modul 7 — Van scope-a (buduće, ne implementirati sada)

- [ ] Email notifikacije (slanje na mejl pri promeni appointment statusa).
- [ ] Kalendar integracija (QR kod / push notifikacija za dodavanje u kalendar telefona).

## Dokumentacija

- [x] Dodati referencu na `SPEC.md` i `PLAN.md` u `CLAUDE.md` (sekcija sa pregledom arhitekture).
- [x] Dodati referencu na frontend (`FitmeF/DESIGN.md` kao izvor istine za UI, `FitmeF/CLAUDE.md`/`PLAN.md` za build plan) u `CLAUDE.md` → Architecture.
- [ ] Ažurirati `CLAUDE.md` heritage-note pošto Modul 0 ukloni competition-domen ostatke iz `ApiPaths`/`Role`.
