# Senior Review — Maturità per pubblicazione

Data analisi: 2026-02-12

## Executive summary

Valutazione complessiva (stato attuale): **6.8/10**.

Rispetto alla review precedente, il progetto è cresciuto in modo concreto:
- è presente una pipeline CI su GitHub Actions con build e unit test su push/PR verso `main`;
- la toolchain è documentata in modo chiaro (Java 21 + runbook operativo);
- la base funzionale dell’app è ampia e orientata a casi reali scolastici.

Restano però gap importanti prima di una pubblicazione “public rollout” in stile software house leader:
- architettura ancora monolitica in alcune aree (file molto grandi);
- quality gate CI ancora minimi (mancano lint/detekt/coverage/security checks);
- strategia di test non ancora completa su UI/instrumentation;
- processi di release/operabilità non ancora formalizzati end-to-end.

## Evidenze tecniche aggiornate

### 1) CI/CD e build governance
- Workflow CI presente (`.github/workflows/android-ci.yml`) con trigger su `push` e `pull_request` verso `main`.
- La pipeline esegue: `clean`, `assembleDebug`, `testDebugUnitTest` con Java 21 e cache Gradle.
- Versioni AGP/Kotlin sono definite nel version catalog, con coerenza rispetto ai plugin applicati.

Valutazione: **miglioramento netto** rispetto allo stato “senza CI”.

### 2) Toolchain e riproducibilità
- Documento toolchain e runbook presenti (`docs/java-toolchain.md`, `docs/phase0-runbook.md`) con linee guida operative.
- Permane dipendenza forte da setup locale Android SDK/JDK; senza JAVA_HOME/SDK la build locale non parte.

Valutazione: **buona base**, ma serve ulteriore automazione/validazione ambiente per onboarding rapido.

### 3) Architettura e manutenibilità
- Persistono file di grandi dimensioni (`MainActivity.kt` e `HomeworkScreens.kt` oltre 2500 linee).
- Questo suggerisce accoppiamento elevato fra UI, orchestrazione flussi e regole applicative.

Valutazione: **rischio medio-alto** per scalabilità del team, regressioni e tempi di modifica.

### 4) Qualità e strategia test
- Presenza di test unitari su componenti core (es. validazione input, divisioni, utility denaro).
- Il set test è ancora contenuto e i test strumentali risultano minimi (strumentazione base).
- Non risultano gate coverage/lint static analysis obbligatori in CI.

Valutazione: **sufficiente per closed testing**, non ancora livello software house top-tier.

### 5) Sicurezza, privacy, operabilità
- Manifest minimale e senza permessi invasivi (profilo rischio privacy favorevole).
- Backup Android abilitato; utile, ma da allineare a policy dati/sensibilità per pubblicazione.
- Logging e osservabilità ancora basici; non emerge integrazione crash reporting/monitoring di produzione.

Valutazione: **base corretta**, ma operabilità enterprise non ancora pronta.

## Scoring maturità (stile software house leader)

### Scala
- 0-3: prototipo
- 4-6: beta/internal
- 7-8: production-ready con rischio controllato
- 9-10: eccellenza operativa continua

### Punteggi area
- **Prodotto funzionale**: 7.5/10
- **Architettura/manutenibilità**: 5.5/10
- **Qualità/testing**: 6/10
- **Build/Release/DevOps**: 7/10
- **Operabilità (monitoring/incidents)**: 4/10
- **Compliance processo release**: 5.5/10

**Media ponderata**: **6.8/10**.

## Parere pubblicazione (go-to-market)

### Cosa è pronto oggi
- **Pronto per Internal testing e Closed testing esteso** (gruppo controllato, feedback rapido).
- **Non ancora pronto** per open/public broad rollout ad alto volume senza ulteriore hardening.

### Principali rischi residui prima della pubblicazione ampia
1. **Rischio regressioni funzionali/UI** dovuto a test non ancora end-to-end sui flussi principali.
2. **Rischio operativo** per assenza di telemetria matura (crash analytics, alerting, KPI live).
3. **Rischio evolutivo** per complessità monolitica dei file centrali.

## Benchmark “best software house” — gap analysis

Per allinearsi alle procedure delle migliori software house, in genere servono questi pilastri minimi:

1. **Engineering Excellence**
   - PR con mandatory checks: build + test + lint + static analysis + coverage diff.
   - Branch protection con merge bloccato su check rossi.

2. **Release Excellence**
   - Canali progressivi (Internal → Closed → Open → Production) con KPI gate.
   - Rollout graduale e rollback playbook.

3. **Operational Excellence**
   - Crash reporting real-time, metriche ANR, dashboard SLI/SLO.
   - Incident process leggero ma codificato (owner, severità, postmortem).

4. **Architecture Governance**
   - Confini chiari `ui/domain/data`, riduzione file monolitici.
   - Test pyramid bilanciata (unit > integration > UI smoke affidabili).

Stato attuale: il progetto ha avviato bene il pilastro 1 (CI base), ma è ancora **a metà percorso** su 2-3-4.

## Roadmap consigliata (30-45 giorni)

### Sprint A (settimana 1-2)
- Aggiungere in CI: `lint`, `detekt` (o equivalente), report coverage Jacoco.
- Definire branch protection e policy merge.
- Inserire checklist release minima (`versionCode`, changelog, smoke test).

### Sprint B (settimana 3-4)
- Refactor progressivo di `MainActivity`/`HomeworkScreens` in feature + ViewModel.
- Introdurre test integration su repository/engine e 2-3 test UI critici.

### Sprint C (settimana 5-6)
- Integrare crash reporting e metriche stabilità.
- Eseguire closed testing con KPI di qualità (crash-free, completion rate, bug severity).
- Go/No-Go meeting con scorecard oggettiva.

## KPI go/no-go suggeriti
- Crash-free sessions >= 99.5%
- 0 bug blocker (P0/P1) aperti
- CI stability >= 95% negli ultimi 14 giorni
- Copertura test su domain/core >= 65%
- Tempo feedback PR (CI completa) <= 10-12 minuti

## Conclusione

L’app oggi è in uno stato **solido da beta avanzata con CI attiva**.

Con i miglioramenti proposti (quality gates completi, refactor aree monolitiche, operabilità produzione), può raggiungere rapidamente un livello **7.5-8/10**, compatibile con una pubblicazione pubblica a rischio controllato secondo standard di software house mature.
