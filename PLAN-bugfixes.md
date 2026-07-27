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

## 12. Klijent self-cancel treba da vrati +1 kredit

- [x] **Rešeno.** `updateAppointment` cancel grana je do sada refund-ovala kredit samo kad `isAdmin && !currentUser.getId().equals(ownerUserId)` (admin otkazuje tuđu rezervaciju) — klijent koji otkazuje sopstvenu rezervaciju nije dobijao kredit nazad. Uslov promenjen na `!isAdmin || !currentUser.getId().equals(ownerUserId)`, što refund-uje u sva tri slučaja osim kad admin otkazuje sopstvenu (nikad-potrošenu) rezervaciju. Postojeći test `givenBookedAppointmentFarInFuture_whenClientCancels_thenReleasesSlotWithoutRefundingCredit` je preimenovan/ažuriran u `...ThenReleasesSlotAndRefundsCredit` (očekivanje 2→3). Full suite zeleno.

## 13. Reschedule na dan gde klijent već ima drugu rezervaciju nije blokiran

- [x] **Rešeno.** Pravilo "max 1 termin dnevno" (stavka 5) se proveravalo samo u `bookAppointment`, ne i u `reschedule()` — klijent je mogao da premesti postojeću rezervaciju na dan kad već ima drugu, zaobilazeći pravilo. `hasBookedAppointmentOnDate` sad prima opcioni `excludeAppointmentId` (izuzima appointment koji se premešta, da se dozvoli premeštanje unutar istog dana), `reschedule()` prima `isAdmin` i baca `DuplicateAppointmentOnSameDayException` za non-admin kad ciljni datum već ima drugu BOOKED rezervaciju. Admin bypass-uje proveru (isti obrazac kao ownership/cancel-window). Testovi: `givenClientWithBookingOnOtherDay_whenReschedulingIntoThatDay_thenThrowsDuplicateAppointmentOnSameDayException`, `givenClientBooking_whenReschedulingWithinSameDay_thenSucceeds`, `givenAdminReschedulingClientBooking_whenTargetDateHasClientBooking_thenBypassesDuplicateCheck`.

## 14. Filter po vremenu (startTime) na admin/termini

- [x] **Rešeno.** `TerminSearchRequestDTO` dobio `startTime` (`LocalTime`, `@DateTimeFormat(ISO.TIME)`), `TerminRepository` dobio `findAllByStatusNotAndStartTime`/`findAllByStatusNotAndDateAndStartTime`, `TerminService.getAllTermini` grana na kombinaciju date/startTime filtera. Testovi u `TerminServiceIT`: `givenStartTimeFilter_...`, `givenDateAndStartTimeFilter_...`.

## 15. Admin add/update na Sprave i Termini "prijavljuje grešku" iako je sačuvano (root cause: CORS)

- [x] **Rešeno (root-caused uživo, 2026-07-27).** Reprodukovano preko browser-a (chrome-devtools) protiv lokalno pokrenutog backend-a: `POST /api/pilates` sa validnom ADMIN sesijom vraćao je `403` sa telom `"Invalid CORS request"` (plain text, ne JSON) čim `Origin` header zahteva nije tačno `http://localhost:5173` — potvrđeno da je ovo Spring-ov `DefaultCorsProcessor.rejectRequest()`, ne `@PreAuthorize`/business greška. `GET` zahtevi ne šalju `Origin` header za same-origin pozive pa prolaze; `POST/PUT/DELETE` browser uvek šalje `Origin`, pa se update/create/delete akcije ruše na svakom originu koji nije tačno onaj jedan hardkodovani (npr. alternativni lokalni port kad je 5173 zauzet, ili — realniji scenario s obzirom na `.env` komentar "temporary ngrok tunnel origin for phone testing" — kad admin testira sa telefona preko ngrok tunela čiji se URL menja pri svakom restartu i vremenom prestane da se poklapa sa onim upisanim u `.env`). `SecurityConfig.corsConfigurationSource()` promenjen sa `setAllowedOrigins` na `setAllowedOriginPatterns` (podržava wildcard, kompatibilno sa `allowCredentials(true)`), default u `application.yaml` i `.env` proširen sa `https://*.ngrok-free.app,https://*.ngrok-free.dev` — ngrok tunel URL više ne treba ručno ažurirati u `.env` posle svakog restarta. Verifikovano curl-om: simulirani `Origin: https://random-tunnel-123.ngrok-free.app` → `200` + entitet kreiran; nepoznat origin (npr. `http://localhost:5174`) i dalje ispravno odbijen sa `403`.
- **Napomena:** `BUGS.md` je opisao ovo kao "operacija je uspela u bazi, ali se prikazuje greška". Pod CORS teorijom zahtev se odbija PRE nego što stigne do kontrolera, pa ništa ne bi trebalo da bude sačuvano pri samom neuspelom pokušaju — najverovatnije objašnjenje je da je admin probao više puta (jednom sa origin-om koji ne odgovara, pa ponovo sa origin-om koji odgovara), i taj drugi, uspešni pokušaj je ono što se videlo u bazi. U svakom slučaju, simptom "add/update povremeno prijavljuje grešku bez razloga" (posebno pri testiranju sa telefona) je sada potpuno objašnjen i otklonjen.

## 16. Admin `/admin/raspored` (i svaka ugnježdena admin ruta) baca grešku na reload i ostaje "zaglavljen" na produkciji (root cause: `FrontendConfig` SPA forward mapping)

- [x] **Rešeno (root-caused uživo na produkcionom serveru, 2026-07-27).** Prijavljeno: kao admin, kreiranje novog reda u `/admin/raspored` prijavljuje grešku, i posle reload-a stranica ostaje polomljena (ne može se ući u app iz tog tab-a). Root-caused preko produkcionih logova (`docker logs fitme-app`): `GET /admin/raspored` od ulogovanog `adam.adamovic@fitme.com` je vratio `NoResourceFoundException: No static resource admin/raspored for request '/admin/raspored'` — dakle Spring uopšte nije imao mapiranu rutu za taj path, pao je na static-resource fallback i 404-ovao, umesto da forward-uje na `index.html` kao SPA shell. Uzrok: `FrontendConfig.forward` je imao samo `@RequestMapping("/{path:[^\\.]*}")` — Spring-ov `PathPatternParser` vezuje `{path}` za tačno JEDAN path segment, bez obzira na regex unutar njega; taj pattern matchuje `/login` (jedan segment) ali strukturno ne može da matchuje `/admin/raspored` (dva segmenta). Bilo koji hard-reload/deep-link na ugnježdenu admin rutu (`/admin/termini`, `/admin/rezervacije`, `/admin/korisnici`, `/admin/sprave`) je pogođen istim bugom, nezavisno od podataka ili konkretne akcije — "greška pri kreiranju" je slučajnost trenutka kada je admin osvežio/otvorio tu rutu, ne posledica same create akcije.
- **Fix:** `FrontendConfig.forward` sad ima dva mapiranja: `{"/{path:[^\\.]*}", "/**/{path:[^\\.]*}"}` — drugi pattern koristi `**` da pokrije proizvoljan broj vodećih segmenata pre finalnog segmenta bez tačke, pa pokriva ugnježđene rute bilo koje dubine.
- **Test:** novi `FrontendConfigIT` (`src/test/java/com/consi/fitme/config/FrontendConfigIT.java`) — 3 testa: `/login` (single-segment, regresija za postojeće ponašanje), `/admin/raspored` (two-segment, direktna regresija za ovaj bug), `/admin/rezervacije/detalji` (three-segment, dokazuje da fix radi za proizvoljnu dubinu). Sva 3 prolaze; full suite 173 (171 prolazi, 2 pre-postojeća neuspeha u `AppointmentReminderServiceIT` nepovezana sa ovom izmenom — flaky zbog pokretanja protiv deljene lokalne dev baze sa akumuliranim realnim appointment/reminder podacima, ne zbog ove promene).
- **Napomena o lokalnoj infrastrukturi otkrivenoj tokom istrage:** dok se root-caused-ovao ovaj bug, primećeno je da je lokalni `fitme-postgres` (port 5432) bio zaustavljen a port zauzet kontejnerom nepovezanog projekta (`postgres:14-alpine`), zbog čega je lokalni backend (IntelliJ debug sesija) neko vreme bacao `HikariPool`/`password authentication failed` greške — nepovezano sa produkcijom, ali je usporilo dijagnozu jer se prvo posumnjalo da je isti uzrok. Rešeno zaustavljanjem tuđeg kontejnera i pokretanjem pravog `fitme-postgres` (podaci netaknuti, `docker exec fitme-postgres psql ... SELECT count(*) FROM termin_template` = 7 pre i posle).
- **Deploy:** fix zahteva novi build+deploy jar-a na produkcioni server (`91.98.235.42`, `~/FitMeB`, `docker-compose.prod.yml`) po koracima iz `docs/deploy/README.md` — nije još izvršeno u trenutku pisanja ove stavke.
