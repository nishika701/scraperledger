# ScrapeLedger

A self-healing scraper pipeline with an event-sourced ledger underneath.

Built for [WeMakeDevs](https://www.wemakedevs.org/)' **Into the Scrape-Verse** hackathon, in partnership with Bright Data.

## What it does

Most scrapers work like this: check a page, save the value, overwrite it next time you check. That means you lose all history, and you can't tell the difference between a real change and a broken scraper silently returning garbage.

ScrapeLedger does it differently: every scrape is recorded as a **permanent, immutable event** — never overwritten, only appended. This means:

- Full history is preserved for every tracked field
- A real data change and a scraper failure look different in the ledger, not the same
- You can trace exactly when a value changed and what it changed from/to

## Architecture

```
Java CLI
   |
   v
Bright Data Scraper Studio (via bdata CLI)
   |
   v
Structured JSON
   |
   v
Spring Boot REST API
   |
   v
Ingestion Service (only logs changed fields)
   |
   v
PostgreSQL (Neon) — append-only ledger
```

- **Data collection:** [Bright Data Scraper Studio](https://brightdata.com/) — invoked from Java via `ProcessBuilder`, running the `bdata` CLI
- **Backend:** Java 21, Spring Boot, Spring Data JPA
- **Database:** PostgreSQL (hosted on [Neon](https://neon.tech/))
- **Interface:** a CLI (`ScrapewatchCli`) — no web dashboard, per the hackathon's own guidance that "the terminal is the UI"

## Why PyPI package pages

The project originally targeted GitHub repository pages. GitHub's dynamic, JavaScript-heavy layout made Scraper Studio's automatic schema generation unreliable, and GitHub is also the kind of large, well-known site Bright Data's pre-built scraper library likely already covers.

The project pivoted to **PyPI package pages** (e.g. `pypi.org/project/requests/`) — simpler, consistently templated HTML, and a better fit for the hackathon's guidance to target long-tail sites rather than ones already well-covered.

## Features

- **Event-sourced ledger** — `repos` and `repo_events` tables in PostgreSQL; every field change is a new row, never an update
- **Change-only logging** — a scrape that returns identical data to the last scrape writes nothing new
- **Anomaly detection** — a scrape returning a missing/blank value is flagged in the ledger rather than trusted as real data
- **Self-healing verified** — `bdata scraper heal` was run against the live collector, approved, and the collector was re-verified to still return correct data for its real targets afterward

## Setup

### Prerequisites
- Java 21+
- Maven
- A [Bright Data](https://brightdata.com/) account, with the CLI installed and logged in:
  ```bash
  npm install -g @brightdata/cli
  bdata login
  ```
- A PostgreSQL database (this project uses a free [Neon](https://neon.tech/) instance)

### Database schema

Run this against your Postgres instance:

```sql
CREATE TABLE repos (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    source_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE repo_events (
    id SERIAL PRIMARY KEY,
    repo_id INTEGER NOT NULL REFERENCES repos(id),
    field VARCHAR(50) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    is_anomaly BOOLEAN DEFAULT FALSE,
    scraped_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_repo_events_repo_id ON repo_events(repo_id);
CREATE INDEX idx_repo_events_scraped_at ON repo_events(scraped_at);
```

### Environment variables

Set these before running the app (do not commit real values):

```bash
DB_URL=jdbc:postgresql://<your-host>/<your-db>?sslmode=require
DB_USER=<your-username>
DB_PASSWORD=<your-password>
```

### Run the backend

```bash
mvn spring-boot:run
```

### Build the CLI classpath and run commands

```bash
mvn dependency:build-classpath -Dmdep.outputFile=classpath.txt

# Track a new package
java -cp "target/classes;$(cat classpath.txt)" com.scrapeledger.scrapeledger.cli.ScrapewatchCli add requests "https://pypi.org/project/requests/"

# View a tracked package's current state
java -cp "target/classes;$(cat classpath.txt)" com.scrapeledger.scrapeledger.cli.ScrapewatchCli check 1

# View full change history
java -cp "target/classes;$(cat classpath.txt)" com.scrapeledger.scrapeledger.cli.ScrapewatchCli history 1

# View all tracked packages
java -cp "target/classes;$(cat classpath.txt)" com.scrapeledger.scrapeledger.cli.ScrapewatchCli status
```

## Bright Data Collector

- **Collector ID:** `c_mt6pl55urtgr3wyqj`
- Created via `bdata scraper create <url> "<field description>"`
- Healed and approved via `bdata scraper heal` / `bdata scraper approve`, verified against real PyPI targets afterward

## What's next

The current anomaly detection only flags missing/blank fields. A more useful version would catch suspicious *values*, not just absent ones — for example:

- A version number that decreases instead of increases
- A price that drops to near zero unexpectedly
- A value that deviates sharply from its own historical pattern

## Tech stack

Java · Spring Boot · Spring Data JPA · PostgreSQL (Neon) · Bright Data Scraper Studio · Lombok · Jackson
