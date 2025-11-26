/**
 * DeckParser.java: Responsible for parsing decklist text into structured data
 * 
 * Provides utility methods or converting raw decklist text into card name -> quantity map,
 * counting total card counts, converting maps back into lines, and writing into txt file.
 * 
 * Methods: 
 * - Map<String, Integer> parseDeck(String):
 *      Takes mutli-line deck text and returns Map<String, Integer>, card name -> quant
 * 
 * - int sumCounts(Map<String, Integer>):
 *      Returns total number of cards in a "deck"
 * 
 * - List<String> mapToLines(Map<String, Integer)
 *      Convert a card map into list of formatted strings like "3 Lightning Bolt"
 * 
 * - void writeToFile(String, List<String>):
 *      Writes a list of text lines to a file on disk
 * 
 * - Map<String, Integer> normalizeNames(Map<String, Integer> map):
 *   Strips off everything after "//" for MDFC cards
 */

package com.deckdiffer.parsing;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class DeckParser {

    private DeckParser() {
    }
    
    /**
     * @param deckText: Multi-line deck text
     * @return Map<String, Integer> card name -> quant
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
     * @param cardMap
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
     * @param cardMap
     * @return List<String> of card name -> count to representative line
     * Ex: {Island} -> {3} to "3 Island"
     */
    public static List<String> mapToLines(Map<String, Integer> cardMap) {
        List<String> lines = new ArrayList<>();
        for (var entry : cardMap.entrySet()) {
            lines.add(entry.getValue() + " " + entry.getKey());
        }
        return lines;
    }

    /**
     * 
     * @param fileName
     * @param lines
     */
    public static void writeToFile(String fileName, List<String> lines) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        } 
        catch (IOException e) {
            System.err.println("Error writing " + fileName + ": " + e.getMessage());
        }
    }

    /**
    * Normalizes card names to a canonical form for comparison.
    * Strips off everything after "//" for MDFC / face cards
    * Makes for easier comparisons
    */
    public static Map<String, Integer> normalizeNames(Map<String, Integer> map) {
        Map<String, Integer> res = new LinkedHashMap<>();

        for (var entry : map.entrySet()) {
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
