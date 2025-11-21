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

import com.deckdiffer.info.CardData;
import com.deckdiffer.info.CardInfoService;
import com.deckdiffer.info.CardDataService;
import com.deckdiffer.parsing.DeckParser;

public class CardGrouping {

    private CardGrouping() {
    }

    /**
     * @param cardName
     * @param count
     * @return Ex: "1 Counterspell"
     */
    public static String buildDisplayLabel(String cardName, int count) {
        return count + " " + cardName;
    }

    
    /**
     * @param cardMap
     * @return Map of primary type then by color category then cards
     * Ex: Creature -> [White -> ["1 Cloud, Midgar Mercenary"]]
     */
    public static Map<String, Map<String, List<String>>> groupTypeThenColor(Map<String, Integer> cardMap){
        Map<String, Map<String, List<String>>> res = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> entry : cardMap.entrySet()){
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
     * @param title: the html section title
     * @param cardMap
     * @param isAdd: whether card price should be included, typically only on added cards
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
                        // extract just card name
                        String cardName = CardInfoService.extractCardNameFromLabel(label);
                        
                        // lookup price in USD
                        Double price = CardDataService.fetchCardData(cardName).price;

                        html.append("<li>").append(label).append(" ($").append(price).append(")").append("</li>");
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

            // String header
            sb.append("# ").append(type.toUpperCase()).append(" (").append(typeCount).append(")\n");

            // Sort colors
            List<String> colorKeys = new ArrayList<>(colors.keySet());
            colorKeys.sort(Comparator.comparingInt(CardInfoService::colorSortKey));

            // Cards per color category
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
