# Toolchain Java ufficiale (build)

Questo progetto usa **Java 21 LTS** come versione ufficiale **solo** per la toolchain di sviluppo:

- esecuzione di Gradle
- Android Gradle Plugin (AGP)
- Kotlin Gradle plugin / Kotlin DSL

## Cosa non cambia

L'uso di Java 21 per la build **non** modifica:

- runtime Android sui dispositivi
- logica applicativa
- UX
- dati utente
- minSdk / targetSdk
- target bytecode Android, che resta configurato dal progetto/AGP

## Versione consigliata

Il progetto dichiara in `.mise.toml` la patch stabile consigliata:

- `java = "corretto-21.0.10.7.1"`

## Uso locale rapido

Con `mise`:

```bash
mise trust
mise install
```

Poi eseguire i comandi Gradle normalmente (es. `./gradlew tasks`).
