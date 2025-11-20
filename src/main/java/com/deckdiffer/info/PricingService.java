/**
 * PricingService.java; Handles Scryfall API requests and price lookups
 *
 * Provides methods for fetching card JSON from the Scryfall API,
 * extracting USD card prices, calculating total cost,
 *  and parsing card names from formatted labels.
 *
 * Methods:
 * - JSONObject fetchCardJson(String):
 *      Retrieves a card's Scryfall JSON object using a fuzzy-name search,
 *      caching results to reduce API calls.
 *
 * - double fetchCardPrice(String):
 *      Extracts the card's USD price from its Scryfall JSON object.
 *
 * - double getTotalUpgradeCost(Map<String, Integer>):
 *      Computes the total upgrade cost by summing (price * quantity) for cards.
 *
 * - String extractCardNameFromLabel(String):
 *      Removes leading quantity numbers from labels such as "3 Lightning Bolt".
 */

package com.deckdiffer.info;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class PricingService {

    private static final Map<String, JSONObject> cardJsonCache = new HashMap<>();

    private PricingService() {
    }

    /**
     * @param cardName: the name of the card to query
     * @return JSONObject representing Scryfall card data
     */
    static JSONObject fetchCardJson(String cardName) {

        // normalize key
        String key = cardName.toLowerCase();

        // check the cache
        if (cardJsonCache.containsKey(key)) {
            return cardJsonCache.get(key);
        }

        try {
            String query = "https://api.scryfall.com/cards/named?fuzzy=" + cardName.trim().replace(" ", "+");

            HttpURLConnection conn = (HttpURLConnection) new URL(query).openConnection();
            conn.setRequestMethod("GET");
            BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }
            bufferedReader.close();

            JSONObject json = new JSONObject(response.toString());

            // store in cache
            cardJsonCache.put(key, json);

            return json;
        }
        catch (Exception e) {
            System.err.println("Failed to fetch card JSON for " + cardName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * @param cardName
     * @return card price in USD
     */
    public static double fetchCardPrice(String cardName) {
        JSONObject card = fetchCardJson(cardName);
        if (card == null){
            return 0.0;
        }
        JSONObject prices = card.optJSONObject("prices");
        if (prices == null){
            return 0.0;
        }
        String usd = prices.optString("usd", "0.0");
        return Double.parseDouble(usd);        
    }

    /**
     * @param cardMap
     * @return total cost of a deck in USD
     */
    public static double getTotalCost(Map<String, Integer> cardMap) {
        double total = 0.0;
        for (var entry : cardMap.entrySet()) {
            double price = fetchCardPrice(entry.getKey());
            total += price * entry.getValue();
        }
        return total;
    }

    /**
     * @param label
     * @return Card name
     */
    public static String extractCardNameFromLabel(String label){
        int indexOfSpace = label.indexOf(' ');
        if (indexOfSpace > 0){
            return label.substring(indexOfSpace + 1).trim();
        }
        return label;
    }
}
