/**
 * CardGrouping.java; Builds grouped representations of cards for display and export.
 *
 * Provides methods for organizing cards by their primary type and color category,
 * generating HTML sections for UI display, and producing detailed text output
 * for downloadable files.
 *
 * Uses CardClassifier to determine type and color data.
 */

package com.deckdiffer.grouping;

import java.util.*;

import com.deckdiffer.cards.CardClassifier;
import com.deckdiffer.cards.CardData;

public class CardGrouping {
    private CardGrouping() {}

    /**
     * Converts card name and count into a label for display.
     * Example: "1 Counterspell"
     * @param cardName - name of a card as a string
     * @param count - the number of cards of that type in the deck
     * @return label as a String formatted like "{card count} {card name}"
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
     * @param cardCounts - map of card name to its count
     * @param cardDataMap - map of card name to is data
     * @return map of primary type then by color category(s) then cards
     */
    public static Map<String, Map<String, List<String>>> groupTypeThenColor(Map<String, Integer> cardCounts, Map<String, CardData> cardDataMap) {

        Map<String, Map<String, List<String>>> res = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> entry : cardCounts.entrySet()) {
            String card = entry.getKey();
            int count = entry.getValue();

            CardData data = cardDataMap.get(card);
            if (data == null){
                continue;
            }

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
     * grouped first by primary card type
     * and then sub-grouped by color identity.
     *
     * @param cardMap - Map of card names as a string and card count as a integer
     * @param cardDataMap - Map of card names as a string and card data as CardData object
     * @return block of html representing grouped card data
     */
    public static String buildGroupedHtml(Map<String, Integer> cardMap, Map<String, CardData> cardDataMap) {

        StringBuilder html = new StringBuilder();

        // Group cards: primary type -> color -> labels
        Map<String, Map<String, List<String>>> grouped = groupTypeThenColor(cardMap, cardDataMap);

        // Sort primary types by priority
        List<String> primaryTypes = new ArrayList<>(grouped.keySet());
        primaryTypes.sort(Comparator.comparingInt(CardClassifier::typeToPriority));

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
            colorKeys.sort(Comparator.comparingInt(CardClassifier::colorSortKey));

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

                    CardData data = cardDataMap.get(cardName);
                    if (data == null){
                        continue;
                    }

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
     * Each line: "{Card count} {Card Name}"
     * Sorted alphabetically.
     *
     * @param cardMap - Map of card names as a string and card count as a integer
     * @return String txt representation of cards grouped by type and color, sorted alphabetically within
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
     * Text file output WITH grouping by primary type, then color category
     * Includes count of cards within a primary type
     * 
     * For example:
     * # CREATURE (12)
     * # WHITE
     * 2 Sun Titan
     * 1 Wall of Omens
     *
     * # BLUE
     * 3 Aether Adept
     *
     * @param cardMap - Map of card names as a string and card count as a integer
     * @param cardDataMap - Map of card names as a string and card data as CardData object
     * @return grouped txt representation
     */
    public static String buildDetailedTxtFile(Map<String, Integer> cardMap, Map<String, CardData> cardDataMap) {

        StringBuilder sb = new StringBuilder();

        Map<String, Map<String, List<String>>> grouped = groupTypeThenColor(cardMap, cardDataMap);

        // Sort primary types
        List<String> primaryTypes = new ArrayList<>(grouped.keySet());
        primaryTypes.sort(Comparator.comparingInt(CardClassifier::typeToPriority));

        // Count number of cards in a primary type
        // For example, if there are 12 creatures in the deck, Creature (12)
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
            
            // Write primary type and count
            sb.append("# ")
              .append(type.toUpperCase())
              .append(" (")
              .append(typeCount)
              .append(")\n");

            // Sort by color key
            List<String> colorKeys = new ArrayList<>(colors.keySet());
            colorKeys.sort(Comparator.comparingInt(CardClassifier::colorSortKey));

            // Group card label by its color category
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
