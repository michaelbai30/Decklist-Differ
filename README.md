# DeckList Differ for Magic: the Gathering, MTG

A lightweight web-based utility to compare two decks in Magic: The Gathering (MTG),
highting cards that are present only in deck1, deck2, or are in common.
Features **real-time card prices** fetched from Scryfall API as well as categorized and sorted data.
<br>

## Purpose and Usage

DeckList Differ helps MTG players quickly see **what differences** exist between two decklists and **how much difference in money the decklists are**.

To use, upload two `.txt` decklists (deck1 and deck2) in the browser app.
<br>
The app will: <br>
- Parse both decklists line by line <br>
- Compare card names and quantities with primary type and color category label <br>
- Fetch live prices from Scryfall <br>
- Output <br>
  - Cards in deck 1 only <br>
  - Cards in deck2 only <br>
  - Cards in common <br>
  - Cost of the cards in deck 1/2 only <br>
- Generate **downloadable text files** for each list

**NOTE: Please ensure that decklists are formatted in the standard plaintext format for MTG. <br>
Most websites that allow you export decks as plaintext follow this format.<br>
Each line should list the number of copies followed by the card name, like so:** <br>

```
4 Lightning Bolt
2 Mountain
4 Chaos Warp
```

## Stack
Java 17+ <br>
Maven <br>
Spark Java - Lightweight web framework <br>
Jetty - Lightweight embedded HTML server <br>
Scryfall API - Real-time card price data<br>

## Requirements
Java 17+ <br>
Maven <br>
JsonObject (if running without Maven) <br>

## Running Locally
`git clone https://github.com/michaelbai30/Decklist-Differ.git` <br>
`cd Decklist-Differ`

If you are using raw Java file with no build tools: <br>
`javac -cp "libs/*" DeckListDiffer.java` <br>
`java -cp ".:libs/*" DeckListDiffer` <br>

If you are using Maven: <br>
`mvn compile exec:java -Dexec.mainClass=com.deckdiffer.server.DeckListDiffer` <br>

After that, open in browser by visiting `http://localhost:4567`

## Author
DeckList Differ - a lightweight MTG deck comparison tool by Michael Bai <br>
