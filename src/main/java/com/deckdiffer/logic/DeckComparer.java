/**
 * DeckComparer.java; Performs comparison logic between two decklists.
 *
 * Determines
 * - which cards were added
 * - which cards are to be cut
 * - which cards remain in common
 * - type count differences between the two decks
 */

package com.deckdiffer.logic;

import java.util.*;
import com.deckdiffer.stats.CardStats;

public class DeckComparer {

    private DeckComparer() {}

    /**
     * @param: Map<String, Integer> representing base card deck
     * @param: Map<String, Integer> representing upgraded card deck
     * @return: map of cards that appear more times in upgraded deck.
     */
    public static Map<String, Integer> computeCardsToAdd(Map<String, Integer> baseMap, Map<String, Integer> upgradedMap) {

        Map<String, Integer> result = new LinkedHashMap<>();

        Set<String> allCards = new HashSet<>();
        allCards.addAll(baseMap.keySet());
        allCards.addAll(upgradedMap.keySet());

        for (String card : allCards) {
            int baseCount = baseMap.getOrDefault(card, 0);
            int newCount = upgradedMap.getOrDefault(card, 0);

            if (newCount > baseCount) {
                result.put(card, newCount - baseCount);
            }
        }

        return result;
    }

    /**
     * @param: Map<String, Integer> representing base card deck
     * @param: Map<String, Integer> representing upgraded card deck
     * @return: map of cards that appear more times in the base deck
     */ 
    public static Map<String, Integer> computeCardsToRemove(Map<String, Integer> baseMap, Map<String, Integer> upgradedMap) {

        Map<String, Integer> result = new LinkedHashMap<>();

        Set<String> allCards = new HashSet<>();
        allCards.addAll(baseMap.keySet());
        allCards.addAll(upgradedMap.keySet());

        for (String card : allCards) {
            int baseCount = baseMap.getOrDefault(card, 0);
            int newCount = upgradedMap.getOrDefault(card, 0);

            if (baseCount > newCount) {
                result.put(card, baseCount - newCount);
            }
        }

        return result;
    }

    /**
     * @param: Map<String, Integer> representing base card deck
     * @param: Map<String, Integer> representing upgraded card deck 
     * Returns cards that appear in both decks (minimum count).
     */
    public static Map<String, Integer> computeCardsInCommon(Map<String, Integer> baseMap, Map<String, Integer> upgradedMap) {

        Map<String, Integer> result = new LinkedHashMap<>();

        Set<String> allCards = new HashSet<>();
        allCards.addAll(baseMap.keySet());
        allCards.addAll(upgradedMap.keySet());

        for (String card : allCards) {
            int baseCount = baseMap.getOrDefault(card, 0);
            int newCount = upgradedMap.getOrDefault(card, 0);

            int common = Math.min(baseCount, newCount);
            if (common > 0) {
                result.put(card, common);
            }
        }

        return result;
    }

    /**
     * @param: Map<String, Integer> representing base card deck
     * @param: Map<String, Integer> representing upgraded card deck 
     * @return: Returns a map describing how type counts changed between base and upgraded.
     * Ex: Creature -> {20, 25} // The amount of creatures in the deck increased from 20 cards to 25 cards
     */
    public static Map<String, int[]> computeTypeChanges(Map<String, Integer> baseMap, Map<String, Integer> upgradedMap) {

        Map<String, Integer> oldTypes = CardStats.computeTypeCounts(baseMap);
        Map<String, Integer> newTypes = CardStats.computeTypeCounts(upgradedMap);

        Map<String, int[]> result = new LinkedHashMap<>();

        Set<String> allTypes = new TreeSet<>();
        allTypes.addAll(oldTypes.keySet());
        allTypes.addAll(newTypes.keySet());

        for (String type : allTypes) {
            int oldCount = oldTypes.getOrDefault(type, 0);
            int newCount = newTypes.getOrDefault(type, 0);

            result.put(type, new int[]{oldCount, newCount});
        }

        return result;
    }
}
