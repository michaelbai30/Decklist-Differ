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
    private static final Map<String, JSONObject> cardJsonCache = new HashMap<>();

    // Maps: file name -> file content
    private static Map<String, String> generatedTxtFiles = new HashMap<>();

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
                textarea { width: 100%; height: 200px; margin-bottom: 20px; }
                button { padding: 10px 20px; font-size: 16px;}
            </style>
        </head>
        <body>
            <h1>Decklist Differ</h1>
            <form action='/compare' method='post'>
            <p><b>Base Decklist:</b></p>
            <textarea name='baseText' placeholder='e.g.:
1 Swords to Plowshares
1 Chaos Warp
1 Lightning Bolt
...'></textarea>

            <p><b>Upgraded Decklist:</b></p>
            <textarea name='upgradedText' placeholder='e.g.:
1 Swords to Plowshares
1 Chaos Warp
1 Blasphemous Act
...'></textarea>
            <button type='submit'>Compare Decks</button>

            </form>
        </body>
        </html>
    """);

        // ---- Handle Uploaded Decks ---
        post("/compare", (req, res) -> {

            String baseDeck = req.queryParams("baseText");
            String upgradedDeck = req.queryParams("upgradedText");

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

            double totalUpgradeCost = getTotalUpgradeCost(cardsToAdd);

            Map<String, Integer> sortedAdd = new TreeMap<>(cardsToAdd);
            Map<String, Integer> sortedRemove = new TreeMap<>(cardsToRemove);
            Map<String, Integer> sortedInCommon= new TreeMap<>(cardsInCommon);

            // Generate downloadable files instead of writing them to disk 
            generatedTxtFiles.put("cards_to_add.txt", String.join("\n", mapToLines(sortedAdd)));
            generatedTxtFiles.put("cards_to_remove.txt", String.join("\n", mapToLines(sortedRemove)));
            generatedTxtFiles.put("cards_in_common.txt", String.join("\n", mapToLines(sortedInCommon)));
            generatedTxtFiles.put("cards_to_add_detailed.txt", buildDetailedTxtFile(cardsToAdd));
            generatedTxtFiles.put("cards_to_remove_detailed.txt", buildDetailedTxtFile(cardsToRemove));
            generatedTxtFiles.put("cards_in_common_detailed.txt", buildDetailedTxtFile(cardsInCommon));

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

            // Cards to Remove HTML
            html.append(buildGroupedHtml("Cards to Remove", cardsToRemove, false));

            // Cards to Add HTML
            html.append(buildGroupedHtml("Cards to Add", cardsToAdd, true));

            // Cards in Common HTML
            html.append(buildGroupedHtml("Cards in Common", cardsInCommon, false));

            // Total Cost and Download Links
            html.append("<p><b>Total Upgrade Cost:</b> $")
                .append(String.format("%.2f", totalUpgradeCost)).append("</p><hr>")
                .append("<h3>Download Results</h3>")
                .append("<a href='/download/cards_to_add'>Download cards_to_add.txt</a><br>") // Generates download link. Clicking it sends get request
                .append("<a href='/download/cards_to_remove'>Download cards_to_remove.txt</a><br>")
                .append("<a href='/download/cards_in_common'>Download cards_in_common.txt</a><br><br>")
                .append("<a href='/download/cards_to_add_detailed'>Download cards_to_add_detailed.txt</a><br>")
                .append("<a href='/download/cards_to_remove_detailed'>Download cards_to_remove_detailed.txt</a><br>")
                .append("<a href='/download/cards_in_common_detailed'>Download cards_in_common_detailed.txt</a><br><br>")
                .append("<a href='/'>Compare Another Deck</a>")
                .append("</body></html>");

            return html.toString();
        });

        // Create Download Endpoint and
        // : indicates dynamic url path, so pass in :filename as a param
        // Initiates download and deals with get request
        get("/download/:filename", (req, res) -> {
            String fileName = req.params(":filename") + ".txt";

            if (!generatedTxtFiles.containsKey(fileName)){
                res.status(404);
                return "File Not Found";
            }

            res.type("text/plain");
            res.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            return generatedTxtFiles.get(fileName);
        });

        get("/download/:filename_detailed", (req, res) -> {
            String fileName = req.params(":filename_detailed") + ".txt";

            if (!generatedTxtFiles.containsKey(fileName)){
                res.status(404);
                return "File Not Found";
            }

            res.type("text/plain");
            res.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            return generatedTxtFiles.get(fileName);
        });
    }

    // === HELPER METHODS ===

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
    // with cache for efficiency / prevent rate limits
    private static JSONObject fetchCardJson(String cardName) {

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

    // Needed for fetchCardTypes
    private static final Set<String> CARD_TYPES = Set.of(
    "Artifact", "Creature", "Enchantment", "Instant",
    "Sorcery", "Land", "Planeswalker", "Battle", "Tribal"
    );

    // Access fetched json object to extract type line(s)
    private static List<String> fetchCardTypes(String cardName){
        JSONObject card = fetchCardJson(cardName);
        List<String> types = new ArrayList<>();
        String typeLine;
        if (card == null){
            return new ArrayList<>();
        }
        if (card.has("card_faces")){
            JSONArray faces = card.optJSONArray("card_faces");
            if (faces.length() > 0){
                typeLine = faces.getJSONObject(0).optString("type_line", "");
            }
            else{
                typeLine = card.optString("type_line", "");
            }
        }
        else{
            typeLine = card.optString("type_line", "");
        }
        String[] sections = typeLine.split("—");

        // contains all keywords that a card may contain
        String leftSide = sections[0].trim();
        for (String word: leftSide.split("\\s+")){
            if (CARD_TYPES.contains(word)){
                types.add(word);
            }
        }
        return types;
    }

    // Access fetched json object to extract color identity as an array
    private static List<String> fetchColorIdentity(String cardName){
        JSONObject card = fetchCardJson(cardName);
        if (card == null){
            return new ArrayList<>();
        }
        JSONArray colorsArray = card.optJSONArray("color_identity");
        List<String> colors = new ArrayList<>();
        
        if (colorsArray != null){
            for (int i = 0; i < colorsArray.length(); i++){
                colors.add(colorsArray.getString(i));
            }
        }
        Collections.sort(colors);
        return colors;
    }

    // Handle type collisions
    // If a card is an Artifact Creature, it is dominantly a creature
    // Follows standard priority used by other websites
    private static final List<String> TYPE_PRIORITY = List.of(
        "Creature",
        "Land",
        "Artifact",
        "Enchantment",
        "Planeswalker",
        "Instant",
        "Sorcery",
        "Battle",
        "Tribal"
        );
    
    // Ordered iteration provides primary type
    private static String fetchPrimaryType(List<String> types){
        for (String type: TYPE_PRIORITY){
            if (types.contains(type)){
                return type;
            }
        }
        return "Other";

    }
    // Return priority index of a type
    private static int typeToPriority(String type){
        int idx = TYPE_PRIORITY.indexOf(type);
        if (idx >= 0){
            return idx;
        }
        else{
            return 9999; // if the card's type is not found for some reason, give it lowest possible priority
        }
    }

   // ex: ["White", "Blue"] -> "WU"
   private static String assignColorCategory(List<String> colorIdentity){

        // Colorless
        if (colorIdentity == null || colorIdentity.isEmpty()){
            return "Colorless";
        }

        // Monocolored
        if (colorIdentity.size() == 1){
            switch (colorIdentity.get(0)){
                case "W": return "White";
                case "U": return "Blue";
                case "B": return "Black";
                case "R": return "Red";
                case "G": return "Green";
        }   
        }      

        // Else is multicolored
        return String.join("", colorIdentity); // ex: "RW", "BG", "WUB"
}
 
    // Given string representing color identity -> sort
    // Sorting logic used by other sites
    private static int colorSortKey(String colors) {
        // Single color switching
        switch (colors) {
            case "White": return 0;
            case "Blue": return 1;
            case "Black": return 2;
            case "Red": return 3;
            case "Green": return 4;
        }

        if (colors.equals("Colorless")) {
            return 9999;
        }

        // Multicolor
        // Priority based on size first then alphabet
        // size 2 colors → group 10
        // size 3 colors → group 20
        // size 4 colors → group 30
        // size 5 colors → group 40

        int len = colors.length();  // number of colors
        int baseGroup = len * 10;

        // add alphabetical weight
        return baseGroup + (colors.hashCode() & 0x7fffffff);
    }

    // Sort by primary type -> color, cards in cardMap by extracting key (cards)
    private static List<String> sortCardsByTypeThenColor(Map<String, Integer> cardMap){
        List<String> cards = new ArrayList<>(cardMap.keySet());
        cards.sort((card1, card2) -> {

            // Determine by Card Primary Type First
            List<String> card1Types = fetchCardTypes(card1);
            List<String> card2Types = fetchCardTypes(card2);

            String card1PrimaryType = fetchPrimaryType(card1Types);
            String card2PrimaryType = fetchPrimaryType(card2Types);

            int typeComparison = Integer.compare(typeToPriority(card1PrimaryType), typeToPriority(card2PrimaryType));
            if (typeComparison != 0){
                return typeComparison;
            }

            // Then look at color categories
            List<String> card1Colors = fetchColorIdentity(card1);
            List<String> card2Colors = fetchColorIdentity(card2);

            String card1ColorCategory = assignColorCategory(card1Colors);
            String card2ColorCategory = assignColorCategory(card2Colors);

            int colorComparison = Integer.compare(colorSortKey(card1ColorCategory), colorSortKey(card2ColorCategory));

            if (colorComparison != 0){
                return colorComparison;
            }

            return (card1.compareToIgnoreCase(card2));
        });

        return cards;

    }

    // For HTML
    private static String buildDisplayLabel(String cardName, int count) {
        return count + " " + cardName;
    }

    // ex: Creature -> White -> ["1 Cloud, Midgar Mercenary"]
    private static Map<String, Map<String, List<String>>> groupTypeThenColor(Map<String, Integer> cardMap){
        Map<String, Map<String, List<String>>> res = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> entry : cardMap.entrySet()){
            String card = entry.getKey(); // card name
            int count = entry.getValue(); // num copies

            List<String> type = fetchCardTypes(card);
            String primaryType = fetchPrimaryType(type);

            List<String> colors = fetchColorIdentity(card);
            String colorCategory = assignColorCategory(colors);


            // Outer:
            res.putIfAbsent(primaryType, new LinkedHashMap<>());

            // Inner:
            res.get(primaryType).putIfAbsent(colorCategory, new ArrayList<>());

            // Build card label
            String label = buildDisplayLabel(card, count);

            res.get(primaryType).get(colorCategory).add(label);
        }
        return res;
    }

    private static String buildGroupedHtml(String title, Map<String, Integer> cardMap, Boolean isAdd) {
        StringBuilder html = new StringBuilder();

        html.append("<h2>").append(title).append(" (")
            .append(sumCounts(cardMap))
            .append(" Total)</h2>");

        Map<String, Map<String, List<String>>> grouped = groupTypeThenColor(cardMap);

        // Sort primary types by priority
        List<String> primaryTypes = new ArrayList<>(grouped.keySet());
        primaryTypes.sort(Comparator.comparingInt(DeckListDifferServer::typeToPriority));

        for (String type : primaryTypes) {
            
            int typeCount = 0;
            Map<String, List<String>> colors = grouped.get(type);

            // Count the number of cards per type (creature, artifact, etc...)
            for (List<String> cardLabels: colors.values()){
                for (String label: cardLabels){
                    int indexOfSpace = label.indexOf(' ');
                    if (indexOfSpace > 0){
                        // extract the "4" out of "4 Lightning Bolt" for example
                        int count = Integer.parseInt(label.substring(0, indexOfSpace));
                        typeCount += count;
                    }
                }
            }

            html.append("<h3>").append(type).append(" (").append(typeCount).append(")").append("</h3><ul>");

            // Sort colors using colorSortKey
            List<String> colorKeys = new ArrayList<>(colors.keySet());
            colorKeys.sort(Comparator.comparingInt(DeckListDifferServer::colorSortKey));

            for (String color : colorKeys) {
                html.append("<li><b>").append(color).append("</b><ul>");

                List<String> cardLabels = colors.get(color);
                Collections.sort(cardLabels, String::compareToIgnoreCase);

                for (String label : cardLabels) {
                    if (isAdd){
                        Double cardPrice = fetchCardPrice(label);
                        html.append("<li>").append(label).append(" ($").append(Double.toString(cardPrice) + ")").append("</li>");
                    }
                    else{
                        html.append("<li>").append(label).append("</li>");
                    }
                    
                }
                html.append("</ul></li>");
            }

            html.append("</ul>");
        }

        return html.toString();
    }

    // Write to a txt file card names and quantity categorized by primary type then color cat
    private static String buildDetailedTxtFile(Map<String, Integer> cardMap){
        StringBuilder sb = new StringBuilder();
        Map<String, Map<String, List<String>>> grouped = groupTypeThenColor(cardMap);
        
        // Sort primary types by priority
        List<String> primaryTypes = new ArrayList<>(grouped.keySet());
        primaryTypes.sort(Comparator.comparingInt(DeckListDifferServer::typeToPriority));
               
            for (String type : primaryTypes){
                int typeCount = 0;
                Map<String, List<String>> colors = grouped.get(type);

                // count total cards for this primary type
                for (List<String> cardLabels : colors.values()) {
                    for (String label : cardLabels) {
                        int idx = label.indexOf(' ');
                        if (idx > 0) {
                            int count = Integer.parseInt(label.substring(0, idx));
                            typeCount += count;
                        }
                    }
                }

                    // Build String Header
                    sb.append("# ").append(type.toUpperCase()).append(" (").append(typeCount).append(")\n");

                    // Sort Colors
                    List<String> colorKeys = new ArrayList<>(colors.keySet());
                    colorKeys.sort(Comparator.comparingInt(DeckListDifferServer::colorSortKey));

                    for (String color : colorKeys){
                        sb.append("# ").append(color.toUpperCase()).append("\n");

                        List<String> cardLabels = colors.get(color);
                        Collections.sort(cardLabels, String::compareToIgnoreCase);

                        for (String label : cardLabels) {
                            sb.append(label).append("\n");
                        }          
                        sb.append("\n");
                    }
                    sb.append("\n");
                }
                return sb.toString();
        }
    }