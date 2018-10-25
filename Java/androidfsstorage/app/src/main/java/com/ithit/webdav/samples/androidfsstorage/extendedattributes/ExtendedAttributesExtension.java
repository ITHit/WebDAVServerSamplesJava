package com.ithit.webdav.samples.androidfsstorage.extendedattributes;

import com.ithit.webdav.samples.androidfsstorage.android.IDatabaseHandler;
import com.ithit.webdav.server.exceptions.ServerException;

/**
 * Helper extension methods for custom attributes.
 */
public class ExtendedAttributesExtension {

    private static IDatabaseHandler databaseHandler;

    /**
     * Gets extended attribute or null if attribute or file not found.
     *
     * @param path       File or folder path to get extended attributes.
     * @param attribName Attribute name.
     * @return Attribute value or null if attribute or file not found.
     * @throws ServerException Throw when file or attribute is no available.
     */
    public static String getExtendedAttribute(String path, String attribName) throws ServerException {
        return getExtendedAttributeSupport(databaseHandler).getExtendedAttribute(path, attribName);
    }

    /**
     * Sets extended attribute.
     *
     * @param path        File or folder path to set extended attributes.
     * @param attribName  Attribute name.
     * @param attribValue Attribute value.
     * @throws ServerException Throw when file or attribute is no available.
     */
    public static void setExtendedAttribute(String path, String attribName, String attribValue) throws ServerException {
        getExtendedAttributeSupport(databaseHandler).setExtendedAttribute(path, attribName, attribValue);
    }

    /**
     * Checks extended attribute existence.
     *
     * @param path       File or folder path to look for extended attributes.
     * @param attribName Attribute name.
     * @return True if attribute exist, false otherwise.
     * @throws ServerException Throw when file or attribute is no available.
     */
    public static boolean hasExtendedAttribute(String path, String attribName) throws ServerException {
        return getExtendedAttributeSupport(databaseHandler).getExtendedAttribute(path, attribName) != null;
    }

    /**
     * Deletes extended attribute.
     *
     * @param path       File or folder path to delete extended attributes.
     * @param attribName Attribute name.
     * @throws ServerException Throw when file or attribute is no available.
     */
    public static void deleteExtendedAttribute(String path, String attribName) throws ServerException {
        getExtendedAttributeSupport(databaseHandler).deleteExtendedAttribute(path, attribName);
    }

    public static ExtendedAttribute getExtendedAttributeSupport(IDatabaseHandler databaseHandler) {
        ExtendedAttributesExtension.databaseHandler = databaseHandler;
        return ExtendedAttributeFactory.buildFileExtendedAttributeSupport(databaseHandler);
    }
}
