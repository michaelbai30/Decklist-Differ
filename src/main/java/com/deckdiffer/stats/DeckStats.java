/**
 * DeckStatsService.java; Contains logic for computing deck-wide stats like average cmc, number of pips, etc...
 * Utilizes nested static classes to return complex, bundled results for those statistics
 */
package com.deckdiffer.stats;

import java.util.*;

import com.deckdiffer.cards.CardData;
import com.deckdiffer.cards.CardDataProvider;

public class DeckStats {

    private DeckStats(){}    

    // Nested static class that bundles the complete set of calculated statistics
    public static class DeckStat{
        // Comlete deck list
        public final Map<String, Integer> fullDeck;

        // Total cost of all cards in fullDeck in USD (TCGPlayer)
        public final double totalCost;

        // Total cost of only cards that are unique to the difference set in USD
        public final double onlyDiffCost;

        // Nested object containing all calculated mana/CMC stats
        public final ManaStats manaStats;

        public DeckStat(Map<String, Integer> fullDeck, double totalCost, double onlyDiffCost, ManaStats manaStats){
            this.fullDeck = fullDeck;
            this.totalCost = totalCost;
            this.onlyDiffCost = onlyDiffCost;
            this.manaStats = manaStats;
        }
    }

    // Nested static class holding all calculated mana / cmc results
    public static class ManaStats{

        // The summed converted mana cost (cmc) of all non-land cards in deck
        public final double totalCMC;

        // Total count of non-land cards considered in average cmc calculations
        public final int totalCards;

        // Map containing the total count of each color pip across all cards in deck
        public final Map<String, Integer> totalPips;

        /**
         * Calculates the average converted mana cost (CMC) of the deck.
         * The calculation only includes non-land cards.
         * @return The average CMC as a double, or 0.0 if the deck contains zero non-land cards.
         */
        public double getAverageCMC(){
            if (totalCards == 0){
                return 0.0;
            }
            return totalCMC / totalCards;
        }

        // Constructor for ManaStats object
        public ManaStats(double totalCMC, int totalCards, Map<String, Integer> totalPips){
            this.totalCMC = totalCMC;
            this.totalCards = totalCards;
            this.totalPips = totalPips;
        }
    }

    /**
     * Computes all necessary deck statistics (cost, CMC, pips) in a single pass
     * @param only - Map of cards and their counts unique to the difference set.
     * @param common - Map of cards and their counts common to both original decks.
     * @return A DeckStat object containing all calculated results.
     */
    public static DeckStat computeDeckStats(Map<String, Integer> only, Map<String, Integer> common){

        Map<String, Integer> fullDeck = buildFullDeck(only, common);

        // Monetary cost tracking
        double totalCost = 0.0;
        double onlyDiffCost = 0.0;

        // CMC related calculations tracking
        double totalCMC = 0.0;
        int totalNonLandCards = 0;

        // For mana pip tracking (WUBRGC). Initialize all to zero.
         Map<String, Integer> totalPips= new HashMap<>(Map.of(
            "C", 0, "W", 0, "U", 0, "B", 0, "R", 0, "G", 0
        ));

        Map<String, CardData> cache = new HashMap<>();

        for (var entry: fullDeck.entrySet()){
            String card = entry.getKey();
            int count = entry.getValue();
            if (count <= 0){
                continue;
            }

            // Retrieve card data
            CardData data = cache.computeIfAbsent(
                card.toLowerCase(),
                k -> CardDataProvider.fetchCardData(card)
            );

            // Skip if card data not found
            if (data == null){
                continue;
            }

            // Cost Calculations

            // Add to total deck value
            totalCost += data.price * count;
            
            // If card is present in difference set (only), add its cost to difference value.
            if (only.containsKey(card)){
                int onlyCount = only.getOrDefault(card, 0);
                onlyDiffCost += data.price * onlyCount;
            }


            // CMC Calculations

            // Count only non-land permanents for average cmc cost calculations
            if (data != null && data.types.contains("Land")){
                continue;
            }
            
            // Skip lands for average cmc calcs
            totalCMC += data.cmc * count;
            totalNonLandCards += count;

            // Mana Pip Calculations
            if (data.pipCounts != null){
                for (var pipEntry: data.pipCounts.entrySet()){
                    String color = pipEntry.getKey();
                    int pipCount = pipEntry.getValue();

                    if (!totalPips.containsKey(color)){
                        continue;
                    }

                    int current = totalPips.get(color);
                    totalPips.put(color, current + pipCount * count);
                }
            }
        }

        // Build final results and return
        ManaStats manaStats = new ManaStats(totalCMC, totalNonLandCards, totalPips);
        return new DeckStat(fullDeck, totalCost, onlyDiffCost, manaStats);
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

    /**
     * Merges two partial deck lists (cards unique to one deck and cards common to both) 
     * into a single, complete deck list with aggregated counts.
     * 
     * @param only - Map of cards and their counts unique to the difference set.
     * @param common - Map of cards and their counts common to both original decks.
     * @return fullDeck - New Map<String, Integer> containing all cards and their merged quantity.
     */
    private static Map<String, Integer> buildFullDeck(Map<String, Integer> only, Map<String, Integer> common){
        Map<String, Integer> fullDeck = new LinkedHashMap<>(only);
        // Since I had already implemented methods to extract only and common, just merge them ...
        for (var entry: common.entrySet()){
            fullDeck.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        return fullDeck;
    }
}
