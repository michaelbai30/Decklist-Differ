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
import com.deckdiffer.frontend.HtmlBuilder;
import com.deckdiffer.parsing.DeckParser;
import com.deckdiffer.info.DownloadService;
import com.deckdiffer.info.CardInfoService;;

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
                <p><b>Base Decklist:</b></p>
                <textarea name='baseText' placeholder='e.g.:
1 Swords to Plowshares
1 Chaos Warp
1 Lightning Bolt
...'></textarea>

                <p><b>Upgraded Decklist:</b></p>
                <textarea name='upgradedText' placeholder='e.g.:
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

            String baseDeck = req.queryParams("baseText");
            String upgradedDeck = req.queryParams("upgradedText");

            // Parse decks
            Map<String, Integer> baseMap = DeckParser.parseDeck(baseDeck);
            Map<String, Integer> upgradedMap = DeckParser.parseDeck(upgradedDeck);

            // Compute adds / removes / common cards
            Map<String, Integer> cardsToAdd = DeckComparer.computeCardsToAdd(baseMap, upgradedMap);
            Map<String, Integer> cardsToRemove = DeckComparer.computeCardsToRemove(baseMap, upgradedMap);
            Map<String, Integer> cardsInCommon = DeckComparer.computeCardsInCommon(baseMap, upgradedMap);

            // Compute type count changes
            Map<String, int[]> typeChanges  = DeckComparer.computeTypeChanges(baseMap, upgradedMap);

            // Convert typeChanges → old/new count maps
            Map<String, Integer> oldTypeCounts = new LinkedHashMap<>();
            Map<String, Integer> newTypeCounts = new LinkedHashMap<>();

            for (var e : typeChanges.entrySet()) {
                oldTypeCounts.put(e.getKey(), e.getValue()[0]);
                newTypeCounts.put(e.getKey(), e.getValue()[1]);
            }

            // Compute total cost of additions
            double totalUpgradeCost = CardInfoService.getTotalCost(cardsToAdd);

            // Generate all downloadable files using CardGrouping
            DownloadService.saveFile("cards_to_add.txt",
                String.join("\n", CardGrouping.buildDetailedTxtFile(cardsToAdd).split("\n")));

            DownloadService.saveFile("cards_to_remove.txt",
                String.join("\n", CardGrouping.buildDetailedTxtFile(cardsToRemove).split("\n")));

            DownloadService.saveFile("cards_in_common.txt",
                String.join("\n", CardGrouping.buildDetailedTxtFile(cardsInCommon).split("\n")));

            // Detailed versions
            DownloadService.saveFile("cards_to_add_detailed.txt",
                CardGrouping.buildDetailedTxtFile(cardsToAdd));

            DownloadService.saveFile("cards_to_remove_detailed.txt",
                CardGrouping.buildDetailedTxtFile(cardsToRemove));

            DownloadService.saveFile("cards_in_common_detailed.txt",
                CardGrouping.buildDetailedTxtFile(cardsInCommon));

            return HtmlBuilder.buildResultsPage(
                cardsToAdd,
                cardsToRemove,
                cardsInCommon,
                oldTypeCounts,
                newTypeCounts,
                totalUpgradeCost
            );
        });

        
        // ===== Download Route =====
        // Create Download Endpoint and
        // : indicates dynamic url path, so pass in :filename as a param
        // Initiates download and deals with get request
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
