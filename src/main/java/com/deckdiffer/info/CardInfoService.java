/**
 * CardInfoService.java
 *
 * Provides helper methods for determining card types, primary types, color identities,
 * sorting categories, extracting prices, computing totals, and parsing names from labels.
 */

package com.deckdiffer.info;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class CardInfoService {

    // Card Type Definitions
    private static final Set<String> CARD_TYPES = Set.of(
        "Artifact", "Creature", "Enchantment", "Instant",
        "Sorcery", "Land", "Planeswalker", "Battle", "Tribal"
    );

    // Priority ordering for type sorting
    private static final List<String> TYPE_PRIORITY = List.of(
        "Creature",
        "Land",
        "Artifact",
        "Enchantment",
        "Planeswalker",
        "Instant",
        "Sorcery",
        "Battle",
        "Tribal"
    );

    private CardInfoService() {
    }

    /**
     * @param card
     * @return List of types from a card typeline
     */
    public static List<String> extractTypesFromJson(JSONObject card) {
        List<String> types = new ArrayList<>();
        if (card == null) return types;

        String typeLine;

        if (card.has("card_faces")) {
            JSONArray faces = card.optJSONArray("card_faces");
            if (faces.length() > 0) {
                typeLine = faces.getJSONObject(0).optString("type_line", "");
            } 
            else {
                typeLine = card.optString("type_line", "");
            }
        }     
        else {
            typeLine = card.optString("type_line", "");
        }

        String[] parts = typeLine.split("—");
        String leftSide = parts[0].trim();

        // Left side of typeline before "-" contains all candidate types
        for (String word : leftSide.split("\\s+")) {
            if (CARD_TYPES.contains(word)) {
                types.add(word);
            }
        }
        return types;
    }

    /**
     * EX: {"White", "Black", "Blue"}
     * @param card
     * @return color identity list
     */
    public static List<String> extractColorsFromJson(JSONObject card) {
        List<String> res = new ArrayList<>();
        if (card == null) return res;

        JSONArray arr = card.optJSONArray("color_identity");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                res.add(arr.getString(i));
            }
        }

        Collections.sort(res);
        return res;
    }

    /**
     * By iterating through TYPE_PRIORITY in sequential order and returning when matched, we get the primary type
     * @param types
     * @return String representing primary type Ex: "Creature"
     */
    public static String fetchPrimaryType(List<String> types){
        for (String type: TYPE_PRIORITY){
            if (types.contains(type)){
                return type;
            }
        }
        return "Other";
    }

    /**
     * @param type
     * @return int representing sorting priority
     */
    public static int typeToPriority(String type){
        int idx = TYPE_PRIORITY.indexOf(type);
        if (idx >= 0){
            return idx;
        }
        else{
            return 9999;
        }
    }

    /**
     * @param colorIdentity
     * @return string representing color identity with abbrevs
     * ex: ["W", "U"] -> "WU"
     */
    public static String assignColorCategory(List<String> colorIdentity){

        if (colorIdentity == null || colorIdentity.isEmpty()){
            return "Colorless";
        }

        // Mono-colored
        if (colorIdentity.size() == 1){
            switch (colorIdentity.get(0)){
                case "W": return "White";
                case "U": return "Blue";
                case "B": return "Black";
                case "R": return "Red";
                case "G": return "Green";
        }   
        }

        // Else is multicolored
        return String.join("", colorIdentity); // ex: "WU", "UB", "WUB"
    }

    /**
     * @param colors as a string
     * @return int representing sorting priority
     */
    public static int colorSortKey(String colors) {
        switch (colors) {
            case "White": return 0;
            case "Blue": return 1;
            case "Black": return 2;
            case "Red": return 3;
            case "Green": return 4;
        }

        if (colors.equals("Colorless")) {
            return 9999;
        }

        int len = colors.length();
        int baseGroup = len * 10;

        return baseGroup + (colors.hashCode() & 0x7fffffff);
    }

    /**
     * @param card
     * @return double price in USD
     */
    public static double extractPriceFromJson(JSONObject card) {
        if (card == null) return 0.0;

        JSONObject prices = card.optJSONObject("prices");
        if (prices == null) return 0.0;

        String usd = prices.optString("usd", "0.0");

        try {
            return Double.parseDouble(usd);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * @param cardMap (deck)
     * @return total cost of deck in USD
     */
    public static double getTotalCost(Map<String, Integer> cardMap) {
        double total = 0.0;

        for (var entry : cardMap.entrySet()) {
            double price = CardDataService.fetchCardData(entry.getKey()).price;
            total += price * entry.getValue();
        }

        return total;
    }

    /**
     * 
     * @param label ex: "1 Island"
     * @return ex: "Island"
     */
    public static String extractCardNameFromLabel(String label){
        int idx = label.indexOf(' ');
        if (idx > 0){
            return label.substring(idx + 1).trim();
        }
        return label;
    }
}
