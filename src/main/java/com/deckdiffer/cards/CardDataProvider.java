/**
 * CardDataProvider.java; Fetches Scryfall JSON and converts it into CardData objects.
 *
 * Responsibilities:
 * - Perform fuzzy-name Scryfall API lookups
 * - Cache JSON to minimize repeated API calls
 * - Build structured CardData objects to be used in CardDataInfo etc..
 * - Contains methods to extract relevant card fields
 */

package com.deckdiffer.cards;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

public class CardDataProvider {

    private static final Map<String, JSONObject> cardJsonCache = new HashMap<>();
    private static final Map<String, CardData> cardDataCache = new HashMap<>();

    // Card Type Definitions
    private static final Set<String> CARD_TYPES = Set.of(
        "Artifact", "Creature", "Enchantment", "Instant",
        "Sorcery", "Land", "Planeswalker", "Battle", "Tribal"
    );

    private CardDataProvider() {
    }

    /**
     * @param cardName: the name of the card to query
     * @return JSONObject representing Scryfall card data
     */
    static JSONObject fetchCardJson(String cardName) {
        String key = cardName.toLowerCase();

        if (cardJsonCache.containsKey(key)) {
            return cardJsonCache.get(key);
        }

        try {
            String query = "https://api.scryfall.com/cards/named?fuzzy=" +
                           cardName.trim().replace(" ", "+");

            HttpURLConnection conn = (HttpURLConnection) new URL(query).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();

            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            JSONObject json = new JSONObject(response.toString());

            cardJsonCache.put(key, json);
            return json;
        }
        catch (Exception e) {
            cardJsonCache.put(key, null);
            System.err.println("Failed to fetch card JSON for " + cardName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * @param cardName
     * @return CardData object extracted from fetchCardJson
     */
    public static CardData fetchCardData(String cardName) {
        String key = cardName.toLowerCase();

        if (cardDataCache.containsKey(key)) {
            return cardDataCache.get(key);
        }

        JSONObject json = fetchCardJson(cardName);

        CardData data;

        if (json == null) {
            data = new CardData(
                null,
                List.of(),
                "Other",
                List.of(),
                "Colorless",
                0.0,
                null,
                null,
                0.0,
                Map.of()
            );
        }
        else {
            data = buildCardDataFromJson(json);
        }

        cardDataCache.put(key, data);
        return data;
    }

    /**
     * Helper
     * @param json
     * @return CardData
     */
    private static CardData buildCardDataFromJson(JSONObject json) {

        List<String> types = extractTypesFromJson(json);
        String primaryType = CardClassifier.fetchPrimaryType(types);

        List<String> colors = extractColorsFromJson(json);
        String colorCategory = CardClassifier.assignColorCategory(colors);

        double price = extractPriceFromJson(json);

        String imageUrl = extractImageUrl(json);
        String scryfallUrl = json.optString("scryfall_uri", null);

        double cmc = extractCMC(json);

        Map<String, Integer> pipCounts = parsePips(json.optString("mana_cost", ""));

        return new CardData(json, types, primaryType, colors, colorCategory, price, imageUrl, scryfallUrl, cmc, pipCounts);
    }

     /**
     * @param card
     * @return List of types from a card typeline
     */
    public static List<String> extractTypesFromJson(JSONObject card) {
        List<String> types = new ArrayList<>();
        if (card == null) return types;

        String typeLine;

        // if card is MDFC
        if (card.has("card_faces")) {
            JSONArray faces = card.optJSONArray("card_faces");
            if (faces.length() > 0) {
                typeLine = faces.getJSONObject(0).optString("type_line", "");
            } 
            else {
                typeLine = card.optString("type_line", "");
            }
        }     
        else {
            typeLine = card.optString("type_line", "");
        }

        // Relevant grouping types are found left of the -, which all cards have
        String[] parts = typeLine.split("—");
        String leftSide = parts[0].trim();

        // Left side of typeline before "-" contains all candidate types
        for (String word : leftSide.split("\\s+")) {
            if (CARD_TYPES.contains(word)) {
                types.add(word);
            }
        }
        return types;
    }

    /**
     * EX: {"White", "Black", "Blue"}
     * @param card
     * @return color identity list
     */
    public static List<String> extractColorsFromJson(JSONObject card) {
        List<String> res = new ArrayList<>();
        if (card == null) return res;

        JSONArray arr = card.optJSONArray("color_identity");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                res.add(arr.getString(i));
            }
        }
        Collections.sort(res);
        return res;
    }

    /**
     * @param json
     * @return String containing image url of card
     */
    private static String extractImageUrl(JSONObject json) {
        if (json == null) return null;

        // Single faced cards
        JSONObject img = json.optJSONObject("image_uris");
        if (img != null) return img.optString("normal", null);

        // MDFC cards
        var faces = json.optJSONArray("card_faces");
        if (faces != null && faces.length() > 0) {
            JSONObject face = faces.getJSONObject(0);
            JSONObject faceImg = face.optJSONObject("image_uris");
            if (faceImg != null) return faceImg.optString("normal", null);
        }

        return null;
    }

    /**
     * @param JSONObject json representing card data from scryfall
     * @return double price in USD
     */
    public static double extractPriceFromJson(JSONObject json) {
        if (json == null) return 0.0;

        JSONObject prices =  json.optJSONObject("prices");
        if (prices == null) return 0.0;

        String usd = prices.optString("usd", "0.0");

        try {
            return Double.parseDouble(usd);
        }
        catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * @param JSONObject json representing card data from scryfall
     * @return double representing converted mana cost
     */
    public static double extractCMC(JSONObject json){
        if (json == null) return 0.0;
        return json.optDouble("cmc", 0.0);
    }

    /**
     * @param String manaCost
     * @return Map<String, Integer> mapping Mana color (WUBRG) to amount
     */

    public static Map<String, Integer> parsePips(String manaCost){
        Map<String, Integer> pipMap = new HashMap<>();
        String[] wubrg = {"W", "U", "B", "R", "G"};
        
        // init
        for (String color: wubrg){
            pipMap.put(color, 0);
        }

        pipMap.put("C", 0); // represents colorless

        if (manaCost == null || manaCost.isEmpty()){
            return pipMap;
        }

        for (String chunk : manaCost.split("\\}")){
            int open = chunk.indexOf('{');
            if (open < 0){
                continue;
            }

            String symbol = chunk.substring(open + 1).trim();

            // Numerate the generic mana first
            try{
                int genericCost = Integer.parseInt(symbol);
                pipMap.put("C", pipMap.get("C") + genericCost);
                continue;
            }
            catch (Exception e){}

            // Numerate WUBRG
            if (pipMap.containsKey(symbol)){
                pipMap.put(symbol, pipMap.get(symbol) + 1);
            }

            // Hybrid Mana, ex: "W/G" for pay with white or green
            // we still count it as if it were 1.
            // "W/G" contributes 1 white pip AND 1 green pip
            if (symbol.contains("/")){
                String[] manaParts = symbol.split("/");
                for (String part: manaParts){
                    if (pipMap.containsKey(part)){
                        pipMap.put(part, pipMap.get(part) + 1);
                    }
                }
            }
        }
        return pipMap;
    }
}
