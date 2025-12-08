/**
 * HtmlBuilder.java; Constructs HTML response pages
 *
 * Goal:
 * - Build the comparison result page HTML
 * - Render grouped card sections for cards in deck 1, deck 2, and in common
 * - Display the per-type count comparison between deck 1 and deck 2
 * - Display cost differences for cards unique to each deck, and total deck prices
 * - Provide download links and clipboard copying for comparing deck 1 and deck 2 cards.
 */

package com.deckdiffer.frontend;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.deckdiffer.grouping.CardGrouping;
import com.deckdiffer.parsing.DeckParser;
import com.deckdiffer.stats.DeckStats;
import com.deckdiffer.stats.DeckStats.DeckStat;
import com.deckdiffer.stats.DeckStats.ManaStats;

public class HtmlBuilder {

    private HtmlBuilder() {}

    /**
     * HTML for the comparison results page
     *
     * @param deck1Only       Cards unique to Deck 1
     * @param deck2Only       Cards unique to Deck 2
     * @param common          Cards shared between Deck 1 + Deck 2
     * @param deck1TypeCounts Type counts for Deck 1
     * @param deck2TypeCounts Type counts for Deck 2
     * @param deck1DiffCost   Total cost of cards only in Deck 1
     * @param deck2DiffCost   Total cost of cards only in Deck 2
     * @return HTML page as string
     */
    public static String buildResultsPage(
            Map<String, Integer> deck1Only,
            Map<String, Integer> deck2Only,
            Map<String, Integer> common,
            Map<String, Integer> deck1TypeCounts,
            Map<String, Integer> deck2TypeCounts,
            double deck1DiffCost,
            double deck2DiffCost)
    {
        // Compute all deck stats in one call per deck
        DeckStat stats1 = DeckStats.computeDeckStats(deck1Only, common);
        DeckStat stats2 = DeckStats.computeDeckStats(deck2Only, common);

        ManaStats d1 = stats1.mana;
        ManaStats d2 = stats2.mana;

        StringBuilder html = new StringBuilder();

        html.append("""
            <html>
            <head>
                <title>Deck Comparison Result</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; }
                    ul { line-height: 1.5; }
                    .cost-box {
                        background:#f4f4f4;
                        padding:15px;
                        margin:10px 0 20px 0;
                        border-radius:6px;
                    }

                    /* Grid-based card display */
                    .card-grid {
                        display: flex;
                        flex-wrap: wrap;
                        flex-direction: row;
                        gap: 12px;
                        margin: 8px 0 24px;
                    }

                    .card-tile {
                        position: relative;
                        width: 180px;
                        transition: transform 0.15s ease, box-shadow 0.2s ease;
                    }

                    .card-tile img {
                        width: 100%;
                        border-radius: 8px;
                        box-shadow: 0 4px 10px rgba(0,0,0,0.3);
                        display: block;
                        transition: transform 0.15s ease;
                    }

                    .card-tile:hover {
                        transform: scale(1.05);
                        box-shadow: 0 8px 20px rgba(0,0,0,0.4);
                    }

                    .card-tile:hover img {
                        transform: scale(1.10);
                    }

                    .card-count-badge {
                        position: absolute;
                        top: 6px;
                        right: 6px;
                        background: rgba(0,0,0,0.75);
                        color: #fff;
                        padding: 2px 6px;
                        border-radius: 999px;
                        font-size: 12px;
                        font-weight: bold;
                    }

                    /* Collapsible section controls */
                    .section-header {
                        cursor: pointer;
                        font-weight: bold;
                        margin: 20px 0 10px;
                        padding: 10px;
                        background: #eaeaea;
                        border-radius: 6px;
                    }

                    .section-content {
                        display: none;
                        margin-left: 10px;
                    }

                    .section-header:hover {
                        background: #dcdcdc;
                    }

                    .arrow {
                        font-size: 14px;
                        color: #555;
                        margin-right: 8px;
                    }

                    .copy-btn {
                        margin-left: 8px;
                        background: #ddd;
                        border: 1px solid #aaa;
                        border-radius: 4px;
                        padding: 2px 6px;
                        font-size: 12px;
                        cursor: pointer;
                    }

                    .copy-btn:hover {
                        background: #ccc;
                    }
                </style>
            </head>
            <body>
                <h1>Deck Comparison Results</h1>
        """);

        // Price Summary
        html.append("<div class='cost-box'>")
            .append("<h2>Deck Prices</h2>")
            .append("<p><b>Deck 1 Price:</b> $")
            .append(String.format("%.2f", stats1.totalCost))
            .append("</p>")
            .append("<p><b>Deck 2 Price:</b> $")
            .append(String.format("%.2f", stats2.totalCost))
            .append("</p>")

            .append("<h3>Difference (Upgrades Needed)</h3>")
            .append("<p><b>Deck 1 Only Prices:</b> $")
            .append(String.format("%.2f", stats1.onlyDiffCost))
            .append("</p>")
            .append("<p><b>Deck 2 Only Prices:</b> $")
            .append(String.format("%.2f", stats2.onlyDiffCost))
            .append("</p>")
            .append("</div><hr>");

        // Type Difference Summary
        html.append("<h2>Card Type Differences</h2><ul>");

        Set<String> allTypes = new TreeSet<>();
        allTypes.addAll(deck1TypeCounts.keySet());
        allTypes.addAll(deck2TypeCounts.keySet());

        for (String type : allTypes) {
            int count1 = deck1TypeCounts.getOrDefault(type, 0);
            int count2 = deck2TypeCounts.getOrDefault(type, 0);
            int diff = count2 - count1;

            html.append("<li><b>")
                .append(type)
                .append(":</b> ")
                .append(count1)
                .append(" vs. ")
                .append(count2)
                .append(" (")
                .append(Math.abs(diff))
                .append(")</li>");
        }

        html.append("</ul><hr>");

        // Mana Curve and Pips Section
        html.append("<h2>Mana Curve and Color Breakdown</h2>");

        html.append("<table border='1' cellpadding='6' cellspacing='0'>")
            .append("<tr><th></th><th>Deck 1</th><th>Deck 2</th></tr>")
            .append("<tr><td><b>Average CMC</b></td>")
            .append("<td>").append(String.format("%.2f", d1.getAverageCMC())).append("</td>")
            .append("<td>").append(String.format("%.2f", d2.getAverageCMC())).append("</td></tr>");

        String[] colors = {"C","W","U","B","R","G"};
        String[] labels = {"Colorless","White","Blue","Black","Red","Green"};

        for (int i = 0; i < colors.length; i++) {
            html.append("<tr><td><b>").append(labels[i]).append(" Pips</b></td>")
                .append("<td>").append(d1.totalPips.get(colors[i])).append("</td>")
                .append("<td>").append(d2.totalPips.get(colors[i])).append("</td></tr>");
        }
        html.append("</table><hr>");

        // Deck 1 Only
        html.append("""
            <div class='section-header' onclick="toggleSection('sec1', this)">
                <span class="arrow">▶</span> In Deck 1, Not in Deck 2 (""")
            .append(DeckParser.sumCounts(deck1Only)).append(")")
            .append("""
                <button class='copy-btn' onclick="copySection(event, 'deck1only-copy')">Copy</button>
            </div>
            <textarea id='deck1only-copy' style='display:none;'>""")
            .append(CardGrouping.buildNonDetailedTxtFile(deck1Only))
            .append("""
            </textarea>
            <div class='section-content' id='sec1'>
        """);
        html.append(CardGrouping.buildGroupedHtml(deck1Only));
        html.append("</div><hr>");

        // Deck 2 Only
        html.append("""
            <div class='section-header' onclick="toggleSection('sec2', this)">
                <span class="arrow">▶</span> In Deck 2, Not in Deck 1 (""")
            .append(DeckParser.sumCounts(deck2Only)).append(")")
            .append("""
                <button class='copy-btn' onclick="copySection(event,'deck2only-copy')">Copy</button>
            </div>
            <textarea id='deck2only-copy' style='display:none;'>""")
            .append(CardGrouping.buildNonDetailedTxtFile(deck2Only))
            .append("""
            </textarea>
            <div class='section-content' id='sec2'>
        """);
        html.append(CardGrouping.buildGroupedHtml(deck2Only));
        html.append("</div><hr>");

        // Common Cards
        html.append("""
            <div class='section-header' onclick="toggleSection('sec3', this)">
                <span class="arrow">▶</span> Common in Both Decks (""")
            .append(DeckParser.sumCounts(common)).append(")")
            .append("""
                <button class='copy-btn' onclick="copySection(event, 'common-copy')">Copy</button>
            </div>
            <textarea id='common-copy' style='display:none;'>""")
            .append(CardGrouping.buildNonDetailedTxtFile(common))
            .append("""
            </textarea>
            <div class='section-content' id='sec3'>
        """);
        html.append(CardGrouping.buildGroupedHtml(common));
        html.append("</div>");

        // Download Links
        html.append("""
            <hr><h3>Download Results</h3>

            <a href='/download/deck1_only'>Download deck1_only.txt</a><br>
            <a href='/download/deck2_only'>Download deck2_only.txt</a><br>
            <a href='/download/common_cards'>Download common_cards.txt</a><br><br>

            <a href='/download/deck1_only_detailed'>Download deck1_only_detailed.txt</a><br>
            <a href='/download/deck2_only_detailed'>Download deck2_only_detailed.txt</a><br>
            <a href='/download/common_cards_detailed'>Download common_cards_detailed.txt</a><br><br>

            <a href='/'>Compare Another Pair of Decks</a>
        """);

        // Collapsible toggle
        html.append("""
            <script>
                function toggleSection(id, headerEl) {
                    const e = document.getElementById(id);
                    if (!e) return;

                    const arrow = headerEl.querySelector('.arrow');
                    const isClosed = (e.style.display === "none" || e.style.display === "");

                    if (isClosed){
                        e.style.display = "block";
                        if (arrow){
                            arrow.textContent = "▼";
                        }
                    } else {
                        e.style.display = "none";
                        if (arrow){
                            arrow.textContent = "▶";
                        }
                    }
                }
                function copySection(event, id){
                    event.stopPropagation();
                    const e = document.getElementById(id);
                    if (!e){
                        return;
                    }
                    navigator.clipboard.writeText(e.value);
                }
            </script>
        """);

        html.append("</body></html>");

        return html.toString();
    }
}
