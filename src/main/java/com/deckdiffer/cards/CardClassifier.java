/**
 * CardClassifier.java
 *
 * Provides helper methods for determining card types, primary types, color identities,
 *  and sorting categories
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
        colorIdentity.sort(Comparator.comparingInt(c -> "WUBRG".indexOf(c))); // sort based on WUBRG
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
}
