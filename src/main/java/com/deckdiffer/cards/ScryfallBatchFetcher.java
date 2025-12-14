package com.deckdiffer.cards;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;

public class ScryfallBatchFetcher {

    private static final String SCRYFALL_COLLECTION_URL = 
        "https://api.scryfall.com/cards/collection";
    private static final int MAX_BATCH_SIZE = 75;

    /**
     * Fetches raw Scryfall JSON objects in batches
     * @param cardNames Set of unique card names to fetch
     * @return Map of card names (strings) to its raw Scryfall JSON (JSONObject).
     */
    
    // Check API Format Here: https://scryfall.com/docs/api/cards/collection
    public static Map<String, JSONObject> fetchBatchJson(Set<String> cardNames) {
        
        Map<String, JSONObject> fetchedJson = new HashMap<>();
        List<String> cardList = new ArrayList<>(cardNames);
        
        // Loop through the card names in batches of MAX_BATCH_SIZE
        for (int i = 0; i < cardList.size(); i += MAX_BATCH_SIZE) { 

            // Extract cur batch of card names up to 75 from the full list
            List<String> batchNames = cardList.subList(i, Math.min(i + MAX_BATCH_SIZE, cardList.size()));
            
            // Build JSON request body: {"identifiers": [{"name": "Card 1"}, ...]}
            JSONObject requestBody = new JSONObject();
            JSONArray identifiers = new JSONArray(); // holds the list of cards
            for (String name : batchNames) {
                identifiers.put(new JSONObject().put("name", name)); // { "name": "Card Name" } for each card
            }
            requestBody.put("identifiers", identifiers);
            
            try {
                // Establish a connection
                URL url = new URL(SCRYFALL_COLLECTION_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(15000);
                
                // Send Request Body
                try (OutputStream os = conn.getOutputStream()) {
                    // convert complete JSON request body string to an array of bytes using standard character encoding
                    byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length); // write byte array to the connection
                }
                
                // Read Response
                String responseText;
                try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    responseText = scanner.useDelimiter("\\A").next(); // read IS into a single string
                }

                // Parse Response JSON
                JSONObject responseJson = new JSONObject(responseText);
                JSONArray dataArray = responseJson.getJSONArray("data");

                for (int j = 0; j < dataArray.length(); j++) {
                    JSONObject cardJson = dataArray.getJSONObject(j);
                    
                    String cardName = cardJson.optString("name"); 
                    
                    if (cardName != null && !cardName.isEmpty()) {
                        fetchedJson.put(cardName, cardJson);
                    }
                }
                
                // Small delay if fetching multiple batches to prevent rate limiting
                if (cardList.size() > MAX_BATCH_SIZE) {
                     Thread.sleep(50); 
                }
            }
            catch (Exception e) {
                System.err.println("Error during Scryfall batch fetch: " + e.getMessage());
            }
        }
        
        return fetchedJson;
    }
}