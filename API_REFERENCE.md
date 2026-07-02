# FitMe Backend — API Reference

Ovaj dokument opisuje sve dostupne API endpointe, strukturu zahteva i odgovora, i pravila autentikacije.
Namijenjen je frontendu kao jedini izvor istine za integraciju.

---

## Opšte konvencije

### Base URL
```
http://localhost:8080
```

### Autentikacija
- JWT token se čuva u **httpOnly cookie** naziva `fitme_cookie` — frontend ga nikad ne čita direktno.
- Svaki zahtev koji zahteva autentikaciju mora biti poslat sa `credentials: 'include'`.
- Cookie se automatski šalje i prima od browsera — nema Bearer headera.

### Response omotač — uspeh
Svi uspešni odgovori imaju isti omotač:
```json
{
  "success": true,
  "message": "Opis akcije",
  "entity": { /* payload */ },
  "timestamp": 1719615689000,
  "path": "/api/auth/login"
}
```
Polje `entity` sadrži konkretan podatak (objekat, lista, paginirani odgovor…).

### Response omotač — greška
```json
{
  "code": 1101,
  "message": "Opis greške",
  "details": ["Detalj validacije 1", "Detalj validacije 2"],
  "timestamp": 1719615689000
}
```

### Paginacija
Endpointi koji vraćaju paginirane liste koriste query parametre:

| Param | Tip | Default | Opis |
|---|---|---|---|
| `page` | integer | `1` | Broj stranice (1-indexed) |
| `size` | integer | `10` | Elemenata po stranici |
| `sortField` | string | `"id"` | Polje za sortiranje |
| `direction` | `ASC` / `DESC` | `DESC` | Smer sortiranja |

Paginirani odgovor u `entity`:
```json
{
  "data": [...],
  "totalPages": 5,
  "totalElements": 47,
  "size": 10,
  "page": 1,
  "empty": false
}
```

---

## Enumi

### `Status`
```
INACTIVE | ACTIVE | DELETED | LOCKED
```

### `Role`
```
ADMIN | CLIENT
```

### `AppointmentStatus`
```
AVAILABLE | BOOKED | CANCELED
```

---

## Error kodovi

| Kod | HTTP | Značenje |
|---|---|---|
| `1000` | 500 | Interna greška servera |
| `1101` | 401 | Neuspešna prijava |
| `1103` | 401 | Nedostaje JWT token |
| `1104` | 401 | Neispravan JWT token |
| `1106` | 401 | Potrebna autentikacija |
| `1107` | 403 | Pristup zabranjen (nema rolu) |
| `1400` | 400 | Validacija nije uspela |
| `2101` | 409 | Korisničko ime već postoji |
| `2102` | 404 | Korisnik nije pronađen |
| `2104` | 400 | Token za aktivaciju neispravan ili istekao |
| `2501` | 404 | Pilates sprava nije pronađena |
| `2601` | 404 | Termin nije pronađen |
| `2602` | 409 | Termin se preklapa sa postojećim |
| `2603` | 400 | Vreme završetka mora biti posle početka |
| `2701` | 404 | Appointment nije pronađen |
| `2702` | 409 | Appointment slot nije dostupan |
| `2703` | 400 | Nema preostalih termina |
| `2704` | 400 | Otkazivanje nije moguće < 12h pre termina |
| `2705` | 403 | Nemate pravo pristupa ovom appointmentu |
| `2706` | 400 | userId obavezan za admin rezervaciju |
| `2707` | 409 | Appointment nije u statusu BOOKED |

---

## Auth — `/api/auth`

### POST `/api/auth/login`
Prijava korisnika. Postavlja `fitme_cookie`.

**Auth**: nije potrebna

**Request body**:
```json
{
  "email": "adam.adamovic@fitme.com",
  "password": "adam.adamovic.fitme123!"
}
```

**Validacija**:
- `email` — obavezno, validan email format, max 254 karaktera
- `password` — obavezno, 8–72 karaktera

**Response `entity`**:
```json
{ "message": "Uspešna prijava" }
```

---

### POST `/api/auth/logout`
Odjava korisnika. Briše `fitme_cookie`.

**Auth**: potrebna

**Request body**: prazan

**Response `entity`**:
```json
{ "message": "Uspešna odjava" }
```

---

### POST `/api/auth/register`
Samoregistracija novog korisnika (klijenta). **Napomena**: ne šalje se stvarni email — aktivacioni link se trenutno samo loguje na backend-u (`AuthService.register`), stvarna SMTP integracija nije implementirana. Aktivacija u praksi ide preko `GET /api/auth/activate?token=...` (link se za sada mora ručno pokupiti iz logova) ili admin ručno kroz `PUT /api/users/{id}`.

**Auth**: nije potrebna

**Request body**:
```json
{
  "username": "novi.korisnik",
  "fullName": "Novi Korisnik",
  "email": "novi.korisnik@example.com",
  "phoneNumber": "+381641234567",
  "password": "Lozinka123!"
}
```

**Validacija**:
- `username` — obavezno, 5–100 karaktera
- `fullName` — obavezno, 2–100 karaktera
- `email` — obavezno, validan format, max 254 karaktera
- `phoneNumber` — opciono, format `+?[0-9 ]{6,30}`
- `password` — obavezno, 8–72 karaktera, mora sadržati bar jedan broj i jedan specijalni karakter

**Response `entity`**: `UserDTO` (videti sekciju Users)

---

### GET `/api/auth/activate?token={token}`
Aktivacija naloga putem emaila.

**Auth**: nije potrebna

**Query param**: `token` (string iz emaila)

**Response `entity`**:
```json
{ "message": "Nalog je aktiviran" }
```

---

### GET `/api/auth/me`
Vraća podatke trenutno prijavljenog korisnika.

**Auth**: potrebna

**Response `entity`**: `UserDTO`

---

### GET `/api/auth/validate-session`
Proverava da li je sesija aktivna (cookie validan).

**Auth**: potrebna

**Response `entity`**:
```json
{ "message": "Sesija je aktivna" }
```
Vraća `401` ako cookie ne postoji ili je istekao.

---

## Users — `/api/users`

> Svi endpointi zahtevaju rolu **ADMIN**.

### `UserDTO` — struktura odgovora
```json
{
  "id": 1,
  "username": "adam.adamovic",
  "fullName": "Adam Adamović",
  "email": "adam.adamovic@fitme.com",
  "phoneNumber": null,
  "status": "ACTIVE",
  "roles": ["ADMIN", "CLIENT"],
  "remainingAppointments": 10,
  "emailNotifications": true,
  "calendarNotifications": true
}
```

---

### GET `/api/users`
Lista korisnika sa pretragom i paginacijom.

**Auth**: ADMIN

**Query parametri** (svi opcioni, osim paginacionih):

| Param | Tip | Opis |
|---|---|---|
| `id` | Long | Filter po ID |
| `username` | string | Filter po korisničkom imenu (sadrži) |
| `fullName` | string | Filter po imenu (sadrži) |
| `email` | string | Filter po emailu (sadrži) |
| `roles` | `ADMIN` / `CLIENT` | Filter po rolama |
| `status` | `ACTIVE` / `INACTIVE` / `DELETED` / `LOCKED` | Filter po statusu |
| `page` | integer | Stranica (default 1) |
| `size` | integer | Veličina (default 10) |
| `sortField` | string | Polje sortiranja (default `id`) |
| `direction` | `ASC` / `DESC` | Smer (default `DESC`) |

**Response `entity`**: `PagingResponseDTO<UserDTO>`

---

### GET `/api/users/{id}`
Jedan korisnik po ID-u.

**Auth**: ADMIN

**Response `entity`**: `UserDTO`

---

### POST `/api/users`
Kreiranje korisnika od strane admina (može dodeliti rolu ADMIN).

**Auth**: ADMIN

**Request body**:
```json
{
  "username": "novi.korisnik",
  "fullName": "Novi Korisnik",
  "email": "novi.korisnik@fitme.com",
  "phoneNumber": null,
  "password": "Lozinka123!",
  "roles": ["CLIENT"],
  "remainingAppointments": 5,
  "emailNotifications": true,
  "calendarNotifications": false
}
```

**Validacija**:
- Ista kao kod `register` + opciona polja `roles`, `remainingAppointments`, `emailNotifications`, `calendarNotifications`
- `roles` — lista `Role` enum vrednosti; ako je prazna, korisnik dobija samo `CLIENT`

**Response `entity`**: `UserDTO`

---

### PUT `/api/users/{id}`
Parcijalno ažuriranje korisnika (samo prosleđena ne-null polja se menjaju).

**Auth**: ADMIN

**Request body** (sva polja opciona):
```json
{
  "username": "novi.username",
  "fullName": "Novo Ime",
  "email": "novi@fitme.com",
  "phoneNumber": "+381641234567",
  "password": "NovaLozinka1!",
  "status": "ACTIVE",
  "roles": ["ADMIN", "CLIENT"],
  "remainingAppointments": 10,
  "emailNotifications": true,
  "calendarNotifications": true
}
```

**Napomena**: Postavljanje `status` na `ACTIVE` resetuje `failedLoginAttempts` na 0.

**Response `entity`**: `UserDTO`

---

### DELETE `/api/users/{id}`
Soft-delete korisnika (status → `DELETED`, fizički red ostaje u bazi).

**Auth**: ADMIN

**Response `entity`**:
```json
{ "message": "Korisnik je uspešno obrisan" }
```

---

## Pilates sprave — `/api/pilates`

> Svi endpointi zahtevaju rolu **ADMIN**.

### `PilatesDTO` — struktura odgovora
```json
{
  "id": 1,
  "position": "A1",
  "name": "Reformer Pro",
  "status": "ACTIVE"
}
```

---

### GET `/api/pilates`
Lista svih sprava (bez paginacije).

**Auth**: ADMIN

**Response `entity`**: `List<PilatesDTO>`

---

### GET `/api/pilates/{id}`
Jedna sprava po ID-u.

**Auth**: ADMIN

**Response `entity`**: `PilatesDTO`

---

### POST `/api/pilates`
Kreiranje nove sprave. Automatski generiše appointment slotove za sve postojeće aktivne termine.

**Auth**: ADMIN

**Request body**:
```json
{
  "position": "A1",
  "name": "Reformer Pro"
}
```

**Validacija**:
- `position` — obavezno, max 50 karaktera
- `name` — obavezno, max 120 karaktera

**Response `entity`**: `PilatesDTO`

---

### PUT `/api/pilates/{id}`
Parcijalno ažuriranje sprave.

**Auth**: ADMIN

**Request body** (sva polja opciona):
```json
{
  "position": "B2",
  "name": "Cadillac Elite",
  "status": "INACTIVE"
}
```

**Response `entity`**: `PilatesDTO`

---

### DELETE `/api/pilates/{id}`
Soft-delete sprave (status → `DELETED`).

**Auth**: ADMIN

**Response `entity`**:
```json
{ "message": "Sprava je uspešno obrisana" }
```

---

## Termini — `/api/termini`

> Svi endpointi zahtevaju rolu **ADMIN**.

### `TerminDTO` — struktura odgovora
```json
{
  "id": 1,
  "date": "2026-07-15",
  "startTime": "09:00:00",
  "endTime": "10:00:00",
  "status": "ACTIVE"
}
```

**Format datuma**: `yyyy-MM-dd`
**Format vremena**: `HH:mm:ss`

---

### GET `/api/termini`
Lista svih termina (bez paginacije).

**Auth**: ADMIN

**Response `entity`**: `List<TerminDTO>`

---

### GET `/api/termini/{id}`
Jedan termin po ID-u.

**Auth**: ADMIN

**Response `entity`**: `TerminDTO`

---

### POST `/api/termini`
Kreiranje novog termina. Automatski generiše appointment slotove za sve postojeće aktivne sprave.

**Auth**: ADMIN

**Request body**:
```json
{
  "date": "2026-07-15",
  "startTime": "09:00:00",
  "endTime": "10:00:00"
}
```

**Validacija**:
- `date` — obavezno
- `startTime` — obavezno
- `endTime` — obavezno, mora biti posle `startTime`
- Ne sme se preklapati sa drugim aktivnim terminom istog datuma

**Response `entity`**: `TerminDTO`

---

### PUT `/api/termini/{id}`
Parcijalno ažuriranje termina.

**Auth**: ADMIN

**Request body** (sva polja opciona):
```json
{
  "date": "2026-07-20",
  "startTime": "10:00:00",
  "endTime": "11:00:00",
  "status": "INACTIVE"
}
```

**Response `entity`**: `TerminDTO`

---

### DELETE `/api/termini/{id}`
Soft-delete termina (status → `DELETED`).

**Auth**: ADMIN

**Response `entity`**:
```json
{ "message": "Termin je uspešno obrisan" }
```

---

## Appointments — `/api/appointments`

Appointment slotovi se **ne kreiraju ručno** — backend ih automatski generiše pri kreiranju sprave ili termina.
Svaki slot predstavlja kombinaciju (Termin × PilatesSprava).

### `AppointmentDTO` — struktura odgovora
```json
{
  "id": 42,
  "terminId": 3,
  "pilatesId": 1,
  "userId": null,
  "status": "AVAILABLE",
  "terminDate": "2026-07-15",
  "terminStartTime": "09:00:00",
  "terminEndTime": "10:00:00",
  "pilatesPosition": "A1",
  "pilatesName": "Reformer Pro"
}
```

`userId` je `null` dok slot nije rezervisan.

---

### GET `/api/appointments`
Lista svih appointmenta.

**Auth**: ADMIN

**Response `entity`**: `List<AppointmentDTO>`

---

### GET `/api/appointments/{id}`
Jedan appointment po ID-u.

**Auth**: ADMIN

**Response `entity`**: `AppointmentDTO`

---

### GET `/api/appointments/available`
Lista dostupnih (slobodnih) appointment slotova. Opciono filtriranje po datumu.

**Auth**: ADMIN, CLIENT

**Query parametri**:

| Param | Tip | Opis |
|---|---|---|
| `date` | `yyyy-MM-dd` | Opciono — filtrira slotove za taj datum |

**Response `entity`**: `List<AppointmentDTO>` (samo slotovi sa statusom `AVAILABLE`)

---

### GET `/api/appointments/user/{userId}`
Lista appointmenta za konkretnog korisnika.

**Auth**: ADMIN, CLIENT

**Napomena**: CLIENT može videti samo svoje appointmente — ako prosledi tuđi `userId`, dobija `403`.

**Response `entity`**: `List<AppointmentDTO>`

---

### POST `/api/appointments`
Rezervacija appointment slota.

**Auth**: ADMIN, CLIENT

**Request body**:
```json
{
  "appointmentId": 42,
  "userId": null
}
```

**Pravila**:
- **CLIENT**: `userId` se ignoriše — rezervacija uvek ide na prijavljenog korisnika. Troši jedan `remainingAppointments` kredit.
- **ADMIN**: `userId` je obavezan. Ne troši kredit.
- Slot mora biti u statusu `AVAILABLE`.

**Response `entity`**: `AppointmentDTO` (status → `BOOKED`)

---

### PUT `/api/appointments/{id}`
Otkazivanje ili premeštanje rezervacije.

**Auth**: ADMIN, CLIENT

**Request body**:
```json
{
  "targetAppointmentId": null
}
```

**Pravila**:
- Ako je `targetAppointmentId` **null** → **otkazivanje** (status → `AVAILABLE`, `userId` se briše — slot je odmah ponovo dostupan drugim korisnicima; `CANCELED` vrednost enuma postoji ali se trenutno nigde ne koristi). Kredit se **ne vraća**.
- Ako je `targetAppointmentId` prisutan → **premeštanje** na drugi slobodan slot (atomic swap). Kredit se ne troši ponovo.
- Oba slučaja nisu moguća ako je manje od **12h do početka termina**.
- **CLIENT**: može otkazati/premestiti samo svoje appointmente.
- **ADMIN**: nema ograničenja vlasništva ni vremenskog prozora.

**Response `entity`**: `AppointmentDTO` (ažurirani slot)

---

### DELETE `/api/appointments/{id}`
Fizičko brisanje appointment slota iz baze.

**Auth**: ADMIN

**Response `entity`**:
```json
{ "message": "Appointment je uspešno obrisan" }
```

---

## Seed korisnici (za razvoj)

| Email | Lozinka | Rola |
|---|---|---|
| `adam.adamovic@fitme.com` | `adam.adamovic.fitme123!` | ADMIN, CLIENT |
| `marko.markovic@fitme.com` | `marko.markovic.fitme123!` | CLIENT |
| `jovana.jovanovic@fitme.com` | `jovana.jovanovic.fitme123!` | CLIENT |
| `petar.petrovic@fitme.com` | `petar.petrovic.fitme123!` | CLIENT |
| `ana.anic@fitme.com` | `ana.anic.fitme123!` | CLIENT |
