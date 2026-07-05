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

- [x] `*IT` testovi za sve nove servise (Postgres via docker compose), pattern kao `UserServiceIT`/`AuthServiceIT` — `PilatesServiceIT`, `TerminServiceIT`, `AppointmentServiceIT`, `AppointmentGenerationServiceIT`.
- [x] Controller testovi sa `@MockitoBean` + `@WithMockUser(roles = "...")` za `@PreAuthorize` provere (403 za CLIENT na admin-only akcijama) — `PilatesControllerIT`, `TerminControllerIT`, `AppointmentControllerIT`.
- [x] Test 12h cancel pravila (granica: tačno 12h, 11h59m, 12h01m) — `AppointmentServiceIT` (`givenBookedAppointmentExactly12hAway...`, `givenBookedAppointment11h59mAway...`, `givenBookedAppointment12h01mAway...`).
- [x] Test termin-overlap validacije — `TerminServiceIT` (`givenOverlappingTermin_whenCreateTermin_thenThrowsTerminOverlapException`, `givenExistingTermin_whenUpdateWithOverlappingAnotherTermin_thenThrowsTerminOverlapException`).
- [x] Test idempotencije generisanja slotova — `AppointmentGenerationServiceIT.givenSlotsAlreadyGenerated_whenGenerationCalledAgain_thenNoDuplicatesAreCreated`.

Sve stavke potvrđene punim test suite-om: 79/79 testova prolazi, 0 failure/error (`mvn test`, 2026-07-02).

## Dopune postojećih modula (Talas 2)

### Dopuna Modula 1 — Zamena email verifikacije SMS-om (§9 SPEC.md)

- [x] `phoneNumber` obavezan: `@NotBlank` u `BaseUserDTO` (važi za register I admin create — odluka korisnika 2026-07-05: DB-level NOT NULL + backfill, ne samo app-level) + `V10__require_phone_and_add_phone_verified.sql` (backfill `'000000'` za NULL, pa `SET NOT NULL`). `UpdateUserRequestDTO` netaknut (patch semantika, null = bez izmene; NOT NULL kolona + null-ignore mapper garantuju da telefon ne može biti obrisan).
- [x] Novo polje `phoneVerified` (Boolean, default false, isti pattern kao `emailNotifications`) na `User` + ista V10 migracija.
- [x] `ActivationTokenService` / email-link flow ostaje u kodu ali se **ne poziva** — poziv u `AuthService.register` zakomentarisan uz napomenu; `GET /api/auth/activate` endpoint i dalje funkcionalan.
- [ ] `INACTIVE → ACTIVE` tranzicija pri uspešnoj SMS verifikaciji (poziva se iz `SmsVerificationService`, Modul 9) — admin fallback `PUT /api/users/{id}` ostaje kao i ranije.
- [x] Uzgredno (otkriveno tokom verifikacije): `mvn test` je pokretao samo `FitmeApplicationTests` — surefire 3.5.4 default includes ne hvataju `*IT` klase; dodata eksplicitna `maven-surefire-plugin` includes konfiguracija u `pom.xml`. Novi `AuthControllerIT` (register validacija telefona kroz MockMvc). Full suite 93/93 (2026-07-05).

### Dopuna Modula 5 — Membership 35 dana (§10 SPEC.md)

- [x] Novo polje `membershipExpiresAt` (LocalDate, nullable) na `User` entitetu + Flyway migracija (`V10__add_membership_expires_at.sql`).
- [x] U `AppointmentService.bookAppointment`: ako `membershipExpiresAt != null && membershipExpiresAt.isBefore(today)` → odbiti sa `MEMBERSHIP_EXPIRED` (`ErrorCode` 2708, appointment domen 27xx opseg umesto 21xx — pravilo se proverava tokom bookinga, isti blok kao `NO_REMAINING_APPOINTMENTS`). Provera važi samo za CLIENT (ADMIN bypass, isti pattern kao credit-check).
- [x] Na prvom uspešnom bookingu (pre toga `membershipExpiresAt == null`) → setovati `membershipExpiresAt = today + 35 dana`. Ovo se dešava nezavisno od toga da li je booking inicirao CLIENT ili ADMIN u ime klijenta (trigger je "prvo korišćenje", ne trigeruje se dopunom kredita).
- [x] `UserDTO` proširena sa `membershipExpiresAt` poljem.
- [x] Testovi u `AppointmentServiceIT`: prvi booking postavlja datum, drugi booking ga ne menja, isteklo članstvo blokira CLIENT ali ne i ADMIN booking.
- Otvoreno (van scope-a ove dopune, per SPEC.md §10): šta se dešava sa `membershipExpiresAt` kad istekne pa se doda novi paket — trenutno ništa ne resetuje datum niti otključava booking dok admin ručno ne interveniše.

### Dopuna Modula 4/5 — AppointmentDTO izmene (§11 SPEC.md)

- [x] `User` ima samo `fullName` (nema odvojena `firstName`/`lastName`) — nema potrebe za migracijom. Dodato `userFullName` u `AppointmentDTO`, uz zadržan `userId` (isti obrazac kao postojeći `pilatesId`+`pilatesName`, `terminId`+`terminDate/StartTime/EndTime`).
- [x] `AppointmentService.enrich`/`toDto` batch-učitavaju `User` mapu (isti pattern kao `Termin`/`Pilates` mape) umesto custom Querydsl JOIN-a — dovoljno za trenutnu skalu (admin-managed liste, ne user-scale).
- [x] `getAllAppointments()` (admin "Rezervacije" tab) filtrira na `status = BOOKED`. `getByUserId` (CLIENT "Moji termini") nije trebalo menjati — `userId` se po invarijanti servisa uvek postavlja/briše zajedno sa `BOOKED`/`AVAILABLE` statusom, pa `findAllByUserId` već vraća samo BOOKED redove.
- [x] Testovi: `AppointmentServiceIT` — `userFullName` popunjen posle booking-a, `getAllAppointments` isključuje AVAILABLE slotove. Full suite 84/84 (83 IT + `FitmeApplicationTests`).
- [x] Usput ispravljen pre-existeći flaky bug u `AppointmentServiceIT` (dva testa su računala termin end-time preko `LocalTime.now().plusMinutes(...)` bez rollover-a na sledeći dan, pa su padala kad se testovi pokrenu blizu ponoći) — dodat `capEndOfDay` helper i konsolidovano na `createAppointmentForCancelWindowTest`.

### Dopuna Modula 4/5 — Admin pretraga (§12 SPEC.md)

- [x] `UserController` — umesto zasebnog `/search` endpoint-a (kako SPEC.md labavo predlaže), prošireno postojeće `GET /api/users` (`UserSearchRequestDTO`/`UserQueryRepositoryImpl`, već ima Querydsl + paging) sa opcionim `q` poljem — Querydsl OR-predikat (`containsIgnoreCase`) preko `username`/`email`/`phoneNumber`, AND-ovan sa postojećim filterima. Izbegnuta paralelna search infrastruktura (DRY/YAGNI).
- [x] `AppointmentController` (admin) — `GET /api/appointments` prošireno sa `AppointmentSearchRequestDTO` (`userId`, `pilatesId`, `dateFrom`, `dateTo`, sve opciono) kroz `@ModelAttribute`. `AppointmentService.getAllAppointments` filtrira in-memory (isti "flat list, admin-managed" pattern kao `getAvailableAppointments`/`filterToActiveTerminAndPilates`) umesto novog Querydsl repository-ja — Appointment je i dalje admin-scale, ne user-scale.
- [x] Testovi: `UserServiceIT` (`q` pogađa telefon), `AppointmentServiceIT` (`userId`, `pilatesId`, `dateFrom`/`dateTo` filteri), `AppointmentControllerIT` mock ažuriran na novi `getAllAppointments(filter)` signature. Full IT suite 87/87.

## Modul 8 — TerminTemplate i generisanje Termina (§7 SPEC.md)

- [x] Entitet `TerminTemplate` (`id`, `dayOfWeek` kao `java.time.DayOfWeek` `@Enumerated(STRING)`, `startTime`, `endTime`; `status` nasleđen), proširuje `BaseAuditableEntity`.
- [x] Flyway migracija `V11__create_termin_template.sql` (struktura po uzoru na V8, uklj. `chk_termin_template_time_order`).
- [x] `template_id` (nullable FK → `termin_template`) na `termin` tabeli — ista V11 migracija + `Termin.templateId` (plain `Long`, isti stil kao `Appointment.terminId`).
- [x] Validacija preklapanja šablona: isti `dayOfWeek` + vremenski interval → `TerminTemplateOverlapException`; dodatno `InvalidTerminTemplateTimeRangeException` (isti par pravila kao `TerminService`). `ErrorCode` 2801–2803 (+ 2604 `TERMIN_DELETE_BLOCKED` unapred, za dopunu `deleteTermin` ispod).
- [x] `TerminTemplateRepository`, `TerminTemplateService`, `TerminTemplateController` (admin CRUD, `/api/termin-templates`), DTO-i + `TerminTemplatePatchMapper`. Soft delete kroz `Status`. Testovi: `TerminTemplateServiceIT` (10), `TerminTemplateControllerIT` (2). Full suite 105/105 (2026-07-05).
- [x] `TerminGenerationService.generateForTemplate(templateId)`: datumi u `[today, today+horizon]` sa odgovarajućim `dayOfWeek` → `TerminService.createTerminFromTemplate` + `AppointmentGenerationService.generateForTermin`. Postojanje se proverava kao `existsByTemplateIdAndDate` **bez obzira na status** (ručno obrisan generisan termin se ne "uskrsava" dnevnim job-om). Preklapanje sa ručno unetim terminom → taj datum se preskače uz `log.warn`, ostali se generišu (`createTerminFromTemplate` je namerno bez sopstvenog `@Transactional` da catch-and-skip ne markira zajedničku transakciju rollback-only). Trigger na create šablona (samo ACTIVE), isti lanac kao Modul 3/4.
- [x] Rolling horizon = `today + 90 dana`, konfigurisano kao `application.scheduling.termin-horizon-days` u `application.yaml` (potvrdio korisnik 2026-07-05, po SPEC §7.3).
- [x] `@Scheduled` daily job (`TerminScheduler`, cron `0 0 2 * * *`) + `SchedulingConfig` (`@EnableScheduling`) — `generateForAllActiveTemplates()` idempotentno popunjava klizni horizont.
- [x] Kaskadno brisanje na `deleteTerminTemplate`: budući (`date >= today`) termini šablona bez BOOKED → DELETED; termini **sa** BOOKED ostaju, ali im se preostali AVAILABLE slotovi prebacuju u CANCELED (odluka korisnika 2026-07-05: zatvoreni za nove rezervacije); prošli termini netaknuti.
- [x] Dopuna `TerminService.deleteTermin`: BOOKED Appointment na terminu → `TerminDeleteBlockedException` (`TERMIN_DELETE_BLOCKED` 2604, HTTP 409) — odluka korisnika 2026-07-05: blokiraj, admin prvo ručno otkazuje rezervacije. Testovi: `TerminGenerationServiceIT` (6), dopune `TerminTemplateServiceIT`/`TerminServiceIT`. Full suite 116/116 (2026-07-05).

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
