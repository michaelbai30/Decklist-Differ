/**
 * DeckStatsService.java; Contains logic for computing deck-wide stats like average cmc, number of pips, etc...
 * Contains nested classes for ManaStats
 */
package com.deckdiffer.stats;

import java.util.*;

import com.deckdiffer.cards.CardData;
import com.deckdiffer.cards.CardDataProvider;

public class DeckStats {

    private DeckStats(){}    

    // Nested static class for all deck statistics
    public static class DeckStat{
        public final Map<String, Integer> fullDeck;
        public final double totalCost;
        public final double onlyDiffCost;
        public final ManaStats mana;

        public DeckStat(Map<String, Integer> fullDeck, double totalCost, double onlyDiffCost, ManaStats mana){
            this.fullDeck = fullDeck;
            this.totalCost = totalCost;
            this.onlyDiffCost = onlyDiffCost;
            this.mana = mana;
        }
    }

    // Nested static class for mana / cmc results
    public static class ManaStats{
        public final double totalCMC;
        public final int totalCards;
        public final Map<String, Integer> totalPips;

        public ManaStats(double totalCMC, int totalCards, Map<String, Integer> totalPips){
            this.totalCMC = totalCMC;
            this.totalCards = totalCards;
            this.totalPips = totalPips;
        }

        public double getAverageCMC(){
            if (totalCards == 0){
                return 0.0;
            }
            return totalCMC / totalCards;
        }
    }

    /**
     * @param only, common
     * @return fullDeck
     */
    // Since I had already implemented methods to extract only and common, just merge them ...
    public static Map<String, Integer> buildFullDeck(Map<String, Integer> only, Map<String, Integer> common){
        Map<String, Integer> fullDeck = new LinkedHashMap<>(only);
        for (var entry: common.entrySet()){
            fullDeck.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        return fullDeck;
    }

    // Compute value + mana + pips in one pass
    public static DeckStat computeDeckStats(Map<String, Integer> only, Map<String, Integer> common){

        Map<String, Integer> fullDeck = buildFullDeck(only, common);

        double totalCost = 0.0;
        double onlyDiffCost = 0.0;

        double totalCMC = 0.0;
        int totalNonLandCards = 0;

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

            CardData data = cache.computeIfAbsent(
                card.toLowerCase(),
                k -> CardDataProvider.fetchCardData(card)
            );

            if (data == null){
                continue;
            }

            // pricing
            totalCost += data.price * count;
            if (only.containsKey(card)){
                onlyDiffCost += data.price * count;
            }

            // Count only non-land permanents for average cmc cost calculations
            if (data != null && data.types.contains("Land")){
                continue;
            }
            
            // skip lands for average cmc calcs
            totalCMC += data.cmc * count;
            totalNonLandCards += count;

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

        ManaStats manaStats = new ManaStats(totalCMC, totalNonLandCards, totalPips);
        return new DeckStat(fullDeck, totalCost, onlyDiffCost, manaStats);
    }
}
