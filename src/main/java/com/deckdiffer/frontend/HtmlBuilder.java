/**
 * HtmlBuilder.java; Constructs HTML response pages
 *
 * Goal:
 * - Build the comparison result page HTML
 * - Render grouped card sections using grouping methods from CardGrouping
 * - Display the per-type count comparison between Deck 1 and Deck 2
 * - Display cost differences for cards unique to each deck
 * - Provide download links for the new comparison model
 */

package com.deckdiffer.frontend;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.deckdiffer.grouping.CardGrouping;
import com.deckdiffer.parsing.DeckParser;

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

                    .card-color-header {
                        margin: 4px 0;
                        font-weight: bold;
                    }

                    .card-link {
                        text-decoration: none;
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
                </style>
            </head>
            <body>
                <h1>Deck Comparison Results</h1>
        """);

        // Cost Summary
        html.append("<div class='cost-box'>")
            .append("<h2>Cost Difference Summary</h2>")
            .append("<p><b>Deck 1 Only Value:</b> $")
            .append(String.format("%.2f", deck1DiffCost))
            .append("</p>")
            .append("<p><b>Deck 2 Only Value:</b> $")
            .append(String.format("%.2f", deck2DiffCost))
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

        // Deck 1 Only
        html.append("""
            <div class='section-header' onclick="toggleSection('sec1', this)">
            <span class="arrow">▶</span> In Deck 1, Not in Deck 2 (""")
            .append(DeckParser.sumCounts(deck1Only)).append(")")
            .append("""
            </div>
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
            </div>
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
            </div>
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

                    const isClosed = (e.style.display ==="none" || e.style.display === "");

                    if (isClosed){
                        e.style.display = "block";
                        if (arrow){
                            arrow.textContent = "▼";
                        }
                    }
                    else{
                        e.style.display = "none";
                        if (arrow){
                            arrow.textContent = "▶";
                        }
                    }
                }
            </script>
        """);

        html.append("</body></html>");

        return html.toString();
    }
}
