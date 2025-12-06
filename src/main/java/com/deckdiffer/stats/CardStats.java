/**
 * CardStats.java: Computes statistics about card collections
 * 
 * Provides methods for calculating how many cards belong to each primary card type.
 * It uses the CardClassifier.java to determine a card's primary type.
 */
package com.deckdiffer.stats;
import java.util.*;

import com.deckdiffer.cards.CardDataProvider;

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

            String primaryType = CardDataProvider.fetchCardData(card).primaryType;

            typeCounts.put(primaryType, typeCounts.getOrDefault(primaryType,0) + count);
        }

        return typeCounts;
    }    
}
