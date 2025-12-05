// Model for a given card
package com.deckdiffer.info;
import java.util.Map;
import java.util.List;
import org.json.JSONObject;

public class CardData {
    public final JSONObject json;
    public final List<String> types;
    public final String primaryType;
    public final List<String> colors;
    public final String colorCategory;
    public final double price;
    public final String imageUrl;
    public final String scryfallUrl;
    public final double cmc;
    public final Map<String, Integer> pipCounts;

    public CardData(JSONObject json, List<String> types, String primaryType, List<String> colors, String colorCategory, double price, String imageUrl, String scryfallUrl, double cmc, Map<String, Integer> pipCounts){
        this.json = json;
        this.types = types;
        this.primaryType = primaryType;
        this.colors = colors;
        this.colorCategory = colorCategory;
        this.price = price;
        this.imageUrl = imageUrl;
        this.scryfallUrl = scryfallUrl;
        this.cmc = cmc;
        this.pipCounts = pipCounts;
    }
}
