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

- [ ] `User` entitet: `phoneNumber`, `remainingAppointments` (Integer, default 0), `emailNotifications` (boolean, default true), `calendarNotifications` (boolean, default true).
- [ ] Flyway migracija za nove kolone.
- [ ] DTO-i (`CreateUserRequestDTO`/`UpdateUserRequestDTO`/response DTO) + `UserPatchMapper` (MapStruct) update za nova polja.
- [ ] Public registracija endpoint (ako ne postoji već pod `/api/auth`) — kreira `User` sa `status = INACTIVE`, role `CLIENT`.
- [ ] Email verifikacija: token generisanje/slanje (odlučiti mehanizam — kolona na `User` vs. posebna tabela), endpoint za potvrdu koji prebacuje `INACTIVE → ACTIVE`.
- [ ] Potvrditi da postojeći admin `PUT /api/users/{id}` (status promena) radi i dalje kao alternativni put do `ACTIVE` (bez izmene, samo regresioni test).

## Modul 2 — Pilates CRUD

- [ ] Entitet `Pilates` (`id`, `position`, `name`, `status`), proširuje `BaseAuditableEntity`.
- [ ] Flyway migracija `pilates` tabele.
- [ ] `PilatesRepository`, `PilatesService`, `PilatesController` (admin-only CRUD), DTO-i.
- [ ] Soft delete kroz `Status` (isti pattern kao `UserService.deleteUser`).
- [ ] Trigger generisanja Appointment slotova na create (vidi Modul 4).

## Modul 3 — Termin CRUD

- [ ] Entitet `Termin` (`id`, `date`, `startTime`, `endTime`, `status`), proširuje `BaseAuditableEntity`.
- [ ] Flyway migracija `termin` tabele.
- [ ] Validacija preklapanja vremenskih intervala istog dana (create/update) — novi `ErrorCode` + business exception.
- [ ] `TerminRepository`, `TerminService`, `TerminController` (admin-only CRUD), DTO-i.
- [ ] Soft delete kroz `Status`.
- [ ] Trigger generisanja Appointment slotova na create (vidi Modul 4).

## Modul 4 — Appointment generisanje slotova

- [ ] Entitet `Appointment` (`id`, `terminId`, `pilatesId`, `userId` nullable, `status` enum AVAILABLE/BOOKED/CANCELED).
- [ ] Flyway migracija `appointment` tabele + unique constraint `(termin_id, pilates_id)`.
- [ ] `AppointmentGenerationService` (ili metoda u `AppointmentService`): za dati Termin ili Pilates, insertuje AVAILABLE redove za sve ACTIVE×ACTIVE kombinacije koje fale. Pozvati iz `PilatesService.create` i `TerminService.create`.
- [ ] Idempotencija — test da dupli pozivi ne prave duplikate.

## Modul 5 — Appointment booking/cancel/reschedule

- [ ] `AppointmentRepository`/`AppointmentService`/`AppointmentController`.
- [ ] `getByUserId` — vraća appointmente ulogovanog korisnika (CLIENT) ili bilo kog korisnika (ADMIN).
- [ ] `create` (booking): provera `remainingAppointments > 0`, slot je AVAILABLE → BOOKED + userId, `remainingAppointments -= 1`. Novi `ErrorCode` za "nema preostalih termina" i "slot nije dostupan".
- [ ] `update` (cancel/reschedule): 12h-pre-termina provera (novi `ErrorCode`), cancel → AVAILABLE/null bez refund-a, reschedule → atomska zamena starog/novog slota bez trošenja kredita.
- [ ] Admin full CRUD bez ograničenja gore navedenih pravila (`@PreAuthorize("hasRole('ADMIN')")` na admin-only akcijama, CLIENT akcije provere ownership-a).
- [ ] `AuditLogService.logCreate/logUpdate` pozivi za booking/cancel/reschedule (audit trail, isti pattern kao User).

## Modul 6 — Testovi

- [ ] `*IT` testovi za sve nove servise (Postgres via docker compose), pattern kao `UserServiceIT`/`AuthServiceIT`.
- [ ] Controller testovi sa `@MockitoBean` + `@WithMockUser(roles = "...")` za `@PreAuthorize` provere (403 za CLIENT na admin-only akcijama).
- [ ] Test 12h cancel pravila (granica: tačno 12h, 11h59m, 12h01m).
- [ ] Test termin-overlap validacije.
- [ ] Test idempotencije generisanja slotova.

## Modul 7 — Van scope-a (buduće, ne implementirati sada)

- [ ] Email notifikacije (slanje na mejl pri promeni appointment statusa).
- [ ] Kalendar integracija (QR kod / push notifikacija za dodavanje u kalendar telefona).

## Dokumentacija

- [ ] Dodati referencu na `SPEC.md` i `PLAN.md` u `CLAUDE.md` (sekcija sa pregledom arhitekture).
- [ ] Ažurirati `CLAUDE.md` heritage-note pošto Modul 0 ukloni competition-domen ostatke iz `ApiPaths`/`Role`.
