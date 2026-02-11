# Phase 0 Runbook (Build deterministica + baseline release)

Questa runbook implementa la **Fase 0** del piano: ripristino pipeline build locale ripetibile e gate minimi prima del merge.

## 1) Toolchain bloccata

- Java: Corretto 21 (gestito via `mise`)
- Gradle wrapper: `8.13`
- AGP: `8.13.2`
- Kotlin plugin: `2.0.21`

## 2) Setup locale minimo

```bash
mise trust
mise install
cp local.properties.example local.properties
# imposta sdk.dir nel file local.properties con il path del tuo Android SDK
```

Se lavori dietro proxy TLS aziendale, importa il certificato nel truststore Java usato da Gradle.

## 3) Matrice build minima (gate Fase 0)

```bash
./gradlew help --no-daemon
./gradlew assembleDebug --no-daemon
./gradlew testDebugUnitTest --no-daemon
```

## 4) Baseline release

Prima della release interna:

1. incrementa `versionCode`
2. aggiorna `versionName`
3. annota changelog release (anche inizialmente in forma manuale)

## 5) Policy merge (obbligatoria)

- PR bloccata se uno dei 3 comandi del gate fallisce.
- Nessuna eccezione senza issue di tracking e owner assegnato.
