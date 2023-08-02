package com.ithit.webdav.samples.androidfsstorage.android;

/**
 * Interface to work with SQLLite database to store extended attributes.
 */
public interface IDatabaseHandler {

    /**
     * Saves attribute info to database.
     * @param path Path of the item.
     * @param attribute Attribute name.
     * @param value Attribute value.
     * @return Count of the records inserted or -1 if nothing.
     */
    long saveInfo(String path, String attribute, String value);

    /**
     * Reads attribute info from the database.
     * @param path Path of the item.
     * @param attribute Attribute name.
     * @return Attribute value.
     */
    String getInfo(String path, String attribute);

    /**
     * Deletes attribute from the database.
     * @param path Path of the item.
     * @param attribute Attribute name.
     * @return Count of the record deleted.
     */
    int deleteInfo(String path, String attribute);

    /**
     * Clears the database.
     */
    void deleteAll();
}
