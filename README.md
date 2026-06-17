# HAIRARENA - Backend

## Lokalno Pokretanje (Backend)

<details>
<summary><strong>Preduslovi</strong></summary>

### Amazon Corretto 21

Instalirajte i konfigurišite [Amazon Corretto 21](https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/downloads-list.html) (JDK). Pratite sledeće korake za instalaciju i podešavanje u vašem IDE-u:

```bash
sudo apt-get update && sudo apt-get install java-common
cd ~/Downloads/
sudo dpkg --install java-21-amazon-corretto-jdk_21.0.6.7-1_amd64.deb
java -version
```

</details>

<details>
<summary><strong>IDE Podešavanje</strong></summary>

#### IntelliJ IDEA

_File ➔ Project Structure ➔ Project ➔ SDK ➔ 21_

_File ➔ Project Structure ➔ Project ➔ Language level ➔ 21_

#### NetBeans IDE

_Tools ➔ Java Platforms ➔ 21_

</details>

<details>
<summary><strong>Promenljive Okruženja</strong></summary>

Generišite tajni ključ koristeći alatku [JwtSecret.com](https://jwtsecret.com/generate).

Obavezne vrednosti u `.env`:
- `POSTGRES_URL`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `POSTGRES_DB`
- `SECRET_KEY`

</details>

<details>
<summary><strong>Pokretanje</strong></summary>

### 1) Environment (.env)

Kopirajte `.env.default` u `.env` i popunite vrednosti:

```bash
cp .env.default .env
```

### 2) Promenljive Okruženja

Generišite tajni ključ koristeći alatku [JwtSecret.com](https://jwtsecret.com/generate).

Obavezne vrednosti u `.env`:
- `POSTGRES_URL`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `POSTGRES_DB`
- `SECRET_KEY`

### 3) Postgres (Docker)

```bash
docker compose down -v
```

```bash
docker compose up -d
```

```bash
docker exec -it hairarena-postgres psql -U hairarena -d postgres -c "CREATE DATABASE hairarena;"
```

### 4) Aplikacija

```bash
./mvnw spring-boot:run
```

</details>

## Formatiranje Koda

Pre kreiranja Merge Request-a obavezno pokrenuti:

```bash
./mvnw spotless:apply
```
