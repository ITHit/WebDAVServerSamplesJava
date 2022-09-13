package com.ithit.webdav.samples.fsstorageservlet


import com.google.gson.Gson
import java.lang.reflect.Type
import java.util.*

/**
 * Utility class to perform serialization of objects.
 */
internal object SerializationUtils {

    /**
     * Serializes object to JSON string.
     *
     * @param object Object to serialize.
     * @return String in JSON format
     */
    fun <T> serialize(`object`: T): String {
        return Gson().toJson(`object`)
    }

     /**
     * Deserialize JSON string to object list.
     *
     * @param clazz Type of objects in the list to deserialize.
     * @param json  JSON string to deserialize.
     * @return List of objects.
     */
     @Suppress("UNCHECKED_CAST")
    fun <T> deserializeList(clazz: Class<T>, json: String?): List<T> {
        var array: Array<T>? = java.lang.reflect.Array.newInstance(clazz, 1) as Array<T>?
        array = Gson().fromJson<Array<T>>(json, array!!.javaClass as Type)
        return if (array == null) {
            ArrayList()
        } else ArrayList(listOf(*array))
    }
}
