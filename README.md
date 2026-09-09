# Familie-ks-exodus - en klon av Historisk Exodus - tilgjengeliggjøring av tabeller for migrering til GCP

orginal: https://github.com/navikt/historisk-exodus

Dette er en applikasjon som gir tilgang til historiske data som opprinnelig kommer fra Infotrygd slik at de kan
replikeres til PostgreSQL i GCP.

**Denne applikasjonen er laget for å lese døde data fra Oracle, dvs. data som ikke forandrer seg.**

For å se hvor denne tjenesten kjøres, se `nais`-mappen.

Nye klienter legges til under `accessPolicy.inbound.rules` i deploymenten.
Tilganger til tabeller ligger som custom roles på hver klient.

## Lokalt utviklingsmiljø

### Relevante endepunkt

- Swagger: http://localhost:8080/swagger-ui/index.html
- Tabeller og kolonner som er i bruk: http://localhost:8080/tables

### Bygg og start utviklingsmiljø

#### Bygg og kjør tester

`mvn clean verify`

#### Kjør backend i debug mode fra IntelliJ
Se `DevMain.kt` i test-mappen

#### Kjør alt med docker compose

```shell
docker compose up
```

## Tekniske krav til Oracle-tabeller som skal replikeres

Følgende tekniske krav må være oppfylt for at Exodus skal kunne replikere en tabell:

- Tabellen må ha en definert primary key. Denne kan bestå av en eller flere kolonner.
- Hver rad må ha et timestamp som viser når den sist ble endret. Denne må ha kolonnenavn `OPPDATERT`.
- Følgende indeks må eksistere: `(OPPDATERT, PK_1, PK_2, ..., PK_n)` hvor `PK_1, PK_2, ..., PK_n` er alle
primary key kolonnene sortert i alfabetisk rekkefølge.

## Arkitektur

Exodus er en tilstandsløs applikasjon som henter data fra Oracle på vegne av 
en klientapplikasjon. Når applikasjonen spør om data fra Exodus så sender
den med et token som inneholder all nødvendig tilstand om hvor langt replikeringen har kommet.
Denne tilstanden genereres av Exodus og lagres av klientapplikasjonen.

```mermaid
   C4Deployment

   Deployment_Node(gcp, "GCP", "Google Compute Platform"){
      Container(gcpApp, "Historisk App")
      ContainerDb(postgresDb, "PostgreSQL")
   }

   Deployment_Node(fss, "FSS", "Fagsystemsonen (on prem)"){
      Container(exodus, "Exodus")
      ContainerDb(oracleDb, "Oracle Database")
   }

   Rel(gcpApp, exodus, "Henter replikerte data", "REST/JSON/HTTPS")
   Rel(gcpApp, postgresDb, "Skriver replikerte data")
   Rel(exodus, oracleDb, "Leser replikerte data")

   UpdateRelStyle(gcpApp, postgresDb, $offsetX="20")
   UpdateRelStyle(gcpApp, exodus, $offsetY="30", $offsetX="-40")

   UpdateLayoutConfig($c4ShapeInRow="1", $c4BoundaryInRow="2")
```

### Interaksjon med klientapplikasjon
Diagrammet nedenfor viser hvordan Exodus kommuniserer med klientapplikasjonen og Oracle, 
samt hvordan klientapplikasjonen er forventet å fungere.

```mermaid
sequenceDiagram
    participant Postgres
    participant App
    participant Exodus
    participant Oracle
    
    loop
        App-->>App: Sjekk om data skal replikeres  
    end
    
   opt Tell antall rader for å senere kunne vise progressjon
      App->>Exodus: POST /tellRader (tabellnavn)
      Exodus->>Oracle: select count(1) from tabellnavn
      Oracle-->>Exodus: 
      Exodus-->>App: 200 OK: Antall rader
      App->>Postgres: insert
   end
    
   loop Hent data
      App-->>Postgres: tx begin
      
      activate App
      activate Postgres

      App->>Postgres: select iterator for update
      Postgres-->>App: iterator eller null første gangen
      
      App->>Exodus: POST /hentRader (tabellnavn, iterator)
      Exodus->>Oracle: sjekk om det er tatt ny baseline
      Exodus->>Oracle: select * from .. where X > iterator order by X
      Oracle-->>Exodus: 
      
      
      Exodus-->>App: 200 OK: rader, ny iterator
      
      App->>Postgres: insert rader
      App->>Postgres: update iterator
      
      App-->>Postgres: tx commit
      deactivate Postgres
      deactivate App
   end

   Note over Postgres, Exodus: Feilhåndtering

   alt Ny baseline
      activate App
      App-->>Postgres: tx begin
      activate Postgres
      App->>Postgres: select iterator for update
      Postgres-->>App: iterator
      App->>Exodus: POST /hentRader (tabellnavn, iterator)
      Exodus->>Oracle: sjekk om det er tatt ny baseline
      Exodus-->>App: 409 CONFLICT: Error: Ny baseline
      App->>Postgres: update ... job_status = ny baseline
      App-->>Postgres: tx commit
      deactivate Postgres
      deactivate App
   else Generell/ukjent feil
      activate App
      App-->>Postgres: tx begin
      activate Postgres
      App->>Postgres: select iterator for update
      Postgres-->>App: iterator eller null første gangen
      App->>Exodus: POST /hentRader (tabellnavn, iterator)
      Exodus-->>App: [noe gikk galt]
      App-->>Postgres: tx: abort
      deactivate Postgres
      deactivate App
      App-->>App: Prøv på nytt senere
   end
```
