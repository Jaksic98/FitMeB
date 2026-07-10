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

- [x] **Rešeno.** Cancel grana u `updateAppointment` sad pamti `ownerUserId` pre brisanja, i poziva novi `refundAppointmentCredit(ownerUserId)` kad `isAdmin && !currentUser.getId().equals(ownerUserId)` — admin otkazuje tuđu rezervaciju. Client self-cancel i admin-cancel-sopstvene-rezervacije ostaju bez refund-a. `reschedule()` nije dirano. Testovi u `AppointmentServiceIT`: `givenClientBooking_whenAdminCancels_thenRefundsClientCredit`, `givenAdminOwnBooking_whenAdminCancels_thenDoesNotRefundCredit` (+ postojeći `givenBookedAppointmentFarInFuture_whenClientCancels_thenReleasesSlotWithoutRefundingCredit` i dalje zeleno). Full suite 157/157.

## 7. "Zaključano" (12h pre termina) — backend nije kriv, ali razmotriti computed polje

- [x] **Implementirano (nice-to-have).** Root cause za "sve zaključano" bug ostaje na frontendu (vidi frontend `PLAN-bugfixes.md` F6). Dodato computed `boolean locked` polje u `AppointmentDTO`, računato u `AppointmentService.toDto(...)` preko novog `isLocked(Termin)` helpera koji koristi isti `CANCEL_WINDOW_HOURS` — `ensureWithinCancelWindow` refaktorisan da poziva isti helper (DRY, umesto duplirane cutoff-logike). Frontend sad može da koristi ovaj flag direktno umesto sopstvenog parsing-a. Testovi: `givenAppointmentWithinCancelWindow_whenBooked_thenLockedIsTrue`, `givenAppointmentFarInFuture_whenBooked_thenLockedIsFalse`. Full suite 159/159.

## 8. Puni CRUD za admin nad rezervacijama (`/admin/rezervacije`)

- [x] **Rešeno (korisnik potvrdio 2026-07-10: i bezbedan delete i novi PATCH endpoint).**
  1. **Bezbedan delete:** `deleteAppointment` sad, pre brisanja BOOKED reda, otkazuje podsetnike (`appointmentReminderService.cancelReminders`) i refund-uje `+1` kredit vlasniku — po istoj politici kao stavka 6 (bez refund-a ako admin briše sopstvenu rezervaciju).
  2. **Nov endpoint:** `PATCH /api/appointments/{id}/admin`, ADMIN-only, `AdminUpdateAppointmentRequestDTO` (`status`/`userId`/`terminId`/`pilatesId`, sva polja opciona — patch semantika). `AppointmentService.adminUpdateAppointment`: validira postojanje ciljnog termina/pilatesa/korisnika, blokira reassign na već zauzetu `(terminId, pilatesId)` kombinaciju (`AppointmentNotAvailableException`), zahteva `userId` ako rezultujući status postaje `BOOKED` (`AppointmentUserRequiredException`), automatski briše `userId` ako status nije `BOOKED` (čuva invarijantu). Podsetnici se otkazuju/reprogramiraju u skladu sa promenom statusa. **Namerno ne dira `remainingAppointments`/`membershipExpiresAt`** — ovo je admin popravka podataka, ne booking akcija.
  - Testovi u `AppointmentServiceIT`: safe-delete refund/no-refund, reassign na drugog klijenta, patch status→AVAILABLE čisti `userId` i podsetnike, patch→BOOKED bez `userId` baca grešku, reassign na zauzet slot baca grešku. `AppointmentControllerIT`: ADMIN 200, CLIENT 403 na novom endpoint-u. Full suite 165/165.

## 9. Filter po datumu na admin rezervacijama — backend već radi ispravno

- [x] Potvrđeno: `AppointmentSearchRequestDTO` već ima `dateFrom`/`dateTo` (`dto/request/AppointmentSearchRequestDTO.java:21-25`), `AppointmentController.getAllAppointments` ih binduje (`controller/AppointmentController.java:38-47`), `AppointmentService.getAllAppointments` (linije 73-86) ih ispravno primenjuje. **Nema backend rada** — problem (ako se i dalje reprodukuje) je isključivo frontend, vidi frontend `PLAN-bugfixes.md` F9 (izgleda već rešeno u trenutnom stanju frontend koda, treba samo re-test).

## 10. Admin kreiranje korisnika — backend već postoji

- [x] `POST /api/users` (`UserController.createUser`, `ADMIN`-only) već postoji i radi kompletno — validacija lozinke, unique email/telefon/username, role sync, opciona polja. **Nema backend rada.** Napomena za frontend: novi korisnici se kreiraju sa `Status.INACTIVE` (isto kao self-registracija) — frontend treba ili odmah da pozove `PUT /api/users/{id}` da postavi `ACTIVE`, ili proizvod treba da odluči da li admin-kreirani nalozi trebaju da startuju aktivni.

## 11. Tekst na `/admin/raspored` — nije backend

- [x] Potvrđeno pretragom `src/main/resources/` — nema i18n/message bundle-a, string ne postoji nigde u backend kodu. Čisto frontend UI copy, vidi frontend `PLAN-bugfixes.md` F8.
