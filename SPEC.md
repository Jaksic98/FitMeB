# SPEC.md — Specifikacija domena (Pilates rezervacije)

Referencirano iz `CLAUDE.md`. Ovo je dorađena verzija opisa iz `Fitme.md` scratch fajla, nakon razjašnjenja arhitekturnih odluka. `PLAN.md` prati implementaciju modula opisanih ovde.

## 1. Pregled

App za rezervaciju termina za pilates sprave. Korisnik (CLIENT) rezerviše određenu pilates spravu u određenom terminu (datum + vremenski interval); jedna sprava u jednom terminu može imati najviše jednog korisnika.

Ovo je **pivot** postojećeg fitness-competition koncepta (`CLAUDE.md` heritage note) na pilates-booking domen — ne paralelna struktura. Postojeća User/Auth/Status/Audit infrastruktura se zadržava i proširuje.

## 2. Role i Auth

- `Role` enum: `ADMIN`, `CLIENT` (brišu se `JUDGE`, `COMPETITOR`).
- `ApiPaths`: brišu se `COMPETITIONS`, `CATEGORIES`, `SCORES`, `LEADERBOARD`; dodaju se `PILATES`, `TERMINI`, `APPOINTMENTS`.
- Odgovarajući leftover `ErrorCode` unosi (template/competition domen) se čiste — `PRAVNO_LICE_*`/`TIP_PRAVNOG_LICA_*` ostaju netaknuti (već su dokumentovani kao dead code u `CLAUDE.md`, van scope-a ove promene).
- JWT/cookie auth, lockout (5 failed attempts → `LOCKED`), `AuditLogService`, `BaseAuditableEntity` — sve se zadržava bez promene mehanizma, samo se proširuje na nove entitete (Pilates, Termin, Appointment).

## 3. Entiteti

### User (postojeći, proširen)

| Polje | Tip | Napomena |
|---|---|---|
| id, username, email, password | postojeće | bez izmena |
| status | `Status` (postojeće) | `INACTIVE` je default na registraciju (vidi §4.1) |
| failedLoginAttempts | postojeće | bez izmena |
| phoneNumber | String | novo |
| remainingAppointments | Integer, default 0 | novo — **kredit-balans** iz kupljenog paketa, ne lifetime brojač. Booking troši 1, cancel **ne** vraća kredit. Top-up je ručna izmena kroz postojeći `PUT /api/users/{id}` (nema posebnog "kupovina paketa" endpointa/istorije u MVP-u). |
| emailNotifications | boolean, default true | novo — flag, slanje samo skeleton/no-op u MVP-u (§6) |
| calendarNotifications | boolean, default true | novo — flag, no-op u MVP-u (§6) |

### Pilates (sprava)

| Polje | Tip |
|---|---|
| id | Long |
| position | int/String (raspored u sali) |
| name | String |
| status | `Status` (ACTIVE/INACTIVE/DELETED) — soft delete, isti enum kao User |

CRUD: `PilatesController`/`PilatesService`/`PilatesRepository`, admin-only (`@PreAuthorize("hasRole('ADMIN')")`).

### Termin

| Polje | Tip |
|---|---|
| id | Long |
| date | LocalDate |
| startTime | LocalTime |
| endTime | LocalTime |
| status | `Status` (ACTIVE/INACTIVE/DELETED) — soft delete |

Validacija: na create/update, **ne dozvoliti** preklapanje vremenskog intervala sa drugim Terminom istog datuma (status ACTIVE).

CRUD: `TerminController`/`TerminService`/`TerminRepository`, admin-only.

### Appointment

| Polje | Tip |
|---|---|
| id | Long |
| terminId | Long (FK) |
| pilatesId | Long (FK) |
| userId | Long (FK), nullable | `NULL` dok slot nije rezervisan |
| status | enum `AVAILABLE` / `BOOKED` / `CANCELED` |

**Model: pre-generisani slotovi.** Appointment red postoji za svaku kombinaciju (Termin × Pilates) odmah po kreiranju Termina ili Pilatesa — generiše se **ono što fali** (idempotentno; vidi §4.2), ne čeka se rezervacija.

**Cancel flow:** isti red se vraća direktno iz `BOOKED` u `AVAILABLE` (`userId = NULL`) — ne ostaje trajno u `CANCELED`, jer pre-generisani slot mora odmah biti dostupan drugim korisnicima. Istorija ko/kada je otkazao se čuva kroz postojeći `AuditLogService` (before/after snapshot), ne kroz trajni `CANCELED` status. `CANCELED` vrednost enuma je rezervisana za eventualnu buduću upotrebu (npr. admin force-cancel bez auto-release, izveštaji) — u MVP toku se ne koristi kao trajno stanje.

CRUD: `AppointmentController`/`AppointmentService`/`AppointmentRepository`.

## 4. Poslovna pravila

### 4.1 Registracija i aktivacija

- Public signup (`username`, `email`, `password`) kreira `User` sa `status = INACTIVE`, role `CLIENT`.
- Dva paralelna puta do `ACTIVE`:
  1. **Email verifikacija** — token (iskoristiti postojeći password-reset-token mehanizam ako postoji, ili novi `verification_token` pristup bez nove kolone/tabele ako je moguće), link u mejlu aktivira korisnika.
  2. **Admin manuelno** — postojeći `PUT /api/users/{id}` već dozvoljava promenu `status`, ostaje dostupan kao alternativni/fallback put (npr. mejl nije stigao).
- Ova dva mehanizma se ne sudaraju — bilo koji od njih prevodi `INACTIVE → ACTIVE`.

### 4.2 Generisanje Appointment slotova

- Trigger: create novog `Pilates` ili novog `Termin` (status ACTIVE).
- Akcija: za svaku ACTIVE×ACTIVE kombinaciju (Termin, Pilates) koja **nema** postojeći Appointment red, insert red sa `status = AVAILABLE`, `userId = NULL`.
- Idempotentno — generator se može pozvati ponovo (npr. nakon dodavanja novog Pilatesa) i samo dopunjava ono što fali, ne duplira postojeće redove.
- Unique constraint: `(termin_id, pilates_id)` — jedan red po kombinaciji (pošto cancel vraća isti red u AVAILABLE, nema potrebe za partial-index izuzecima kao kod username-a).

### 4.3 Booking

- Klijent bira `AVAILABLE` appointment → `status = BOOKED`, `userId = <self>`.
- Preduslov: `User.remainingAppointments > 0`. Uspešan booking: `remainingAppointments -= 1`. Nema refunda na plain cancel.
- Najviše jedan `BOOKED` appointment po (Termin, Pilates) u svakom trenutku — garantovano modelom (jedan red = jedan resurs).

### 4.4 Cancel

- Klijent može otkazati **sopstveni** `BOOKED` appointment najkasnije **12h pre** `Termin.startTime` (datum + start vreme).
- Efekat: `status = AVAILABLE`, `userId = NULL`. **Kredit se ne vraća** (`remainingAppointments` ostaje umanjen).
- Manje od 12h pre termina → odbijeno (business exception, novi `ErrorCode`).

### 4.5 Reschedule (update)

- Klijentov "update" appointmenta pokriva **i** cancel **i** reschedule (promena pilates/termin postojeće rezervacije), u jednom pozivu.
- Reschedule = atomska zamena: stari slot → `AVAILABLE` (`userId = NULL`), novi target slot (mora biti `AVAILABLE`) → `BOOKED` za istog korisnika. **Ne troši dodatni kredit** — to je 1-za-1 zamena, ne novi booking. Isti 12h-pre-termina uslov važi i za reschedule (računa se prema **starom** terminu, koji se napušta).
- Plain cancel (bez rebookinga) troši kredit trajno, kao u §4.4.

### 4.6 Admin pristup Appointment-u

- Admin ima full CRUD bez ograničenja (bez 12h pravila, bez credit logike) — koristi se za ručne korekcije.
- Klijent ima samo: `getByUserId` (sve appointmente tog korisnika), `create` (booking), `update` (cancel/reschedule kao gore).

## 5. API pregled

| Path (ApiPaths) | Controller | Pristup |
|---|---|---|
| `/api/pilates` | PilatesController | ADMIN CRUD |
| `/api/termini` | TerminController | ADMIN CRUD |
| `/api/appointments` | AppointmentController | ADMIN full CRUD; CLIENT: getByUserId/create/update (cancel+reschedule) |
| `/api/users` | UserController (postojeći) | ADMIN CRUD, proširen sa novim User poljima |
| `/api/auth` | AuthController (postojeći) | bez promene mehanizma, registracija ostaje INACTIVE default |

## 6. Van scope-a za sada (buduće)

Servis za notifikacije (email) i dodavanje u kalendar telefona (QR kod ili push) — eksplicitno odloženo u originalnom opisu ("feature za kasnije"). `emailNotifications`/`calendarNotifications` flagovi na `User` postoje od početka (da se ne radi migracija kasnije), ali se ne koriste aktivno dok se servis ne implementira.
