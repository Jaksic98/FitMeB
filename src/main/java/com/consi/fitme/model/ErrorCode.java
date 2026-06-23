package com.consi.fitme.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

  // 1xxx – infra/security
  INTERNAL_SERVER_ERROR(1000, "Interna greška servera", HttpStatus.INTERNAL_SERVER_ERROR),
  LOGIN_FAILED(1101, "Neuspesna prijava", HttpStatus.UNAUTHORIZED),
  LOGOUT_FAILED(1102, "Neuspesna odjava", HttpStatus.BAD_REQUEST),
  MISSING_JWT_TOKEN(1103, "Nedostaje JWT token", HttpStatus.UNAUTHORIZED),
  INVALID_JWT_TOKEN(1104, "JWT token nije ispravan", HttpStatus.UNAUTHORIZED),
  MISSING_COOKIE(1105, "Nedostaje kolačić", HttpStatus.BAD_REQUEST),
  AUTHENTICATION_REQUIRED(1106, "Potrebna je autentikacija", HttpStatus.UNAUTHORIZED),
  ACCESS_DENIED(1107, "Pristup je zabranjen", HttpStatus.FORBIDDEN),
  VALIDATION_FAILED(1400, "Validacija nije uspela", HttpStatus.BAD_REQUEST),

  // 21xx – user domain
  USERNAME_ALREADY_EXISTS(2101, "Korisničko ime već postoji", HttpStatus.CONFLICT),
  USER_NOT_FOUND(2102, "Korisnik nije pronađen", HttpStatus.NOT_FOUND),
  WEAK_PASSWORD(2103, "Lozinka ne ispunjava bezbednosne uslove", HttpStatus.BAD_REQUEST),
  INVALID_ACTIVATION_TOKEN(
      2104, "Token za aktivaciju nije ispravan ili je istekao", HttpStatus.BAD_REQUEST),
  TIP_PRAVNOG_LICA_NOT_FOUND(2201, "Tip pravnog lica nije pronađen", HttpStatus.NOT_FOUND),
  TIP_PRAVNOG_LICA_ALREADY_EXISTS(2202, "Tip pravnog lica već postoji", HttpStatus.CONFLICT),
  TIP_PRAVNOG_LICA_DELETE_BLOCKED(
      2203,
      "Tip pravnog lica nije moguće obrisati jer je povezan sa pravnim licem",
      HttpStatus.CONFLICT),
  PRAVNO_LICE_NOT_FOUND(2204, "Pravno lice nije pronađeno", HttpStatus.NOT_FOUND),
  PRAVNO_LICE_ALREADY_EXISTS(2205, "Pravno lice već postoji", HttpStatus.CONFLICT),
  KONTAKT_PRAVNOG_LICA_NOT_FOUND(2206, "Kontakt pravnog lica nije pronađen", HttpStatus.NOT_FOUND),
  KONTAKT_PRAVNOG_LICA_ALREADY_EXISTS(
      2207, "Kontakt pravnog lica već postoji", HttpStatus.CONFLICT),

  // 25xx – pilates domain
  PILATES_NOT_FOUND(2501, "Sprava za pilates nije pronađena", HttpStatus.NOT_FOUND),

  // 26xx – termin domain
  TERMIN_NOT_FOUND(2601, "Termin nije pronađen", HttpStatus.NOT_FOUND),
  TERMIN_OVERLAP(
      2602, "Termin se preklapa sa postojećim terminom istog datuma", HttpStatus.CONFLICT),
  TERMIN_INVALID_TIME_RANGE(
      2603, "Vreme završetka termina mora biti posle vremena početka", HttpStatus.BAD_REQUEST),

  // 3xxx – generic resources
  RESOURCE_NOT_FOUND(3001, "Resurs nije pronađen", HttpStatus.NOT_FOUND);

  private final int code;
  private final String message;
  private final HttpStatus httpStatus;
}
