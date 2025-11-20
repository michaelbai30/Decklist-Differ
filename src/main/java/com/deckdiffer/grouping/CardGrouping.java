/**
 * CardGrouping.java; Builds grouped representations of cards for display and export.
 *
 * Provides methods for organizing cards by their primary type and color category,
 * generating HTML sections for UI display, and producing detailed text output
 * for downloadable files. 
 * 
 * Uses CardInfoService to determine type and color data.
 *
 * Methods:
 * - Map<String, Map<String, List<String>>> groupTypeThenColor(Map<String, Integer>):
 *      Groups cards first by PRIMARY TYPE, then by COLOR CATEGORY.
 *
 * - String buildGroupedHtml(String, Map<String, Integer>, Boolean):
 *      Builds fully formatted HTML representing grouped cards,
 *
 * - String buildDetailedTxtFile(Map<String, Integer>):
 *      Produces a detailed text representation of cards grouped by type and color.
 *
 * - String buildDisplayLabel(String, int):
 *      Converts a card name + count into a display string like "3 Lightning Bolt".
 */

package com.deckdiffer.grouping;
import java.util.*;

import com.deckdiffer.info.CardInfoService;
import com.deckdiffer.info.PricingService;
import com.deckdiffer.parsing.DeckParser;

public class CardGrouping {

    private CardGrouping() {
    }

    /**
     * @param cardName
     * @param count
     * @returm simple string label
     */
    public static String buildDisplayLabel(String cardName, int count) {
        return count + " " + cardName;
    }

    /**
     * @param cardMap; Deck of cards
     * @return Map of primary type then by color category then cards
     * ex: Creature -> [White -> ["1 Cloud, Midgar Mercenary"]]
     */
    public static Map<String, Map<String, List<String>>> groupTypeThenColor(Map<String, Integer> cardMap){
        Map<String, Map<String, List<String>>> res = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> entry : cardMap.entrySet()){
            String card = entry.getKey(); // card name
            int count = entry.getValue(); // num copies

            List<String> type = CardInfoService.fetchCardTypes(card);
            String primaryType = CardInfoService.fetchPrimaryType(type);

            List<String> colors = CardInfoService.fetchColorIdentity(card);
            String colorCategory = CardInfoService.assignColorCategory(colors);

            // Outer:
            res.putIfAbsent(primaryType, new LinkedHashMap<>());

            // Inner:
            res.get(primaryType).putIfAbsent(colorCategory, new ArrayList<>());

            // Build card label
            String label = buildDisplayLabel(card, count);

            res.get(primaryType).get(colorCategory).add(label);
        }
        return res;
    }

    /**
     * @param title: the html section title
     * @param cardMap: card nams and quantities to display
     * @param isAdd: whether card price should be included
     * @return block of html representing grouped card data
     */
    public static String buildGroupedHtml(String title, Map<String, Integer> cardMap, Boolean isAdd) {
        StringBuilder html = new StringBuilder();

        html.append("<h2>").append(title).append(" (")
            .append(DeckParser.sumCounts(cardMap))
            .append(" Total)</h2>");

        Map<String, Map<String, List<String>>> grouped = groupTypeThenColor(cardMap);

        // Sort primary types by priority
        List<String> primaryTypes = new ArrayList<>(grouped.keySet());
        primaryTypes.sort(Comparator.comparingInt(CardInfoService::typeToPriority));

        for (String type : primaryTypes) {
            
            int typeCount = 0;
            Map<String, List<String>> colors = grouped.get(type);

            // Count the number of cards per type (creature, artifact, etc...)
            for (List<String> cardLabels: colors.values()){
                for (String label: cardLabels){
                    int indexOfSpace = label.indexOf(' ');
                    if (indexOfSpace > 0){
                        // extract the "4" out of "4 Lightning Bolt" for example
                        int count = Integer.parseInt(label.substring(0, indexOfSpace));
                        typeCount += count;
                    }
                }
            }

            html.append("<h3>").append(type).append(" (").append(typeCount).append(")").append("</h3><ul>");

            // Sort colors using colorSortKey
            List<String> colorKeys = new ArrayList<>(colors.keySet());
            colorKeys.sort(Comparator.comparingInt(CardInfoService::colorSortKey));

            for (String color : colorKeys) {
                html.append("<li><b>").append(color).append("</b><ul>");

                List<String> cardLabels = colors.get(color);
                Collections.sort(cardLabels, String::compareToIgnoreCase);

                for (String label : cardLabels) {
                    if (isAdd){
                        String cardName = PricingService.extractCardNameFromLabel(label);
                        Double cardPrice = PricingService.fetchCardPrice(cardName);
                        html.append("<li>").append(label).append(" ($").append(Double.toString(cardPrice) + ")").append("</li>");
                    }
                    else{
                        html.append("<li>").append(label).append("</li>");
                    }
                }
                html.append("</ul></li>");
            }

            html.append("</ul>");
        }

        return html.toString();
    }

    /**
     * @param cardMap: deck
     * @return string txt representation of cards grouped by type and color
     */
    public static String buildDetailedTxtFile(Map<String, Integer> cardMap){
        StringBuilder sb = new StringBuilder();
        Map<String, Map<String, List<String>>> grouped = groupTypeThenColor(cardMap);
        
        // Sort primary types by priority
        List<String> primaryTypes = new ArrayList<>(grouped.keySet());
        primaryTypes.sort(Comparator.comparingInt(CardInfoService::typeToPriority));
               
        for (String type : primaryTypes){
            int typeCount = 0;
            Map<String, List<String>> colors = grouped.get(type);
            // count total cards for this primary type
            for (List<String> cardLabels : colors.values()) {
                for (String label : cardLabels) {
                    int idx = label.indexOf(' ');
                    if (idx > 0) {
                        int count = Integer.parseInt(label.substring(0, idx));
                        typeCount += count;
                    }
                }
            }
            // Build String Header
            sb.append("# ").append(type.toUpperCase()).append(" (").append(typeCount).append(")\n");

            // Sort Colors
            List<String> colorKeys = new ArrayList<>(colors.keySet());
            colorKeys.sort(Comparator.comparingInt(CardInfoService::colorSortKey));

            for (String color : colorKeys){
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
