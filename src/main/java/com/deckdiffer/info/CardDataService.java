/**
 * CardDataService.java; Fetches Scryfall JSON and converts it into CardData objects.
 *
 * Responsibilities:
 * - Perform fuzzy-name Scryfall API lookups
 * - Cache JSON to minimize repeated API calls
 * - Build structured CardData objects to be used in CardDataInfo etc..
 */

package com.deckdiffer.info;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

public class CardDataService {

    private static final Map<String, JSONObject> cardJsonCache = new HashMap<>();
    private static final Map<String, CardData> cardDataCache = new HashMap<>();

    private CardDataService() {
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

        List<String> types = CardInfoService.extractTypesFromJson(json);
        String primaryType = CardInfoService.fetchPrimaryType(types);

        List<String> colors = CardInfoService.extractColorsFromJson(json);
        String colorCategory = CardInfoService.assignColorCategory(colors);

        double price = CardInfoService.extractPriceFromJson(json);

        String imageUrl = extractImageUrl(json);
        String scryfallUrl = json.optString("scryfall_uri", null);

        double cmc = CardInfoService.extractCMC(json);

        Map<String, Integer> pipCounts = CardInfoService.parsePips(json.optString("mana_cost", ""));

        return new CardData(json, types, primaryType, colors, colorCategory, price, imageUrl, scryfallUrl, cmc, pipCounts);
    }


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
}
