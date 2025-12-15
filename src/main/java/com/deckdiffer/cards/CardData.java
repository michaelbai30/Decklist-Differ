/**
 * CardData.java:
 * 
 * Provides a model for a given card
 * Populated after JSON is downloaded from Scryfall and relevant fields are extracted
 */

package com.deckdiffer.cards;
import java.util.Map;
import java.util.List;
import org.json.JSONObject;

public final class CardData {
    public final JSONObject json; // Raw Scryfall JSON response
    public final List<String> types; // All card types and subtypes (ex: Creature, Artifact, Enchantment)
    public final String primaryType; // Main type used for grouping
    public final List<String> colors; // WUBRG color identity of the card, ex: {"W", "U", "B"}
    public final String colorCategory; // String representing color category, ex: "WUB"
    public final double price; // Price of the card in USD according to Scryfall
    public final String imageUrl; // Artwork image URL for rendering
    public final String scryfallUrl; // Link to the card on Scryfall
    public final double cmc; // Converted mana cost of card
    public final Map<String, Integer> pipCounts; // Count of mana symbols by color (ex: W:2, U;1)

    public CardData(JSONObject json, List<String> types, String primaryType, List<String> colors, String colorCategory, double price, String imageUrl, String scryfallUrl, double cmc, Map<String, Integer> pipCounts){
        this.json = json;
        this.types = List.copyOf(types);
        this.primaryType = primaryType;
        this.colors = List.copyOf(colors);
        this.colorCategory = colorCategory;
        this.price = price;
        this.imageUrl = imageUrl;
        this.scryfallUrl = scryfallUrl;
        this.cmc = cmc;
        this.pipCounts = Map.copyOf(pipCounts);
    }
}
