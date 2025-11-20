/**
 * CardInfoService.java; Get info from Scryfall API and analyze data
 *
 * Provides helper methods for determining card types, primary types, color identities,
 * and sorting categories.
 * 
 * Card info is derived from the JSON returned by PricingService.fetchCardJson().
 *
 * Methods:
 * - List<String> fetchCardTypes(String):
 *      Extracts all recognized types from a card's type line.
 *
 * - List<String> fetchColorIdentity(String):
 *      Retrieves a card's full color identity as a sorted list.
 *
 * - String fetchPrimaryType(List<String>):
 *      Selects a card's PRIMARY type based on a fixed type-priority list.
 *
 * - int typeToPriority(String):
 *      Converts a type into a sortable priority index (integer).
 *
 * - String assignColorCategory(List<String>):
 *      Converts list color identity into a display category
 *      (e.g., "Blue", "BG", "WUB", "Colorless").
 *
 * - int colorSortKey(String):
 *      Computes a numeric sorting key for ordering colors.
 *
 * - List<String> sortCardsByTypeThenColor(Map<String, Integer>):
 *      Sorts card names by primary type, then color category, then name.
 */

package com.deckdiffer.info;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class CardInfoService {

    // Set of possible card types
    private static final Set<String> CARD_TYPES = Set.of(
        "Artifact", "Creature", "Enchantment", "Instant",
        "Sorcery", "Land", "Planeswalker", "Battle", "Tribal"
    );

    // Liat of card types in canon order of dominance
    // If a card is an Artifact Creature, it is dominantly a creature
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
     * @param cardName
     * @return List<String> of all recognized types from a card's type line
     */
    public static List<String> fetchCardTypes(String cardName){
        JSONObject card = PricingService.fetchCardJson(cardName);
        List<String> types = new ArrayList<>();
        String typeLine;
        if (card == null){
            return new ArrayList<>();
        }
        if (card.has("card_faces")){
            JSONArray faces = card.optJSONArray("card_faces");
            if (faces.length() > 0){
                typeLine = faces.getJSONObject(0).optString("type_line", "");
            }
            else{
                typeLine = card.optString("type_line", "");
            }
        }
        else{
            typeLine = card.optString("type_line", "");
        }
        String[] sections = typeLine.split("—");

        // contains all keywords that a card may contain
        String leftSide = sections[0].trim();
        for (String word: leftSide.split("\\s+")){
            if (CARD_TYPES.contains(word)){
                types.add(word);
            }
        }
        return types;
    }

    /**
     * @param cardName
     * @return List<String> of colors
     */
    public static List<String> fetchColorIdentity(String cardName){
        JSONObject card = PricingService.fetchCardJson(cardName);
        if (card == null){
            return new ArrayList<>();
        }
        JSONArray colorsArray = card.optJSONArray("color_identity");
        List<String> colors = new ArrayList<>();
        
        if (colorsArray != null){
            for (int i = 0; i < colorsArray.length(); i++){
                colors.add(colorsArray.getString(i));
            }
        }
        Collections.sort(colors);
        return colors;
    }

    /**
     * @param types 
     * @return The primary type among list of types
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
     * @param type: primary type
     * @return int representing priority
     */
    public static int typeToPriority(String type){
        int idx = TYPE_PRIORITY.indexOf(type);
        if (idx >= 0){
            return idx;
        }
        else{
            return 9999; // if the card's type is not found for some reason, give it lowest possible priority
        }
    }

    /**
     * @param colorIdentity
     * @return string representing color identity with abbrevs
     * ex: ["White", "Blue"] -> "WU"
     */
    public static String assignColorCategory(List<String> colorIdentity){

        // Colorless
        if (colorIdentity == null || colorIdentity.isEmpty()){
            return "Colorless";
        }

        // Monocolored
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
        return String.join("", colorIdentity); // ex: "RW", "BG", "WUB"
    }
 
    /**
     * @param colors as a string
     * @return int representing sorting priority
     */
    public static int colorSortKey(String colors) {
        // Single color switching
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

        // Multicolor
        // Priority based on size first then alphabet
        // size 2 colors → group 10
        // size 3 colors → group 20
        // size 4 colors → group 30
        // size 5 colors → group 40

        int len = colors.length();  // number of colors
        int baseGroup = len * 10;

        // add alphabetical weight
        return baseGroup + (colors.hashCode() & 0x7fffffff);
    }

    /**
     * @param cardMap
     * @return sorted card by type then color
     */
    public static List<String> sortCardsByTypeThenColor(Map<String, Integer> cardMap){
        List<String> cards = new ArrayList<>(cardMap.keySet());
        cards.sort((card1, card2) -> {

            // Determine by Card Primary Type First
            List<String> card1Types = fetchCardTypes(card1);
            List<String> card2Types = fetchCardTypes(card2);

            String card1PrimaryType = fetchPrimaryType(card1Types);
            String card2PrimaryType = fetchPrimaryType(card2Types);

            int typeComparison = Integer.compare(typeToPriority(card1PrimaryType), typeToPriority(card2PrimaryType));
            if (typeComparison != 0){
                return typeComparison;
            }

            // Then look at color categories
            List<String> card1Colors = fetchColorIdentity(card1);
            List<String> card2Colors = fetchColorIdentity(card2);

            String card1ColorCategory = assignColorCategory(card1Colors);
            String card2ColorCategory = assignColorCategory(card2Colors);

            int colorComparison = Integer.compare(colorSortKey(card1ColorCategory), colorSortKey(card2ColorCategory));

            if (colorComparison != 0){
                return colorComparison;
            }

            return (card1.compareToIgnoreCase(card2));
        });

        return cards;
    }
}
