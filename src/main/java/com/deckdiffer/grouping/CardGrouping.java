/**
 * CardGrouping.java; Builds grouped representations of cards for display and export.
 *
 * Provides methods for organizing cards by their primary type and color category,
 * generating HTML sections for UI display, and producing detailed text output
 * for downloadable files.
 *
 * Uses CardInfoService + CardDataService to determine type and color data.
 *
 * Methods:
 * - Map<String, Map<String, List<String>>> groupTypeThenColor(Map<String, Integer>):
 *      Groups cards first by PRIMARY TYPE, then by COLOR CATEGORY.
 *
 * - String buildGroupedHtml(String, Map<String, Integer>):
 *      Builds fully formatted HTML representing grouped card data.
 *
 * - String buildNonDetailedTxtFile(Map<String, Integer>):
 *      Produces a non detailed text representation of ungrouped cards sorted alphabetically
 * 
 * - String buildDetailedTxtFile(Map<String, Integer>):
 *      Produces a detailed text representation of cards grouped by type and color.
 *
 * - String buildDisplayLabel(String, int):
 *      Converts a card name + count into a display string like "3 Lightning Bolt".
 */

package com.deckdiffer.grouping;

import java.util.*;

import com.deckdiffer.info.CardData;
import com.deckdiffer.info.CardInfoService;
import com.deckdiffer.info.CardDataService;
import com.deckdiffer.parsing.DeckParser;

public class CardGrouping {

    private CardGrouping() {}

    /**
     * Converts card name and count into a label for display.
     * Example: "1 Counterspell"
     * @param cardName
     * @param count
     * @return label
     */
    public static String buildDisplayLabel(String cardName, int count) {
        return count + " " + cardName;
    }

    /**
     * Groups cards into:
     * Primary Type → Color Category → List of display labels
     *
     * Ex:
     * Creature → 
     *    White → ["1 Cloud, Midgar Mercenary"]
     *    Blue  → ["1 Aether Adept"]
     *
     * @param cardMap
     * @return map of primary type then by color category(s) then cards
     */
    public static Map<String, Map<String, List<String>>> groupTypeThenColor(Map<String, Integer> cardMap) {

        Map<String, Map<String, List<String>>> res = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> entry : cardMap.entrySet()) {

            String card = entry.getKey();
            int count = entry.getValue();

            CardData data = CardDataService.fetchCardData(card);
            String primaryType = data.primaryType;
            String colorCategory = data.colorCategory;

            res.putIfAbsent(primaryType, new LinkedHashMap<>());
            res.get(primaryType).putIfAbsent(colorCategory, new ArrayList<>());

            String label = buildDisplayLabel(card, count);
            res.get(primaryType).get(colorCategory).add(label);
        }

        return res;
    }

    /**
     * Builds a grouped HTML block for the given cards.
     *
     * @param title: the html section title
     * @param cardMap
     * @return block of html representing grouped card data
     */
    public static String buildGroupedHtml(String title, Map<String, Integer> cardMap) {
        StringBuilder html = new StringBuilder();

        // Section header with total count
        html.append("<h2>")
            .append(title)
            .append(" (")
            .append(DeckParser.sumCounts(cardMap))
            .append(" Total)</h2>");

        // Group cards: primary type -> color -> labels
        Map<String, Map<String, List<String>>> grouped = groupTypeThenColor(cardMap);

        // Sort primary types by priority
        List<String> primaryTypes = new ArrayList<>(grouped.keySet());
        primaryTypes.sort(Comparator.comparingInt(CardInfoService::typeToPriority));

        for (String type : primaryTypes) {

            int typeCount = 0;
            Map<String, List<String>> colors = grouped.get(type);

            // Count total number of cards for this primary type
            for (List<String> labels : colors.values()) {
                for (String label : labels) {
                    int idx = label.indexOf(' ');
                    if (idx > 0) {
                        int count = Integer.parseInt(label.substring(0, idx));
                        typeCount += count;
                    }
                }
            }

            // Type header (Creature, Land, Artifact, ...)
            html.append("<h3>").append(type).append(" (")
                .append(typeCount).append(")</h3>");

            // Sort color categories using colorSortKey
            List<String> colorKeys = new ArrayList<>(colors.keySet());
            colorKeys.sort(Comparator.comparingInt(CardInfoService::colorSortKey));

            for (String color : colorKeys) {

                // Color header (White, Blue, WU, Colorless, etc.)
                html.append("<div class='card-color-header'>")
                    .append(color)
                    .append("</div>");

                html.append("<div class='card-grid'>");

                List<String> cardLabels = colors.get(color);
                Collections.sort(cardLabels, String::compareToIgnoreCase);

                for (String label : cardLabels) {

                    // ex label: "3 Lightning Bolt"
                    int idx = label.indexOf(' ');
                    int count = 1;
                    String cardName = label;

                    if (idx > 0) {
                        count = Integer.parseInt(label.substring(0, idx));
                        cardName = label.substring(idx + 1).trim();
                    }

                    CardData data = CardDataService.fetchCardData(cardName);

                    html.append("<a class='card-link' href='")
                        .append(data.scryfallUrl)
                        .append("' target='_blank'>")
                        .append("<div class='card-tile'>");
                    String imgUrl = data.imageUrl;

                    if (imgUrl != null) {
                        html.append("<img src='")
                            .append(imgUrl)
                            .append("' alt='")
                            .append(cardName.replace("'", "&#39;"))
                            .append("'>");
                    } else {
                        // If no image is found, use a textbox
                        html.append("<div class='card-fallback'>")
                            .append(label)
                            .append("</div>");
                    }

                    // Card counts for non-singleton cards
                    if (count > 1) {
                        html.append("<div class='card-count-badge'>x")
                            .append(count)
                            .append("</div>");
                    }

                    html.append("</div></a>"); // .card-tile
                }

                html.append("</div>"); // .card-grid
            }
        }
        return html.toString();
    }

     /**
     * Text file format WITHOUT grouping.
     * Sorted alphabetically.
     *
     * @param cardMap: deck
     * @return string txt representation of cards grouped by type and color
     */
    public static String buildNonDetailedTxtFile(Map<String, Integer> cardMap) {
        StringBuilder sb = new StringBuilder();

        List<String> cards = new ArrayList<>(cardMap.keySet());
        Collections.sort(cards, String::compareToIgnoreCase);

        for (String card : cards) {
            if (card.startsWith("#")) {
                continue;
            }
            int count = cardMap.getOrDefault(card, 0);
            sb.append(count).append(" ").append(card).append("\n");
        }

        return sb.toString();
    }

    /**
     * Text file output WITH grouping:
     * For example:
     *
     * # CREATURE (12)
     * # WHITE
     * 2 Sun Titan
     * 1 Wall of Omens
     *
     * # BLUE
     * 3 Aether Adept
     *
     * @param cardMap
     * @return grouped txt representation
     */
    public static String buildDetailedTxtFile(Map<String, Integer> cardMap) {

        StringBuilder sb = new StringBuilder();

        Map<String, Map<String, List<String>>> grouped = groupTypeThenColor(cardMap);

        // Sort primary types
        List<String> primaryTypes = new ArrayList<>(grouped.keySet());
        primaryTypes.sort(Comparator.comparingInt(CardInfoService::typeToPriority));

        for (String type : primaryTypes) {

            int typeCount = 0;
            Map<String, List<String>> colors = grouped.get(type);

            for (List<String> labels : colors.values()) {
                for (String label : labels) {
                    int idx = label.indexOf(' ');
                    if (idx > 0) {
                        int count = Integer.parseInt(label.substring(0, idx));
                        typeCount += count;
                    }
                }
            }
            sb.append("# ")
              .append(type.toUpperCase())
              .append(" (")
              .append(typeCount)
              .append(")\n");

            List<String> colorKeys = new ArrayList<>(colors.keySet());
            colorKeys.sort(Comparator.comparingInt(CardInfoService::colorSortKey));

            for (String color : colorKeys) {

                sb.append("# ").append(color.toUpperCase()).append("\n");

                List<String> cardLabels = colors.get(color);
                Collections.sort(cardLabels, String::compareToIgnoreCase);

                for (String label : cardLabels) {
                    sb.append(label).append("\n");
                }

                sb.append("\n");
            }

            sb.append("\n");
        }

        return sb.toString();
    }
}
