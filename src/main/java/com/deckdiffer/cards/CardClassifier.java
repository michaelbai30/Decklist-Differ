/**
 * CardClassifier.java
 *
 * Provides helper methods for determining card types, primary types, color identities,
 * and sorting categories
 */

package com.deckdiffer.cards;
import java.util.*;

public class CardClassifier {

    private CardClassifier() {}

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
 
    /**
     * By iterating through TYPE_PRIORITY in sequential order and returning when matched, we get the primary type
     * Ex: An "Artifact Creature" would be foremost categorized as a "Creature"
     * @param types - The types of a card as a List<String>
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
     * Maps a card type string to a numeric priority for sorting
     * @param type - The primary type of a card
     * @return int representing sorting priority
     */
    public static int typeToPriority(String type){
        int idx = TYPE_PRIORITY.indexOf(type);
        if (idx >= 0){
            return idx;
        }
        else{
            return 9999; // Lowest priority
        }
    }

    /**
     * Takes in a list of color identities from Scryfall and converts into a representative string
     * 
     * W = White
     * U = Blue
     * B = Black
     * R = Red
     * G = Green
     * 
     * WU = White and Blue etc...
     * 
     * @param colorIdentity - A list of strings representing the card's colors
     * @return string representing color identity with abbrevs
     * ex: {"W", "U"} -> "WU"
     * {"W"} -> "White"
     */
    public static String assignColorCategoryAsString(List<String> colorIdentity){
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
        colorIdentity.sort(Comparator.comparingInt(c -> "WUBRG".indexOf(c))); // sort based on WUBRG
        return String.join("", colorIdentity); // ex: "WU", "UB", "WUB"
    }

    /**
     * Maps a color identity string to its sorting priority in WUBRG canonical order
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
}
