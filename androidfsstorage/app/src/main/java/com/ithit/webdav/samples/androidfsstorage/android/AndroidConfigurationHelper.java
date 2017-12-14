package com.ithit.webdav.samples.androidfsstorage.android;

import android.content.res.AssetManager;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents json configuration values collection and function to get file content from android assets.
 */
class AndroidConfigurationHelper {

    private Map<String, String> jsonValueCollection = new HashMap<>();

    AndroidConfigurationHelper(AssetManager assetManager, String jsonFileName) {
        String jsonString = readConfig(assetManager, jsonFileName);
        fillConfigMap(jsonString);
    }

    Map<String, String> getJsonValueCollection() {
        return jsonValueCollection;
    }

    private void fillConfigMap(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            JSONObject davEngineOptions = json.getJSONObject("DavEngineOptions");
            jsonValueCollection.put("License", davEngineOptions.getString("License"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String readConfig(AssetManager assetManager, String jsonFileName) {
        StringBuilder jsonString = new StringBuilder();
        try {
            List<String> jsonLines = IOUtils.readLines(assetManager.open(jsonFileName));

            for (String s: jsonLines) {
                jsonString.append(s);
            }
        } catch (IOException ignored) {
        }
        return jsonString.toString();
    }


}
