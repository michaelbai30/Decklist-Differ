# DeckList Differ for Magic: the Gathering, MTG

A lightweight web-based utility to compare a base Magic: The Gathering (MTG) decklist with a prospective upgraded decklist,
highlighing cards that were added, removed, or kept, along with **real-time card prices** fetched from Scryfall API.
<br>

**Quickly find what's new and what it'll cost you!** <br>

## Purpose and Usage

DeckList Differ helps MTG players quickly see **what changes** exist between two decklists and **how much upgrading will cost**.

To use, upload two `.txt` decklists (base and upgraded) in the browser app.
<br>
The app will: <br>
- Parse both decklists line by line <br>
- Compare card names and quantities <br>
- Fetch live prices from Scryfall <br>
- Output <br>
  - Cards to add (with current prices) <br>
  - Cards to remove <br>
  - Cards in common <br>
  - Total upgrade cost <br>
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
`javac -cp "libs/*" DeckListDifferServer.java` <br>
`java -cp ".:libs/*" DeckListDifferServer` <br>

If you are using Maven: <br>
`mvn compile exec:java -Dexec.mainClass=DeckListDifferServer` <br>

After that, open in browser by visiting `http://localhost:4567`

## Author
DeckList Differ - a lightweight MTG deck comparison tool by Michael Bai <br>
