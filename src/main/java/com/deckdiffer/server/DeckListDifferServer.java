/**
 * DeckListDifferServer.java; Main web server for DeckList Differ.
 *
 * Goal:
 * - Configure Spark Server
 * - Define web routes
 * - Uses DeckComparer for diff logic
 * - HtmlBuilder for HTML formatting and layout
 * - DownloadService for file storage and downloable files
 */

package com.deckdiffer.server;

import static spark.Spark.*;

import java.util.*;

import com.deckdiffer.grouping.CardGrouping;
import com.deckdiffer.logic.DeckComparer;
import com.deckdiffer.download.DownloadService;
import com.deckdiffer.frontend.HtmlBuilder;
import com.deckdiffer.parsing.DeckParser;
import com.deckdiffer.stats.DeckStats;

public class DeckListDifferServer {

    public static void main(String[] args) {

        port(4567);
        staticFiles.location("/public");

        // ===== Homepage =====
        get("/", (req, res) -> """
            <html>
            <head>
                <title>Decklist Differ</title>
                <style>
                    body { font-family: Arial; margin: 40px; }
                    textarea { width: 100%; height: 200px; margin-bottom: 20px; }
                    button { padding: 10px 20px; font-size: 16px;}
                </style>
            </head>
            <body>
                <h1>Decklist Differ</h1>
                <form action='/compare' method='post'>
                <p><b>Deck 1:</b></p>
                <textarea name='deck1Text' placeholder='e.g.:
1 Swords to Plowshares
1 Chaos Warp
1 Lightning Bolt
...'></textarea>

                <p><b>Deck 2:</b></p>
                <textarea name='deck2Text' placeholder='e.g.:
1 Swords to Plowshares
1 Chaos Warp
1 Blasphemous Act
...'></textarea>

                <button type='submit'>Compare Decks</button>
                </form>
            </body>
            </html>
        """);

        // ===== Deck Comparison =====
        post("/compare", (req, res) -> {

            String deck1Text= req.queryParams("deck1Text");
            String deck2Text = req.queryParams("deck2Text");

            if (deck1Text == null || deck1Text.isBlank() || deck2Text == null || deck2Text.isBlank()) {
                res.status(400);
                return "<h2>Please enter both deck lists</h2><a href='/'>Go Back</a>";
            }

            // Parse decks and normalize names
            Map<String, Integer> deck1Map = DeckParser.parseDeck(deck1Text);
            Map<String, Integer> deck2Map = DeckParser.parseDeck(deck2Text);
            deck1Map = DeckParser.normalizeNames(deck1Map);
            deck2Map = DeckParser.normalizeNames(deck2Map);


            // Compute comparison
            Map<String, Integer> deck1Only = DeckComparer.computeDeck1Only(deck1Map, deck2Map);   // in 1 not 2
            Map<String, Integer> deck2Only = DeckComparer.computeDeck2Only(deck1Map, deck2Map);   // in 2 not 1
            Map<String, Integer> inBoth = DeckComparer.computeCardsInCommon(deck1Map, deck2Map);

            // Compute type count changes
            Map<String, int[]> typeChanges  = DeckComparer.computeTypeDifferences(deck1Map, deck2Map);

            Map<String, Integer> deck1Types = new LinkedHashMap<>();
            Map<String, Integer> deck2Types = new LinkedHashMap<>();

            for (var e : typeChanges.entrySet()) {
                deck1Types.put(e.getKey(), e.getValue()[0]);
                deck2Types.put(e.getKey(), e.getValue()[1]);
            }

            DeckStats.DeckStat stats1 = DeckStats.computeDeckStats(deck1Only, inBoth);
            DeckStats.DeckStat stats2 = DeckStats.computeDeckStats(deck2Only, inBoth);

            double deck1DiffCost = stats1.onlyDiffCost;
            double deck2DiffCost = stats2.onlyDiffCost;

            // Generate all downloadable files using CardGrouping
            DownloadService.saveFile("deck1_only.txt",
            CardGrouping.buildNonDetailedTxtFile(deck1Only));

            DownloadService.saveFile("deck2_only.txt",
            CardGrouping.buildNonDetailedTxtFile(deck2Only));

            DownloadService.saveFile("common_cards.txt",
            CardGrouping.buildNonDetailedTxtFile(inBoth));

            // Detailed
            DownloadService.saveFile("deck1_only_detailed.txt",
            CardGrouping.buildDetailedTxtFile(deck1Only));

            DownloadService.saveFile("deck2_only_detailed.txt",
            CardGrouping.buildDetailedTxtFile(deck2Only));

            DownloadService.saveFile("common_cards_detailed.txt",
            CardGrouping.buildDetailedTxtFile(inBoth));

            return HtmlBuilder.buildResultsPage(
                deck1Only,
                deck2Only,
                inBoth,
                deck1Types,
                deck2Types,
                deck1DiffCost,
                deck2DiffCost
            );
        });

        
        // ===== Download Route =====
        get("/download/:filename", (req, res) -> {
            String fileName = req.params(":filename") + ".txt";

            if (!DownloadService.exists(fileName)) {
                res.status(404);
                return "File Not Found";
            }

            res.type("text/plain");
            res.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            return DownloadService.getFile(fileName);
        });

        
        get("/download/:filename_detailed", (req, res) -> {
            String fileName = req.params(":filename_detailed") + ".txt";

            if (!DownloadService.exists(fileName)) {
                res.status(404);
                return "File Not Found";
            }

            res.type("text/plain");
            res.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            return DownloadService.getFile(fileName);
        });
    }
}
