import static spark.Spark.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;
import spark.utils.IOUtils;

public class DeckListDifferServer {

    
    public static void main(String[] args) {
        port(4567);
        staticFiles.location("/public");

        // Upload Form HTML
        get("/", (req, res) -> """
            <html>
            <head>
                <title>Decklist Differ</title>
                <style>
                    body { font-family: Arial; margin: 40px; }
                    input[type=file] { margin: 10px 0; }
                    button { margin-top: 20px; padding: 10px 20px; font-size: 16px; }
                </style>
            </head>
            <body>
                <h1>Decklist Differ</h1>
                <form action='/compare' method='post' enctype='multipart/form-data'>
                    <p><b>Upload Base Decklist:</b></p>
                    <input type='file' name='baseFile' accept='.txt' required><br>
                    <p><b>Upload Upgraded Decklist:</b></p>
                    <input type='file' name='upgradedFile' accept='.txt' required><br>
                    <button type='submit'>Compare Decks</button>
                </form>
            </body>
            </html>
        """);

        // ---- Handle Uploaded Decks
        post("/compare", (req, res) -> {
            req.attribute("org.eclipse.jetty.multipartConfig", new javax.servlet.MultipartConfigElement("/tmp"));

            // Read uploaded files
            // read raw request from jetty
            String baseDeck = readUploadedFile(req.raw().getPart("baseFile")); // from form post request
            String upgradedDeck = readUploadedFile(req.raw().getPart("upgradedFile"));

            // card name -> card count
            Map<String, Integer> baseMap = parseDeck(baseDeck);
            Map<String, Integer> upgradedMap = parseDeck(upgradedDeck);

            Map<String, Integer> cardsToAdd = new LinkedHashMap<>();
            Map<String, Integer> cardsToRemove = new LinkedHashMap<>();
            Map<String, Integer> cardsInCommon = new LinkedHashMap<>();

            // Compare by Quantities
            Set<String> allCards = new HashSet<>();
            allCards.addAll(baseMap.keySet());
            allCards.addAll(upgradedMap.keySet());

            for (String card : allCards) {
                int baseCount = baseMap.getOrDefault(card, 0);
                int upgradeCount = upgradedMap.getOrDefault(card, 0);

                // need to add cards
                if (upgradeCount > baseCount) {
                    cardsToAdd.put(card, upgradeCount - baseCount);
                }

                // need to remove cards
                else if (baseCount > upgradeCount) {
                    cardsToRemove.put(card, baseCount - upgradeCount);
                }

                int common = Math.min(baseCount, upgradeCount);
                if (common > 0) {
                    cardsInCommon.put(card, common);
                }
            }

            int totalAddCount = sumCounts(cardsToAdd);
            int totalRemoveCount = sumCounts(cardsToRemove);
            int totalCommonCount = sumCounts(cardsInCommon);

            double totalUpgradeCost = getTotalUpgradeCost(cardsToAdd);

            // Write downloadable files
            writeToFile("cards_to_add.txt", mapToLines(cardsToAdd));
            writeToFile("cards_to_remove.txt", mapToLines(cardsToRemove));
            writeToFile("cards_in_common.txt", mapToLines(cardsInCommon));

            // Build HTTP Response
            StringBuilder html = new StringBuilder();
            html.append("""
                <html>
                <head>
                    <title>Deck Comparison Result</title>
                    <style>
                        body { font-family: Arial; margin: 40px; }
                        ul { line-height: 1.5; }
                    </style>
                </head>
                <body>
                    <h1>Deck Comparison Results</h1>
            """);
            // Build Cards to Remove, Add, in Common HTML
            // Card Name - Card Count
            html.append("<h3>Cards to Remove (").append(totalRemoveCount).append(" Total)</h3><ul>");
            for (var entry : cardsToRemove.entrySet())
                html.append("<li>").append(entry.getValue()).append(" ").append(entry.getKey()).append("</li>");
            html.append("</ul>");

            html.append("<h3>Cards to Add (").append(totalAddCount).append(" Total)</h3><ul>");
            for (var entry : cardsToAdd.entrySet())
                html.append("<li>").append(entry.getValue()).append(" ").append(entry.getKey()).append(" $").append(String.format("%.2f", fetchCardPrice(entry.getKey()))).append("</li>");

            html.append("</ul>");

            html.append("<h3>Cards in Common (").append(totalCommonCount).append(" Total)</h3><ul>");
            for (var entry : cardsInCommon.entrySet())
                html.append("<li>").append(entry.getValue()).append(" ").append(entry.getKey()).append("</li>");
            html.append("</ul>");

            // Total Cost and Download Links
            html.append("<p><b>Total Upgrade Cost:</b> $")
                .append(String.format("%.2f", totalUpgradeCost)).append("</p><hr>")
                .append("<h3>Download Results</h3>")
                .append("<a href='/download/cards_to_add'>Download cards_to_add.txt</a><br>") // Generates download link. Clicking it sends get request
                .append("<a href='/download/cards_to_remove'>Download cards_to_remove.txt</a><br>")
                .append("<a href='/download/cards_in_common'>Download cards_in_common.txt</a><br><br>")
                .append("<a href='/'>Compare Another Deck</a>")
                .append("</body></html>");

            return html.toString();
        });

        // Create Download Endpoint and
        // : indicates dynamic url path, so pass in :filename as a param
        // Initiates download and deals with get request
        get("/download/:filename", (req, res) -> {
            String fileName = req.params(":filename") + ".txt";

            // Path is an interface. Stores ref to a filepath
            // Paths is a utility class
            Path filePath = Paths.get(fileName);

            if (!Files.exists(filePath)) {
                res.status(404);
                return "File not found: " + fileName;
            }

            res.type("text/plain");
            res.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            return Files.readString(filePath);
        });
    }

    // === HELPER METHODS ===

    // 1) Take uploaded filePart
    // 2) Read contents into big string builder
    // 3) Return stream as string
    private static String readUploadedFile(javax.servlet.http.Part filePart) throws IOException {
    try (InputStream input = filePart.getInputStream();
         InputStreamReader reader = new InputStreamReader(input);
         BufferedReader bufferedReader = new BufferedReader(reader)) {

        StringBuilder sb = new StringBuilder();
        String line;

        // Read file line by line
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line).append("\n");
        }

        // Return as string
        return sb.toString();
        }
    }

    // Parse lines, extracting card count and card name
    // Return map: Card Name -> Card Count
    private static Map<String, Integer> parseDeck(String deckText) {
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

    // Sum the total number of cards in a card map
    private static int sumCounts(Map<String, Integer> cardMap) {
        int sum = 0;
        for (int val : cardMap.values()){
            sum += val;
        }
        return sum;
    }

    // Convert map (card name -> card count) to representative line
    // Ex: {Island} -> {3} to "3 Island"
    // Returns List<String> of those lines
    private static List<String> mapToLines(Map<String, Integer> cardMap) {
        List<String> lines = new ArrayList<>();
        for (var entry : cardMap.entrySet()) {
            lines.add(entry.getValue() + " " + entry.getKey());
        }
        return lines;
    }

    // write line lines to fileName
    private static void writeToFile(String fileName, List<String> lines) {
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

    // Sum prices using api for each card and return total cost
    private static double getTotalUpgradeCost(Map<String, Integer> cardMap) {
        double total = 0.0;
        for (var entry : cardMap.entrySet()) {
            double price = fetchCardPrice(entry.getKey());
            total += price * entry.getValue();
        }
        return total;
    }

    // Fetch Card Object from scryfall API
    private static JSONObject fetchCardJson(String cardName) {
        try {
            String query = "https://api.scryfall.com/cards/named?fuzzy=" +cardName.trim().replace(" ", "+");

            @SuppressWarnings("deprecation")
            HttpURLConnection conn = (HttpURLConnection) new URL(query).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }
            bufferedReader.close();

            return new JSONObject(response.toString());
        }
        catch (Exception e) {
            System.err.println("Failed to fetch card JSON for " + cardName + ": " + e.getMessage());
            return null;
        }
    }

    // Access fetched json object to extract card price in USD
    private static double fetchCardPrice(String cardName) {
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

    

    // TODO
    // Access fetched json object to extract type line
    // "artifact", "equipment", etc
    // Problem: Multiple cards may have multiple type lines
    private static String fetchTypeLine(String cardName){
        JSONObject card = fetchCardJson(cardName);
        if (card == null){
            return "?";
        }
        return card.optString("type_line", "?");
    }

    // Access fetched json object to extract colors as an array
    private static List<String> fetchColors(String cardName){
        JSONObject card = fetchCardJson(cardName);
        if (card == null){
            return new ArrayList<>();
        }
        JSONArray colorsArray = card.optJSONArray("colors");
        List<String> colors = new ArrayList<>();
        
        if (colorsArray != null){
            for (int i = 0; i < colorsArray.length(); i++){
                colors.add(colorsArray.getString(i));
            }
        }
        Collections.sort(colors);
        return colors;
    }

    }
