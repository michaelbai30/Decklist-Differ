// Model for a given card
package com.deckdiffer.info;

import java.util.List;
import org.json.JSONObject;

public class CardData {
    public JSONObject json;
    public List<String> types;
    public String primaryType;
    public List<String> colors;
    public String colorCategory;
    public double price;

    public CardData(JSONObject json, List<String> types, String primaryType, List<String> colors, String colorCategory, double price){
        this.json = json;
        this.types = types;
        this.primaryType = primaryType;
        this.colors = colors;
        this.colorCategory = colorCategory;
        this.price = price;
    }
}
