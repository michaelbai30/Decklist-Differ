/**
 * DeckListDiffer.java
 * 
 * Application Entry Point
 * 
 * Handles HTTP routes using Spark including:
 * - The homepage (GET "/") that displays deck comparison form
 * - The comparison endpoint (POST "/compare") that parses two decklists,
 *   computes differences, generates statistics, prepares grouped HTML output,
 *   and builds downloable TXT files.
 * - The download endpoints (GET "/download/...") that returns generated files.
 * 
 * Function:
 * - Accept decklist text input from form.
 * - Parse decklist using DeckParser.
 * - Identify cards to add, remove, or in common.
 * - Compute type counts using CardStats.
 * - Compute upgrade cost using PricingService.
 * - Produce grouped HTML summaries using CardGrouping.
 * - Store generated text files in memory for download.
 */

package com.deckdiffer.server;
import static spark.Spark.*;

import java.util.*;
import org.json.JSONObject;

import com.deckdiffer.grouping.CardGrouping;
import com.deckdiffer.info.PricingService;
import com.deckdiffer.parsing.DeckParser;
import com.deckdiffer.stats.CardStats;

import org.json.JSONArray;

public class DeckListDiffer{
    // Maps: file name -> file content
    private static final Map<String, String> generatedTxtFiles = new HashMap<>();

    public static void main(String[] args) {
        port(4567);
        staticFiles.location("/public");

        // Upload Form HTML
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

        // ---- Handle Uploaded Decks ---
        post("/compare", (req, res) -> {

            String baseDeck = req.queryParams("baseText");
            String upgradedDeck = req.queryParams("upgradedText");

            // card name -> card count
            Map<String, Integer> baseMap = DeckParser.parseDeck(baseDeck);
            Map<String, Integer> upgradedMap = DeckParser.parseDeck(upgradedDeck);

            Map<String, Integer> cardsToAdd = new LinkedHashMap<>();
            Map<String, Integer> cardsToRemove = new LinkedHashMap<>();
            Map<String, Integer> cardsInCommon = new LinkedHashMap<>();

            // get type counts
            Map<String, Integer> oldTypeCounts = CardStats.computeTypeCounts(baseMap);
            Map<String, Integer> newTypeCounts = CardStats.computeTypeCounts(upgradedMap);

            // Compare by Quantities
            Set<String> allCards = new HashSet<>();
            allCards.addAll(baseMap.keySet());
            allCards.addAll(upgradedMap.keySet());

            for (String card : allCards) {
                int baseCount = baseMap.getOrDefault(card, 0);
                int upgradeCount = upgradedMap.getOrDefault(card, 0);

                // need to add cards
                if (upgradeCount > baseCount) {
                    cardsToAdd.put(card, upgradeCount - baseCount);
                }

                // need to remove cards
                else if (baseCount > upgradeCount) {
                    cardsToRemove.put(card, baseCount - upgradeCount);
                }

                int common = Math.min(baseCount, upgradeCount);
                if (common > 0) {
                    cardsInCommon.put(card, common);
                }
            }

            double totalUpgradeCost = PricingService.getTotalCost(cardsToAdd);

            Map<String, Integer> sortedAdd = new TreeMap<>(cardsToAdd);
            Map<String, Integer> sortedRemove = new TreeMap<>(cardsToRemove);
            Map<String, Integer> sortedInCommon = new TreeMap<>(cardsInCommon);

            // Generate downloadable files instead of writing them to disk 
            generatedTxtFiles.put("cards_to_add.txt", String.join("\n", DeckParser.mapToLines(sortedAdd)));
            generatedTxtFiles.put("cards_to_remove.txt", String.join("\n", DeckParser.mapToLines(sortedRemove)));
            generatedTxtFiles.put("cards_in_common.txt", String.join("\n", DeckParser.mapToLines(sortedInCommon)));
            generatedTxtFiles.put("cards_to_add_detailed.txt", CardGrouping.buildDetailedTxtFile(cardsToAdd));
            generatedTxtFiles.put("cards_to_remove_detailed.txt", CardGrouping.buildDetailedTxtFile(cardsToRemove));
            generatedTxtFiles.put("cards_in_common_detailed.txt", CardGrouping.buildDetailedTxtFile(cardsInCommon));

            // Build HTTP Response
            StringBuilder html = new StringBuilder();
            html.append("""
                <html>
                <head>
                    <title>Deck Comparison Result</title>
                    <style>
                        body { font-family: Arial; margin: 40px; }
                        ul { line-height: 1.5; }
                    </style>
                </head>
                <body>
                    <h1>Deck Comparison Results</h1>
            """);

            // Total Upgrade Cost
            html.append("<p><b>Total Upgrade Cost:</b> $").append(String.format("%.2f", totalUpgradeCost)).append("</p><hr>");

            // Changes in Type Count
            html.append("<h2>Changes in Card Type</h2><ul>");
            Set<String> allTypes = new TreeSet<>();
            allTypes.addAll(oldTypeCounts.keySet());
            allTypes.addAll(newTypeCounts.keySet());

            for (String type : allTypes) {
                int oldCount = oldTypeCounts.getOrDefault(type, 0);
                int newCount = newTypeCounts.getOrDefault(type, 0);
                String sign = ((newCount - oldCount) >= 0) ? "+" : "-";
                String diff = Integer.toString(Math.abs(newCount - oldCount));

                html.append("<li><b>")
                    .append(type)
                    .append(":</b> ")
                    .append(oldCount)
                    .append(" → ")
                    .append(newCount)
                    .append(" (")
                    .append(sign)
                    .append(diff)
                    .append(")")
                    .append("</li>");
            }

            html.append("</ul><hr>");

            // Cards to Remove HTML
            html.append(CardGrouping.buildGroupedHtml("Cards to Remove", cardsToRemove, false));
            html.append("</ul><hr>");

            // Cards to Add HTML
            html.append(CardGrouping.buildGroupedHtml("Cards to Add", cardsToAdd, true));
            html.append("</ul><hr>");

            // Cards in Common HTML
            html.append(CardGrouping.buildGroupedHtml("Cards in Common", cardsInCommon, false));
            html.append("</ul><hr>");
            
            // Total Cost and Download Links
            
            html.append("<hr><h3>Download Results</h3>")
                .append("<a href='/download/cards_to_add'>Download cards_to_add.txt</a><br>") // Generates download link. Clicking it sends get request
                .append("<a href='/download/cards_to_remove'>Download cards_to_remove.txt</a><br>")
                .append("<a href='/download/cards_in_common'>Download cards_in_common.txt</a><br><br>")
                .append("<a href='/download/cards_to_add_detailed'>Download cards_to_add_detailed.txt</a><br>")
                .append("<a href='/download/cards_to_remove_detailed'>Download cards_to_remove_detailed.txt</a><br>")
                .append("<a href='/download/cards_in_common_detailed'>Download cards_in_common_detailed.txt</a><br><br>")
                .append("<a href='/'>Compare Another Deck</a>")
                .append("</body></html>");

            return html.toString();
        });

        // Create Download Endpoint and
        // : indicates dynamic url path, so pass in :filename as a param
        // Initiates download and deals with get request
        get("/download/:filename", (req, res) -> {
            String fileName = req.params(":filename") + ".txt";

            if (!generatedTxtFiles.containsKey(fileName)){
                res.status(404);
                return "File Not Found";
            }

            res.type("text/plain");
            res.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            return generatedTxtFiles.get(fileName);
        });

        get("/download/:filename_detailed", (req, res) -> {
            String fileName = req.params(":filename_detailed") + ".txt";

            if (!generatedTxtFiles.containsKey(fileName)){
                res.status(404);
                return "File Not Found";
            }

            res.type("text/plain");
            res.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            return generatedTxtFiles.get(fileName);
        });
    }
}
