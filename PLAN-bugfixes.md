# PLAN-bugfixes.md — Backend zadaci (iz test session-a 2026-07-10)

Ovo su backend zadaci izvedeni iz grešaka/zahteva zabeleženih u IntelliJ scratch-u nakon ručnog testiranja aplikacije. Odgovarajući frontend zadaci su u `FitmeF/PLAN-bugfixes.md` — stavke koje zahtevaju promenu na obe strane su unakrsno referencirane.

Svaka stavka ima status `[ ]`/`[x]`, kratak opis trenutnog stanja (sa file:line referencama) i plan implementacije.

## 1. Paginacija liste termina (admin `/admin/termini`)

- [x] **Implementirano.** Novi `TerminSearchRequestDTO extends PagingRequestDTO` (`page`/`size`/`sortField`/`direction`/`date`). `TerminRepository` dobio `findAllByStatusNot(Status, Pageable)` i `findAllByStatusNotAndDate(Status, LocalDate, Pageable)`. `TerminService.getAllTermini(TerminSearchRequestDTO)` vraća `PagingResponseDTO<TerminDTO>` (sort polje ograničeno allow-listom `id`/`date`/`startTime`/`endTime`/`status`, default `date` ako nevalidno). `TerminController.getAllTermini` binduje `@ModelAttribute TerminSearchRequestDTO` i vraća `SuccessResponseDTO<PagingResponseDTO<TerminDTO>>`. Testovi dodati u `TerminServiceIT` (paginacija + date filter), `TerminControllerIT` ažuriran na novi signature. Full suite 151/151.
- [ ] **Breaking API promena** — frontend (`terminiApi.getAll`/`TerminiPage.tsx`) i dalje očekuje `Termin[]`, treba da pređe na `Page<Termin>` obrazac isti kao `KorisniciPage.tsx` — vidi frontend `PLAN-bugfixes.md` stavku F2. Koordinisati redosled deploy-a.

## 2. Filter po datumu za listu termina ne radi

- [x] **Rešeno zajedno sa stavkom 1** — `date` polje na `TerminSearchRequestDTO` filtrira preko `findAllByStatusNotAndDate`, `GET /api/termini?date=YYYY-MM-DD` sada radi.

## 3. Postman kolekcija — nepotpuna

- [x] **Rešeno.** Dodati folderi/request-ovi u `src/main/resources/postman/postman_collection.json`: Auth (Register, Activate Account, Send/Verify Phone OTP), Pilates (puni CRUD), Termini (puni CRUD + `page`/`size`/`sortField`/`direction`/`date` query parametri iz stavke 1), Termin Templates (puni CRUD), Appointments (Get All/By Id/Available/By User Id, Book, Cancel, Reschedule, Delete). Nove kolekcijske varijable: `pilatesId`, `terminId`, `terminTemplateId`, `appointmentId`, `targetAppointmentId`, `activationToken`.

## 4. Klijent vidi prošle termine kao dostupne za rezervaciju

- [x] **Rešeno.** Privatna metoda `filterToActiveTerminAndPilates` preimenovana u `filterToBookableAppointments` i proširena dodatnim uslovom: `Termin.date`+`Termin.startTime` ne sme biti pre `LocalDateTime.now()`. Test `givenTerminAlreadyStarted_whenGetAvailableAppointments_thenExcludesIt` u `AppointmentServiceIT`. `getAllAppointments` (admin audit lista) namerno nepromenjena — admin i dalje vidi prošle rezervacije. Full suite zeleno.
- **Napomena:** frontend (`BookingPage.tsx`) rešava isti problem nezavisno na svojoj strani (vidi frontend `PLAN-bugfixes.md` F4) — ovaj backend fix je defense-in-depth (štiti i druge klijente API-ja, npr. buduću mobilnu app), nije bio blokirajući za frontend fix.

## 5. Pravilo: max 1 termin dnevno po klijentu

- [x] **Rešeno.** Novi `hasBookedAppointmentOnDate(userId, date)` helper u `AppointmentService` (`repository.findAllByUserId` filtrirano na `BOOKED` → `terminRepository.findAllById` → provera `date`), pozvan unutar postojećeg `if (!isAdmin)` bloka u `bookAppointment` (odmah posle `remainingAppointments` provere). Nov `DuplicateAppointmentOnSameDayException` + `ErrorCode.DUPLICATE_APPOINTMENT_SAME_DAY` (2709, HTTP 409).
- **Odluka korisnika (2026-07-10):** pravilo važi **samo za CLIENT self-booking** — admin booking u ime klijenta ga preskače (isti obrazac kao membership/remaining-appointments provere).
- Testovi u `AppointmentServiceIT`: drugi CLIENT booking istog dana baca `DuplicateAppointmentOnSameDayException`; drugi CLIENT booking različitog dana prolazi; admin booking dva termina istog dana za istog klijenta prolazi (pravilo se ne primenjuje). Full suite 155/155.

## 6. Admin otkazivanje treba da vrati +1 kredit klijentu

- [ ] Cancel grana u `AppointmentService.updateAppointment` (`targetAppointmentId == null`, `service/AppointmentService.java:195-204`) samo setuje `status = AVAILABLE`, `userId = null` — nigde ne menja `User.remainingAppointments`, bez obzira ko otkazuje (potvrđuje trenutnu dokumentovanu politiku "cancel never refunds the credit").
- **Plan:**
  - U cancel grani, **pre** brisanja `appointment.setUserId(null)` (linija ~196), zapamtiti `Long ownerUserId = appointment.getUserId()`.
  - Refund samo kad: `isAdmin == true` **i** `!currentUser.getId().equals(ownerUserId)` (admin otkazuje tuđu rezervaciju, ne svoju) — ovo prirodno isključuje slučaj kad admin otkazuje sopstveni booking.
  - Kad CLIENT sam otkazuje (`!isAdmin`, ownership već enforced iznad) — **bez** refund-a, ostaje kao danas.
  - Učitati `User` preko `userRepository.findById(ownerUserId)`, `setRemainingAppointments(+1)`, `save(...)` — isti obrazac kao decrement u `bookAppointment` (linije 162-168).
  - `reschedule()` (linije 218-243) **ne dirati** — treba da ostane bez promene kredita (izvorni slot se oslobađa, ciljni zauzima od istog korisnika).
  - Testovi u `AppointmentServiceIT`: admin cancel tuđe rezervacije → `remainingAppointments +1`; client self-cancel → bez promene; admin cancel sopstvene rezervacije (ako je admin ikad i sam klijent) → bez promene; reschedule → bez promene.

## 7. "Zaključano" (12h pre termina) — backend nije kriv, ali razmotriti computed polje

- [ ] Potvrđeno: root cause je na frontendu (vidi frontend `PLAN-bugfixes.md` F6) — backend ne izlaže `locked`/`bookable` flag, samo raw `terminDate`/`terminStartTime`, što je dovoljno za frontend da sam računa. `AppointmentService.ensureWithinCancelWindow` (linije 292-304, `CANCEL_WINDOW_HOURS = 12`) postoji ali se koristi samo kao enforcement pri cancel/reschedule, ne kao read-time flag.
- **Opciono (nice-to-have, ne blokira frontend fix):** dodati computed `boolean locked` polje u `AppointmentDTO`, računato u `toDto(...)`/`enrich(...)` koristeći isti `CANCEL_WINDOW_HOURS` — garantuje da frontend i backend nikad ne dođu iz sinhronizacije oko tačne granice. Nije neophodno za rešavanje trenutnog buga (koji je čisto frontend parsing bug).

## 8. Puni CRUD za admin nad rezervacijama (`/admin/rezervacije`)

- [ ] Trenutno postoji: `GET` lista (sa filterima), `GET /{id}`, `GET /available`, `GET /user/{userId}`, `POST` (book — zauzima postojeći AVAILABLE slot, ne kreira novi red), `PUT /{id}` (cancel/reschedule kroz state machine), `DELETE /{id}` (**hard delete bez guardova** — briše i BOOKED redove bez refund-a kredita ili otkazivanja podsetnika, `service/AppointmentService.java:209-216`).
- **Gapovi:**
  - Nema endpoint-a za direktnu izmenu proizvoljnih polja appointment-a (reassign korisnika/mašine/vremena) van cancel/reschedule state machine-a.
  - `DELETE` na BOOKED appointment tiho gubi rezervaciju bez refund-a kredita — **treba uskladiti sa stavkom 6** (ili blokirati brisanje BOOKED redova po uzoru na `TerminService.deleteTermin`'s `TerminDeleteBlockedException`, ili refund-ovati kredit + otkazati podsetnike pre brisanja).
  - "Kreiranje" nove rezervacije za klijenta već radi kroz postojeći `POST /api/appointments` (admin prosleđuje `userId`) — frontend to trenutno ne koristi (vidi frontend F7), ali backend podrška već postoji.
- **Plan (potvrditi obim sa korisnikom pre implementacije — ovo je najmanje precizirana stavka):**
  1. Uskladiti `deleteAppointment` sa stavkom 6: ili refund `+1` kredit vlasniku + `appointmentReminderService.cancelReminders(id)` pre brisanja BOOKED reda, ili baciti `AppointmentDeleteBlockedException` i tražiti da admin prvo otkaže.
  2. Ako je zaista potreban direktan admin-edit (reassign bez cancel+rebook ciklusa) — nov endpoint npr. `PATCH /api/appointments/{id}/admin`, ADMIN-only, novi `AppointmentService.adminUpdateAppointment(...)`. **Preporuka: ne implementirati dok se ne potvrdi da postojeći book/cancel/reschedule/delete tok zaista ne pokriva realan admin use-case** — YAGNI dok ne postoji konkretan scenario koji trenutni API ne pokriva.

## 9. Filter po datumu na admin rezervacijama — backend već radi ispravno

- [x] Potvrđeno: `AppointmentSearchRequestDTO` već ima `dateFrom`/`dateTo` (`dto/request/AppointmentSearchRequestDTO.java:21-25`), `AppointmentController.getAllAppointments` ih binduje (`controller/AppointmentController.java:38-47`), `AppointmentService.getAllAppointments` (linije 73-86) ih ispravno primenjuje. **Nema backend rada** — problem (ako se i dalje reprodukuje) je isključivo frontend, vidi frontend `PLAN-bugfixes.md` F9 (izgleda već rešeno u trenutnom stanju frontend koda, treba samo re-test).

## 10. Admin kreiranje korisnika — backend već postoji

- [x] `POST /api/users` (`UserController.createUser`, `ADMIN`-only) već postoji i radi kompletno — validacija lozinke, unique email/telefon/username, role sync, opciona polja. **Nema backend rada.** Napomena za frontend: novi korisnici se kreiraju sa `Status.INACTIVE` (isto kao self-registracija) — frontend treba ili odmah da pozove `PUT /api/users/{id}` da postavi `ACTIVE`, ili proizvod treba da odluči da li admin-kreirani nalozi trebaju da startuju aktivni.

## 11. Tekst na `/admin/raspored` — nije backend

- [x] Potvrđeno pretragom `src/main/resources/` — nema i18n/message bundle-a, string ne postoji nigde u backend kodu. Čisto frontend UI copy, vidi frontend `PLAN-bugfixes.md` F8.
