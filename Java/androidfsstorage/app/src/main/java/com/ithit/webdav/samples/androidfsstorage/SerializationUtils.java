package com.ithit.webdav.samples.androidfsstorage;


import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class to perform serialization of objects.
 */
class SerializationUtils {

    /**
     * Serializes object to JSON string.
     *
     * @param object Object to serialize.
     * @return String in JSON format
     */
    static <T> String serialize(T object) {
        Gson gson = new Gson();
        return gson.toJson(object);
    }

    /**
     * Deserialize JSON string to object list.
     *
     * @param clazz Type of objects in the list to deserialize.
     * @param json  JSON string to deserialize.
     * @return List of objects.
     */
    @SuppressWarnings("unchecked")
    static <T> List<T> deserializeList(final Class<T> clazz, final String json) {
        T[] array = (T[]) java.lang.reflect.Array.newInstance(clazz, 1);
        array = new Gson().fromJson(json, (Type) array.getClass());
        if (array == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(array));
    }
}
