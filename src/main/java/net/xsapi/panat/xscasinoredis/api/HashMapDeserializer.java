package net.xsapi.panat.xscasinoredis.api;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class HashMapDeserializer implements JsonDeserializer<HashMap<Integer, Integer>> {
    @Override
    public HashMap<Integer, Integer> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        HashMap<Integer, Integer> result = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : jsonElement.getAsJsonObject().entrySet()) {
            int key = Integer.parseInt(entry.getKey());
            int value = entry.getValue().getAsInt();
            result.put(key, value);
        }
        return result;
    }
}

