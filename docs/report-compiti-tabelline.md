# Report: Compiti – tabelline multiple e avanzamento

## Obiettivo
Permettere ai genitori di scegliere più tabelline (es. 7, 8, 9) nei compiti e risolvere il blocco di avanzamento alla fine della tabellina in modalità compiti.

## Soluzione proposta
- **Selezione multipla tabelline**: aggiunta di una griglia di pulsanti (1–10) nella configurazione dei compiti. Se l’utente seleziona una o più tabelline, il generatore usa la lista selezionata; se non seleziona nulla, resta attiva la tabellina predefinita.
- **Generazione esercizi**: il motore estrae casualmente una tabellina dalla lista selezionata per ogni esercizio nelle modalità “Tabellina”, “Buchi”, “Al contrario” e “Scelta multipla”.
- **Avanzamento in compiti**: per la tabellina classica e per i “Buchi”, viene mostrato un pulsante “Avanti” una volta completato l’esercizio, così l’utente può passare esplicitamente all’esercizio successivo.

## Impatti UI/UX
- Migliora la configurazione per i genitori: consente di concentrare l’esercizio su più tabelline senza dover creare compiti separati.
- Evita lo stallo a fine esercizio nelle tabelline in modalità compiti, rendendo chiaro come proseguire.

## Verifica suggerita
1. Abilitare “Tabellina” nei compiti.
2. Selezionare le tabelline 7, 8 e 9 dalla griglia.
3. Avviare il compito e verificare che gli esercizi alternino le tabelline selezionate.
4. Completare una tabellina in modalità compiti e usare il pulsante “Avanti” per proseguire.
