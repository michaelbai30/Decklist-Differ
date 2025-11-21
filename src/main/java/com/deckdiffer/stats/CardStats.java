/**
 * CardStats.java; Computes statistics about card collections
 * 
 * Provides methods for calculating how many cards belong to each primary card type.
 * It uses the CardInfoService to determine a card's primary type.
 * 
 * Method:
 * - Map<String, Integer> computeTypeCounts(Map<String, Integer>):
 *      Takes a map of card names (a deck) to their quantities and returns a new map
 *      that maps each PRIMARY type to the total number of cards of that type
 */

package com.deckdiffer.stats;
import java.util.*;

import com.deckdiffer.info.CardDataService;

public class CardStats {

    private CardStats() {
    }

    /**
     * @param cardMap
     * @return map of primary card types to quantity
     */
    public static Map<String, Integer> computeTypeCounts(Map<String, Integer> cardMap){

        // Ex: Creature -> 25, Land -> 35, etc...
        Map<String, Integer> typeCounts = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : cardMap.entrySet()){
            String card = entry.getKey();
            int count = entry.getValue();

            String primaryType = CardDataService.fetchCardData(card).primaryType;

            typeCounts.put(primaryType, typeCounts.getOrDefault(primaryType,0) + count);
        }

        return typeCounts;
    }    
}
