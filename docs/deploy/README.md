# Deploy notes

Working notes on hosting FitMeB (Spring Boot jar serving both the API and the built SPA, plus Postgres) on the production VPS.

## Why not Vercel

Vercel targets static sites / Node serverless functions. This app is a single long-running JVM process (Spring Boot) plus a Postgres database — not a fit for Vercel's execution model. Vercel is fine for FitmeF (the frontend repo) if it's ever deployed standalone, but not for this backend jar.

## Server

- Hetzner Cloud, server name `fitme-prod`, Ubuntu 26.04 LTS, 2 vCPU / 4GB RAM / 40GB SSD.
- Public IPv4: `91.98.235.42` (IPv6 also assigned, unused so far).
- Domain (pending DNS): `pilates.fitme.rs` → A record to the IP above, not yet live — testing directly via IP:port until then.
- Hetzner Cloud Firewall + `ufw` on the box both restrict inbound to 22, 80, 443.
- Non-root sudo user `fitme`, SSH key auth only, in the `docker` group (`docker ps` works without sudo).
- Docker installed on the server.

Connect: `ssh fitme@91.98.235.42`

## Build strategy: locally, not on the server

`mvn package` runs on the dev machine (faster, doesn't burn the VPS's 4GB RAM on a build), then the finished jar is copied to the server. The Dockerfile is single-stage — it only copies a pre-built jar, it does not run Maven.

### 1. Build the frontend

FitmeF (sibling repo) has no Maven integration — its build output must be copied into `src/main/resources/static` by hand before packaging.

```bash
cd ../FitmeF
npm install
npm run build

cd ../FitMeB
mkdir -p src/main/resources/static
cp -r ../FitmeF/dist/. src/main/resources/static/
```

### 2. Build the jar

```bash
./mvnw clean package -DskipTests
```

Verify the frontend actually landed in the jar: `unzip -l target/fitme-*.jar | grep static/index.html`.

### 3. Ship it to the server

```bash
ssh fitme@91.98.235.42 "mkdir -p ~/FitMeB"
scp Dockerfile docker-compose.prod.yml target/fitme-*.jar fitme@91.98.235.42:~/FitMeB/
```

`.env` is deliberately **not** included in that scp — see below, create it directly on the server.

### 4. Create `.env` on the server

SSH in, then in `~/FitMeB/` create `.env` (copy `.env-default`'s shape, fill in production values — do **not** reuse the dev `SECRET_KEY`, generate a new one via jwtsecret.com):

```
POSTGRES_DB=fitme
POSTGRES_USER=fitme
POSTGRES_PASSWORD=<new strong password>
SECRET_KEY=<new secret, NOT the dev one>
CORS_ALLOWED_ORIGINS=

# Brevo (transactional email - OTP, reminders, newsletter)
BREVO_API_KEY=<real Brevo API key>
BREVO_SENDER_EMAIL=noreply@pilates.fitme.rs
BREVO_SENDER_NAME=FitMe Pilates

# Leave both blank once HTTPS (step 6) is live — docker-compose.prod.yml
# falls back to JWT_COOKIE_SECURE=true / JWT_COOKIE_SAME_SITE=None when these
# are blank/unset in .env, which is what you want behind HTTPS.
# Only while smoke-testing over plain http://91.98.235.42:8080 before that,
# temporarily set JWT_COOKIE_SECURE=false and JWT_COOKIE_SAME_SITE=Lax —
# browsers silently drop Secure cookies sent over plain HTTP, so login
# won't persist otherwise. Revert both (back to blank) once Caddy/HTTPS is up.
JWT_COOKIE_SECURE=
JWT_COOKIE_SAME_SITE=
```

`CORS_ALLOWED_ORIGINS` doesn't matter much in this deployment shape — the SPA and API share an origin (same jar, `FrontendConfig` forwards SPA routes), so no cross-origin request ever happens in production. It only matters for the local Vite dev server. Same blank-is-safe rule applies: `docker-compose.prod.yml` falls back to the dev/ngrok/prod origin list when it's blank/unset.

> **Note:** an earlier version of `docker-compose.prod.yml` passed `JWT_COOKIE_SECURE`/`JWT_COOKIE_SAME_SITE`/`CORS_ALLOWED_ORIGINS` straight through from the host env without a fallback. When `.env` left them blank (as instructed above), Compose still set the container env var to a literal empty string — which is *present but empty*, not unset — so Spring's own `${JWT_COOKIE_SECURE:true}` default in `application.yaml` never kicked in, and boot failed trying to bind `""` to a boolean. Fixed by adding `:-true`/`:-None`/`:-<origins>` defaults directly in `docker-compose.prod.yml`'s `environment:` block.

### 5. Bring it up

```bash
cd ~/FitMeB
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml logs -f app
```

`docker-compose.prod.yml` runs two services: `app` (built from `Dockerfile`) and `postgres` (image `postgres:16-alpine`, no host port published — only reachable from `app` over the internal `fitme-net` network).

### 6. Smoke test (pre-HTTPS)

`http://91.98.235.42:8080` — fine for checking the app boots and serves the SPA/public endpoints. **Login won't persist** unless `JWT_COOKIE_SECURE=false` was set per step 4 (Secure cookies are dropped by browsers over plain HTTP). Don't chase auth bugs here without checking that first.

### 7. Reverse proxy + HTTPS (once DNS is live)

Once `pilates.fitme.rs` → `91.98.235.42` A record has propagated:

- Install **Caddy** on the server, ~5-line Caddyfile reverse-proxying `pilates.fitme.rs` → `localhost:8080`, automatic Let's Encrypt cert.
- Revert `JWT_COOKIE_SECURE`/`JWT_COOKIE_SAME_SITE` in `.env` back to unset (defaults `true`/`None`) and restart the `app` container.
- Final test: `https://pilates.fitme.rs`, full login flow.

### Ongoing

- Redeploy = rebuild jar locally, scp it over again, `docker compose -f docker-compose.prod.yml up -d --build`.
- Back up the `fitme-db-data` volume regularly (`pg_dump` cron job, or Hetzner snapshot feature).

## Open items

- DNS A record for `pilates.fitme.rs` — waiting on cPanel access at the registrar (EuroNet).
- Caddy install + Caddyfile — not done yet, blocked on the DNS record above.
- First real deploy (steps 1-6) — not yet executed end to end.
