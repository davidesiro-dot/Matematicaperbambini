# DEV_SETUP

## Prerequisiti
- **Java ufficiale:** **Java 21 LTS**
- Android SDK installato localmente

## JAVA_HOME (Windows / PowerShell)
```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

## Android SDK
Esempio `local.properties`:
```properties
sdk.dir=C:\Users\<USER>\AppData\Local\Android\Sdk
```

## Verifica ambiente (comandi minimi)
```powershell
.\gradlew help
.\gradlew assembleDebug
.\gradlew testDebugUnitTest
```

## Policy di merge
**Non si merge se:**
- build fallisce
- test falliscono
- lint fallisce
