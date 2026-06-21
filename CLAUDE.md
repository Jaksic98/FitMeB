# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

FitMe backend: a Spring Boot 4.0.2 (Java 21) REST API backing a fitness-competition app, serving a built SPA from the same jar. Postgres via Flyway migrations, stateless JWT auth stored in an httpOnly cookie (not `Authorization` header), Querydsl for dynamic search/paging, MapStruct for partial-update mapping.

Only the user/auth domain is implemented so far. `ApiPaths` (`src/main/java/com/consi/fitme/util/ApiPaths.java`) already declares `COMPETITIONS`, `CATEGORIES`, `SCORES`, `LEADERBOARD` prefixes for domains that don't have controllers/entities yet.

**Pivot in progress**: the project is moving from the fitness-competition concept to a pilates-class booking domain (user reserves a pilates machine for a time slot). See `SPEC.md` for the full domain specification (entities, business rules, API surface) and `PLAN.md` for the module-by-module implementation checklist — read both before touching `Role`, `ApiPaths`, or adding new entities/controllers, since the competition-domain placeholders above are slated for removal as part of this pivot.

## Commands

Setup (one-time, also in README.md):
```bash
cp .env-default .env        # fill in POSTGRES_*, SECRET_KEY (generate via jwtsecret.com)
docker compose up -d
docker exec -it fitme-postgres psql -U fitme -d postgres -c "CREATE DATABASE fitme;"
```

Run:
```bash
./mvnw spring-boot:run
```

Test (all, or a single class/method — integration tests need Postgres up via docker compose and a `.env`):
```bash
./mvnw test
./mvnw test -Dtest=UserServiceIT
./mvnw test -Dtest=AuthServiceIT#givenInactiveUser_whenLogin_thenThrowsLoginFailedException
```

Format (run before opening a Merge Request, per README.md):
```bash
./mvnw spotless:apply
```

Flyway reset, if migration history gets into a bad state (see README.md for full detail):
```bash
docker compose down -v && docker compose up -d   # nukes data, re-creates db, recommended for local dev
./mvnw flyway:clean flyway:migrate                # dev/test only, never on prod — drops all tables
```

## Architecture

**Layering**: `controller` → `service` → `repository` → `model/entity`. Controllers are thin: validate input, delegate to one service call, wrap the result with `ResponseUtil.success(data, message, request.getRequestURI())`. All success responses are `SuccessResponseDTO<T>`; everything else (validation, business, auth, 404, 500) flows through `GlobalExceptionHandler` into `ErrorResponseDTO`.

**Error handling**: `ErrorCode` (`model/ErrorCode.java`) is the single source of truth for error code/message/`HttpStatus` triples, namespaced by range (1xxx infra/security, 2xxx user domain, 3xxx generic resources). Domain exceptions extend one of the `exception/base` types (`CustomException`, `BusinessException`, `NotFoundException`) which all implement `CustomError` (carries an `ErrorCode`). `GlobalExceptionHandler` has one generic handler for `RuntimeException` that special-cases anything implementing `CustomError`, plus dedicated handlers for the usual Spring binding/validation/routing exceptions. When adding a new failure mode, add an `ErrorCode` entry and a small exception class under `exception/<domain>` rather than throwing ad hoc exceptions.

**Auth**: Login issues a JWT (`JwtService`, HS256, secret from `SECRET_KEY` env var) set as an httpOnly cookie (`fitme_cookie`, name/expiry/secure configurable under `security.jwt.*`). `JwtAuthenticationFilter` reads the cookie (not a bearer header) and populates the `SecurityContext` per request; sessions are stateless. Authorization is method-level via `@PreAuthorize("hasRole('ADMIN')")` on controllers, not URL-pattern based (`SecurityConfig` only permits `/api/auth/**` and static assets, everything else just requires authentication). Roles are looked up from the DB on each authentication via `CustomUserDetailsService`, prefixed `ROLE_<code>` — they are not cached on the JWT claims.

**Roles vs. user identity**: roles live in `roles`/`user_roles` join tables, not as a column on `users`. `UserRepository` exposes native-query methods (`findRoleCodesByUserId`, `deleteUserRolesByUserId`, `addUserRole`) instead of a JPA `@ManyToMany`, and `UserService.syncUserRoles` always ensures `Role.COMPETITOR` is present, replacing the full role set on create/update.

**Status-driven lifecycle**: `Status` enum (`INACTIVE`, `ACTIVE`, `DELETED`, `LOCKED`) drives soft delete and account lockout instead of physical deletes or a separate `enabled` flag — `deleteUser` just flips status to `DELETED`. Persisted as a smallint via `StatusConverter`. Uniqueness constraints are status-aware: email uniqueness excludes `DELETED` rows, and username uniqueness is enforced only for `ACTIVE` users via a partial unique index (`uk_users_username_active` in `V1__create_users_and_roles.sql`) — so the same username can be reused across non-active accounts. `AuthService` increments `failedLoginAttempts` on bad credentials and flips status to `LOCKED` at 5 attempts; resetting status to `ACTIVE` (e.g. by an admin `PUT`) zeroes the counter again.

**Two distinct audit mechanisms** — don't conflate them:
- `BaseAuditableEntity` (`model/entity`) + `JpaAuditConfig`'s `AuditorAware` automatically stamp `createdBy/At`, `updatedBy/At` on entities that extend it, using the authenticated principal name (or `"system"` if anonymous).
- `AuditLogService` writes explicit before/after snapshots (JSONB `old_row`/`new_row` + computed `changed_cols`) to the `audit_log` table for business actions (currently user create/update/delete), tagged with the request id and actor pulled from MDC/`SecurityContext`. Call `logCreate`/`logUpdate`/`logDelete` from services when an action should be auditable this way, in the same transaction as the mutation.

`RequestMdcFilter` (highest filter precedence) puts `requestId` (from `X-Request-Id` header or a generated UUID) and resolved username into MDC for the duration of each request, used by both logging (`logback-spring.xml`) and `AuditLogService`.

**Dynamic search/paging**: `UserQueryRepository`/`UserQueryRepositoryImpl` builds Querydsl predicates by hand from `UserSearchRequestDTO` (filters default to excluding `DELETED` unless a status filter is explicitly given) and only allows sorting on a hardcoded `ALLOWED_SORT_FIELDS` set. Querydsl `Q*` metamodel classes are annotation-processor-generated into `target/generated-sources` — run a build before relying on `QUser` etc. in an IDE. `PaginationUtils.getPageable` translates the API's 1-indexed `page` param to Spring Data's 0-indexed `Pageable`.

**Partial updates**: `UserPatchMapper` (MapStruct, `@BeanMapping(nullValuePropertyMappingStrategy = IGNORE)`-style patching) copies non-null fields from `UpdateUserRequestDTO` onto the existing entity; service methods handle password/status side effects (re-hashing, failed-attempt reset) separately afterward since those aren't plain field copies.

**SPA hosting**: `FrontendConfig` forwards any path without a `.` (i.e. not a static asset) to `/index.html`, so this jar serves both the API and a built single-page frontend.

**Heritage note**: this repo was bootstrapped from a generic template (`pom.xml` description and some legacy `ErrorCode` entries like `PRAVNO_LICE_*`/`TIP_PRAVNOG_LICA_*` reference an unrelated "Agencija za Osiguranje Depozita" domain). Don't treat those leftover error codes as in-use; they're dead until/unless that domain is actually built.

## Config

- `spring.profiles.active`: `dev` (default, runs Flyway migrate on startup via `FlywayStartupConfig`), `test` (used by `*IT` tests, `@TestPropertySource`-driven), `prod`.
- `.env` (loaded via `spring-dotenv`, not committed) supplies `POSTGRES_URL/USER/PASSWORD/DB` and `SECRET_KEY`; copy from `.env-default`.
- CORS is locked to `http://localhost:5173` (`SecurityConfig.corsConfigurationSource`) — update this if the frontend's dev origin changes.

## Testing conventions

Integration tests are suffixed `IT` (e.g. `UserServiceIT`, `AuthServiceIT`, `UserControllerIT`) and use `@SpringBootTest` against a real Postgres (via docker compose) rather than mocks/H2 — `@Transactional` rolls back DB changes per test. Controller tests additionally mock the service layer with `@MockitoBean` and use `MockMvc` + `@WithMockUser(roles = "...")` to assert `@PreAuthorize` behavior (expect 403, and verify the mocked service was never called).