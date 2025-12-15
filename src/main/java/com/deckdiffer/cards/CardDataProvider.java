/**
 * CardDataProvider.java; Fetches Scryfall JSON and converts it into CardData objects.
 *
 * Card Name -> Scryfall -> JSON -> CardData
 * 
 * Responsibilities:
 * - Perform fuzzy-name Scryfall API lookups
 * - Cache JSON to minimize repeated API calls
 * - Build structured CardData objects
 * - Contains methods to extract relevant card fields
 */

package com.deckdiffer.cards;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

public class CardDataProvider {
    private static final Map<String, JSONObject> cardJsonCache = new ConcurrentHashMap<>();
    private static final Map<String, CardData> cardDataCache = new ConcurrentHashMap<>();

    // Card Type Definitions
    private static final Set<String> CARD_TYPES = Set.of(
        "Artifact", "Creature", "Enchantment", "Instant",
        "Sorcery", "Land", "Planeswalker", "Battle", "Tribal"
    );

    private CardDataProvider() {
    }

    /**
     * Populates the cache using the fast batch API for all required cards.
     * DeckListDifferServer calls this method just once per comparison.
     * 
     * @param cardNames - A set of strings representing names of cards
     */
    public static void populateCacheInBatch(Set<String> cardNames) {
        // Don't include names already in the cache
        Set<String> namesToFetch = new HashSet<>();
        for (String name : cardNames) {
            if (!cardDataCache.containsKey(name.toLowerCase())) {
                namesToFetch.add(name);
            }
        }

        if (namesToFetch.isEmpty()) {
            return;
        }

        // Execute fast batch fetch
        Map<String, JSONObject> newFetchedJson = ScryfallBatchFetcher.fetchBatchJson(namesToFetch);

        // Add results to our static caches
        for (Map.Entry<String, JSONObject> entry : newFetchedJson.entrySet()) {
            String cardName = entry.getKey();
            JSONObject json = entry.getValue();
            String key = cardName.toLowerCase();

            cardJsonCache.put(key, json);
            CardData data = buildCardDataFromJson(json);
            cardDataCache.put(key, data);
        }
    }

    /**
     * Fetches card data from either the cardDataCache or calls fetchCardJson to call API
     * 
     * @param cardName
     * @return CardData object extracted from cache or fetchCardJson
     */
    public static CardData fetchCardData(String cardName) {
        String key = cardName.toLowerCase();

        if (cardDataCache.containsKey(key)) {
            return cardDataCache.get(key);
        }

        JSONObject json = cardJsonCache.get(key);

        // if no JSON, fetch it (slow fallback)
        if (json == null) {
             json = fetchCardJson(cardName);
        }

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

    // ---------------
    // Helper Methods
    // ---------------

    /**
     * Performs fuzzy-name Scryfall API request for a given card (cardName) and returns JSON from endpoint
     * 
     * @param cardName - the name of the card to query as a string
     * @return JSONObject representing Scryfall card data
     */
    private static JSONObject fetchCardJson(String cardName) {
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
     * Builds and returns a CardData object from a card's json info
     * 
     * @param json - JSONObject representing a card's data, returned from Scryfall
     * @return CardData - CardData object populated and returned (model from CardData.java)
     */
    private static CardData buildCardDataFromJson(JSONObject json) {

        List<String> types = extractTypesFromJson(json);
        String primaryType = CardClassifier.fetchPrimaryType(types);

        List<String> colors = extractColorsFromJson(json);
        String colorCategory = CardClassifier.assignColorCategoryAsString(colors);

        double price = extractPriceFromJson(json);

        String imageUrl = extractImageUrl(json);
        String scryfallUrl = json.optString("scryfall_uri", null);

        double cmc = extractCMC(json);

        Map<String, Integer> pipCounts = parsePips(json.optString("mana_cost", ""));

        return new CardData(json, types, primaryType, colors, colorCategory, price, imageUrl, scryfallUrl, cmc, pipCounts);
    }

    /**
     * Extracts the cards types from json as a List<String>
     * 
     * @param json - JSONObject representing a card's data, returned from Scryfall
     * @return List of types from a card typeline
     */
    private static List<String> extractTypesFromJson(JSONObject json) {
        List<String> types = new ArrayList<>();
        if (json == null) return types;

        String typeLine;

        // if card is MDFC
        if (json.has("card_faces")) {
            JSONArray faces = json.optJSONArray("card_faces");
            if (faces.length() > 0) {
                typeLine = faces.getJSONObject(0).optString("type_line", "");
            }
            else {
                typeLine = json.optString("type_line", "");
            }
        }     
        else {
            typeLine = json.optString("type_line", "");
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
     * EX: {"White", "Black", "Blue"} - Non-abbreviated!
     * @param json - JSONObject representing a card's data, returned from Scryfall
     * @return color identity list as a List<String> directly from the JSON
     */
    private static List<String> extractColorsFromJson(JSONObject json) {
        List<String> res = new ArrayList<>();
        if (json == null) return res;

        JSONArray arr = json.optJSONArray("color_identity");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                res.add(arr.getString(i));
            }
        }
        return res;
    }

    /**
     * Extracts image URL from JSONObject image_uris field
     * 
     * @param json - JSONObject representing a card's data, returned from Scryfall
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
     * Extracts cards approximate price (TCGPlayer) in USD
     * 
     * @param json - JSONObject representing a card's data, returned from Scryfall
     * @return double price in USD
     */
    private static double extractPriceFromJson(JSONObject json) {
        if (json == null) return 0.0;

        JSONObject prices = json.optJSONObject("prices");
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
     * Extracts cards converted mana cost (cmc) as a double
     * 
     * @param json - JSONObject representing a card's data, returned from Scryfall
     * @return double representing converted mana cost
     */
    private static double extractCMC(JSONObject json){
        if (json == null) return 0.0;
        return json.optDouble("cmc", 0.0);
    }

    /**
     * Extracts cards colored mana cost as a Map<String, Integer>
     * 
     * @param String manaCost
     * @return Map<String, Integer> mapping Mana color (WUBRG) to amount
     */
    private static Map<String, Integer> parsePips(String manaCost){
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