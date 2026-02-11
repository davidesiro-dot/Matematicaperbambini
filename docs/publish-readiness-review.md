# Senior Review — Maturità per pubblicazione

Data analisi: 2026-02-11

## Executive summary

Valutazione complessiva (stato attuale): **6/10**.

L'app mostra una buona base funzionale per una prima release privata/internal test:
- dominio educativo già ampio (tabelline, addizioni/sottrazioni in colonna, divisioni guidate, denaro);
- presenza di test unitari su alcune logiche core;
- persistenza locale strutturata su DataStore.

Tuttavia, rispetto agli standard di una software house matura per una release pubblica stabile, restano gap importanti in:
- affidabilità pipeline build (toolchain/plugin non risolti);
- qualità architetturale (file molto grandi e UI + logica mescolate);
- readiness di processo (assenza CI/CD, quality gates, checklist release);
- hardening prodotto (osservabilità, test e policy di regression più complete).

## Evidenze tecniche principali

### 1) Build/release engineering
- La versione AGP dichiarata nel catalogo è `8.13.2`, non risolvibile dai repository standard al momento dell'analisi, con build bloccata.
- Toolchain Java documentata (Corretto 21) presente e coerente con progetto.

Impatto: **alto**. Senza build ripetibile, la pubblicazione non è governabile.

### 2) Architettura codice
- Presenza di file Kotlin molto grandi (es. `MainActivity.kt` ~2800+ linee, altri >1000 linee) che indicano accoppiamento alto e manutenzione difficile.
- UI Compose, stato, persistenza e logica in parte coesistono nello stesso livello applicativo.

Impatto: **alto** su evoluzione futura, bug fixing e onboarding.

### 3) Test strategy
- Ci sono test unitari utili su porzioni pure (divisione, validazione input, utility denaro), positivi per robustezza logica.
- Suite test ancora ridotta e senza copertura esplicita end-to-end del flusso UX; test strumentale quasi solo smoke test.

Impatto: **medio/alto** per rischio regressioni UI e regressioni cross-feature.

### 4) Dati, privacy, resilience
- Persistenza locale su DataStore con gestione errori difensiva (try/catch e fallback a liste vuote).
- Manca una chiara policy di migrazioni schema/versioning dei payload serializzati e un piano osservabilità errori in produzione.

Impatto: **medio**.

### 5) Product readiness (store/public release)
- Risorse stringhe minimali (solo app name), segnale di internazionalizzazione e content governance iniziale.
- Mancano indicatori di processo tipici: checklist release, CI, crash monitoring, rollout progressivo codificato, SLO/SLI.

Impatto: **alto** per una pubblicazione “da software house”.

## Valutazione di prontezza secondo benchmark software house

## Scala usata
- 0-3: prototipo
- 4-6: beta/internal test
- 7-8: production-ready con rischi gestibili
- 9-10: organizzazione matura con controllo continuo qualità

## Scoring per area
- **Prodotto funzionale**: 7/10
- **Architettura/manutenibilità**: 5/10
- **Qualità/testing**: 5/10
- **Build/Release/DevOps**: 4/10
- **Operabilità (monitoring/incidents)**: 3/10
- **Compliance processo release**: 4/10

**Media ponderata**: circa **5.5-6/10**.

Stato consigliato: **aprire solo canale Internal testing/Closed testing**, non ancora “public production broad rollout”.

## Cosa fare in dettaglio (piano operativo)

## Fase 0 — Blocchi immediati (1-2 giorni)
1. **Ripristinare build deterministica**
   - Allineare AGP/Kotlin/Gradle wrapper a combinazione supportata e realmente pubblicata.
   - Eseguire matrix minima di build (`assembleDebug`, `testDebugUnitTest`).
2. **Definire baseline release**
   - Versioning semantico interno (`versionCode` incrementale, changelog).
   - Build type release firmata (keystore gestita in modo sicuro).
3. **Gate iniziale obbligatorio**
   - Nessuna merge senza build verde locale e test unitari essenziali.

## Fase 1 — Hardening qualità (1 settimana)
1. **CI/CD minima (GitHub Actions o equivalente)**
   - Job: lint + unit test + assemble debug/release.
   - Caching Gradle, timeout, artifact APK/AAB.
2. **Quality gates**
   - ktlint/detekt con soglie iniziali realistiche.
   - Coverage minima su moduli domain/core (target iniziale 60% su classi pure).
3. **Testing ampliato**
   - Aggiungere test per `HomeworkEngine`, repository, formatter/report.
   - Test UI smoke su flussi principali (avvio gioco, completamento esercizio, salvataggio report).

## Fase 2 — Refactor architetturale (2-4 settimane)
1. **Spezzare mega-file**
   - Estrarre da `MainActivity` e schermate monolitiche in feature package modulari.
   - Introdurre ViewModel per schermata/feature con stato unidirezionale.
2. **Separare livelli**
   - `ui` / `domain` / `data` con interfacce repository.
   - Use-case espliciti per regole di gioco e generazione compiti.
3. **Stabilizzare contratti dati**
   - DTO versionati e migrazioni gestite per payload DataStore.

## Fase 3 — Operabilità e pubblicazione (1-2 settimane)
1. **Osservabilità**
   - Crash reporting + analytics minime privacy-safe.
   - Logging strutturato con eventi critici.
2. **Release strategy professionale**
   - Internal → Closed (20-50 tester) → Open → Production.
   - Rollout graduale (5%, 20%, 50%, 100%) con stop automatico su KPI negativi.
3. **Definition of Done release**
   - Checklist fissa: test pass, bug blocker zero, performance baseline, asset store pronti.

## KPI consigliati per decidere Go/No-Go
- Crash-free sessions >= 99.5%
- ANR rate entro soglie Play Console
- 0 bug P0/P1 aperti
- Tempo cold start entro target definito
- Tasso completamento flussi chiave >= soglia (da definire con team prodotto)

## Conclusione

L'app è **promettente** e già utile in contesto educativo, ma oggi è più vicina a una **beta avanzata** che a una release pubblica pienamente governata.

Con il piano sopra (soprattutto build affidabile + CI + refactor dei file monolitici + test/UI gate), può raggiungere una maturità **7.5/10** in poche iterazioni e supportare una pubblicazione con rischio significativamente più basso.
