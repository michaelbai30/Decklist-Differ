/**
 * HtmlBuilder.java; Constructs HTML response pages
 *
 * Goal: 
 * - Build the comparison result page HTML
 * - Render grouped card sections using grouping methods from CardGrouping
 * - Displaying type count changes and total price
 */

package com.deckdiffer.frontend;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.deckdiffer.grouping.CardGrouping;

public class HtmlBuilder {

    private HtmlBuilder() {}

    /**
     * HTML for the comparison results page
     * @param cardsToAdd
     * @param cardsToRemove
     * @param cardsInCommon
     * @param oldTypeCounts
     * @param newTypeCounts
     * @param totalUpgradeCost
     * @return HTML page as a string
     */
    public static String buildResultsPage(
            Map<String, Integer> cardsToAdd,
            Map<String, Integer> cardsToRemove,
            Map<String, Integer> cardsInCommon,
            Map<String, Integer> oldTypeCounts,
            Map<String, Integer> newTypeCounts,
            double totalUpgradeCost)
    {
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
        html.append("<p><b>Total Upgrade Cost:</b> $")
            .append(String.format("%.2f", totalUpgradeCost))
            .append("</p><hr>");

        // Type differences
        html.append("<h2>Changes in Card Type</h2><ul>");

        Set<String> allTypes = new TreeSet<>();
        allTypes.addAll(oldTypeCounts.keySet());
        allTypes.addAll(newTypeCounts.keySet());

        for (String type : allTypes) {
            int oldCount = oldTypeCounts.getOrDefault(type, 0);
            int newCount = newTypeCounts.getOrDefault(type, 0);

            String sign = ((newCount - oldCount) > 0) ? "+" : "-";
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
                .append(")</li>");
        }

        html.append("</ul><hr>");

        // Cards to Remove
        html.append(CardGrouping.buildGroupedHtml("Cards to Remove", cardsToRemove, false));

        // Cards to Add (with price)
        html.append(CardGrouping.buildGroupedHtml("Cards to Add", cardsToAdd, true));

        // Cards in Common
        html.append(CardGrouping.buildGroupedHtml("Cards in Common", cardsInCommon, false));

        // Download links
        html.append("""
            <hr><h3>Download Results</h3>
            <a href='/download/cards_to_add'>Download cards_to_add.txt</a><br>
            <a href='/download/cards_to_remove'>Download cards_to_remove.txt</a><br>
            <a href='/download/cards_in_common'>Download cards_in_common.txt</a><br><br>
            <a href='/download/cards_to_add_detailed'>Download cards_to_add_detailed.txt</a><br>
            <a href='/download/cards_to_remove_detailed'>Download cards_to_remove_detailed.txt</a><br>
            <a href='/download/cards_in_common_detailed'>Download cards_in_common_detailed.txt</a><br><br>
            <a href='/'>Compare Another Deck</a>
        """);

        html.append("</body></html>");

        return html.toString();
    }
}
