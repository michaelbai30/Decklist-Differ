/**
 * DeckComparer.java: Performs comparison logic between two decklists.
 *
 * Determines:
 * - cards unique to Deck 1
 * - cards unique to Deck 2
 * - cards shared in common between both decks
 * - type count differences between the two decks
 */

package com.deckdiffer.logic;

import java.util.*;
import com.deckdiffer.stats.CardStats;

public class DeckComparer {

    private DeckComparer() {}

    private static Map<String, Integer> computeDifference(Map<String, Integer> a, Map<String, Integer> b) {
        Map<String, Integer> res = new LinkedHashMap<>();

        for (String card : a.keySet()) {
            int diff = a.getOrDefault(card, 0) - b.getOrDefault(card, 0);
            if (diff > 0) {
                res.put(card, diff);
            }
        }
        return res;
    }
    /**@param deck1 Map<String, Integer>
     * @param deck2 Map<String, Integer>
     * @return cards that appear exclusively in Deck 1 with their counts
     */
    public static Map<String, Integer> computeDeck1Only(Map<String, Integer> deck1, Map<String, Integer> deck2) {
        return computeDifference(deck1, deck2);
    }

    /**@param deck1 Map<String, Integer>
     * @param deck2 Map<String, Integer>
     * @return cards that appear exclusively in Deck 2 with their counts
     */
    public static Map<String, Integer> computeDeck2Only(Map<String, Integer> deck1, Map<String, Integer> deck2) {
        return computeDifference(deck2, deck1);
    }

    /**
     * @param deck1 Map<String, Integer> representing Deck 1
     * @param deck2 Map<String, Integer> representing Deck 2
     * @return cards that appear in BOTH decks (minimum count)
     */
    public static Map<String, Integer> computeCardsInCommon(Map<String, Integer> deck1, Map<String, Integer> deck2) {

        Map<String, Integer> result = new LinkedHashMap<>();

        Set<String> allCards = new HashSet<>();
        allCards.addAll(deck1.keySet());
        allCards.addAll(deck2.keySet());

        for (String card : allCards) {
            int count1 = deck1.getOrDefault(card, 0);
            int count2 = deck2.getOrDefault(card, 0);

            int common = Math.min(count1, count2);
            if (common > 0) {
                result.put(card, common);
            }
        }
        return result;
    }

    /**
     * @param deck1 Map<String, Integer>
     * @param deck2 Map<String, Integer>
     * @return Map <String, int[]>: Returns a map describing how type counts changed between base and upgraded.
     * where
     * value[0] = count in Deck 1
     * value[1] = count in Deck 2
     * Ex: Creature -> {20, 25} // The amount of creatures in the deck increased from 20 cards to 25 cards
     */
    public static Map<String, int[]> computeTypeDifferences(Map<String, Integer> deck1, Map<String, Integer> deck2) {

        Map<String, Integer> d1Types = CardStats.computeTypeCounts(deck1);
        Map<String, Integer> d2Types = CardStats.computeTypeCounts(deck2);

        Map<String, int[]> result = new LinkedHashMap<>();

        Set<String> allTypes = new TreeSet<>();
        allTypes.addAll(d1Types.keySet());
        allTypes.addAll(d2Types.keySet());

        for (String type : allTypes) {
            int count1 = d1Types.getOrDefault(type, 0);
            int count2 = d2Types.getOrDefault(type, 0);

            result.put(type, new int[]{count1, count2});
        }

        return result;
    }
}
