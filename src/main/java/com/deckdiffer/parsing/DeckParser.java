/**
 * DeckParser.java: Responsible for parsing decklist text into structured data
 * 
 * Provides utility methods for converting raw decklist text into card name -> quantity map,
 * counting total card counts, converting maps back into lines, and writing into txt file.
 */

package com.deckdiffer.parsing;
import java.util.*;

public class DeckParser {

    private DeckParser() {
    }
    
    /**
     * Returns map representing a deck (card name -> card count) from multi-line string of txt
     * 
     * Ex:
     * 1 Island
     * 1 Mountain
     * 1 Plains
     * 
     * becomes {{"Island", 1}, {"Mountain", 1}, {"Plains"}}
     * 
     * @param deckText: Multi-line deck text
     * @return Map<String, Integer> mapping card name -> card count
     */
    public static Map<String, Integer> parseDeck(String deckText) {
        Map<String, Integer> map = new LinkedHashMap<>();

        // deckText is a massive string representing decklist with \n separations
        if (deckText == null){
            return map;
        }
        // split by newline or carraige return + newline
        for (String line : deckText.split("\\r?\\n")) {
            line = line.trim();
            if (line.isEmpty()){
                continue;
            }

            if (line.startsWith("#")){
                continue;
            }

            // split on 1+ whitespace only once
            String[] parts = line.split("\\s+", 2);

            int count = 1;
            String name;

            try {
                count = Integer.parseInt(parts[0]);
                if (parts.length > 1){
                    name = parts[1].trim();
                }
                else{
                    name = "";
                }
            }
            catch (NumberFormatException e) {
                name = line; // line has no leading number
            }

            if (!name.isEmpty()) {
                map.put(name, map.getOrDefault(name, 0) + count);
            }
        }
        return map;
    }

    /**
     * @param cardMap - Map<String, Integer> epresenting a deck of cards (card name -> card count)
     * @return total number of cards in a card map
     */
    public static int sumCounts(Map<String, Integer> cardMap) {
        int sum = 0;
        for (int val : cardMap.values()){
            sum += val;
        }
        return sum;
    }

    /**
    * Normalizes card names to a canonical form for comparison.
    * Strips off everything after "//" for MDFC / face cards
    * Makes for easier comparisons
    * 
    * @param cardMap - Map<String, Integer> epresenting a deck of cards (card name -> card count) 
    * 
    */
    public static Map<String, Integer> normalizeNames(Map<String, Integer> cardMap) {
        Map<String, Integer> res = new LinkedHashMap<>();

        for (var entry : cardMap.entrySet()) {
            String name = entry.getKey();
            int count = entry.getValue();

            // Remove alternate faces, e.g.
            // "A // B" → "A"
            int idx = name.indexOf("//");
            if (idx != -1) {
                name = name.substring(0, idx).trim();
            }

            res.put(name, count);
        }
        return res;
    }
}
