package net.valdora.trainers;

import com.google.gson.*;

import java.lang.reflect.Type;

public class ConfigPokemonAdapter implements JsonDeserializer<ConfigPokemon> {
    private static final Gson vanillaGson = new Gson();
    
    @Override
    public ConfigPokemon deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        
        if (obj.has("requiredFlag") && obj.has("requiredValue")) {
            return vanillaGson.fromJson(obj, ConditionalConfigPokemon.class);
        }
        
        return vanillaGson.fromJson(obj, ConfigPokemon.class);
    }
}
