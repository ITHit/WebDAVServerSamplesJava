package com.ithit.webdav.samples.springboot.extendedattributes;

import com.ithit.webdav.server.exceptions.ServerException;

import java.io.IOException;

/**
 * Helper extension methods for custom attributes.
 */
public class ExtendedAttributesExtension {

    /**
     * Reads extended attribute.
     *
     * @param path          File or folder path to read extended attribute.
     * @param attribName    Attribute name.
     * @return              Attribute value or null if attribute doesn't exist.
     * @throws ServerException  If file is not available or read attribute was unsuccessful.
     */
    public static String getExtendedAttribute(String path, String attribName) throws ServerException {
        try {
            return getExtendedAttributeSupport().getExtendedAttribute(path, attribName);
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }
    }

    /**
     * Write the extended attribute to the file.
     *
     * @param path          File or folder path to write attribute.
     * @param attribName    Attribute name.
     * @param attribValue   Attribute value.
     * @throws ServerException  If file is not available or write attribute was unsuccessful.
     */
    public static void setExtendedAttribute(String path, String attribName, String attribValue) throws ServerException {
        try {
            getExtendedAttributeSupport().setExtendedAttribute(path, attribName, attribValue);
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }
    }

    /**
     * Checks extended attribute existence.
     *
     * @param path          File or folder path to read extended attribute.
     * @param attribName    Attribute name.
     * @return              True if attribute exist, false otherwise.
     * @throws ServerException  If file is not available or read attribute was unsuccessful.
     */
    public static boolean hasExtendedAttribute(String path, String attribName) throws ServerException {
        try {
            return getExtendedAttributeSupport().getExtendedAttribute(path, attribName) != null;
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }
    }

    /**
     * Deletes extended attribute.
     *
     * @param path          File or folder path to delete extended attributes.
     * @param attribName    Attribute name.
     * @throws ServerException  If file is not available or delete attribute was unsuccessful.
     */
    public static void deleteExtendedAttribute(String path, String attribName) throws ServerException {
        try {
            getExtendedAttributeSupport().deleteExtendedAttribute(path, attribName);
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }
    }

    /**
     * Determines whether extended attributes are supported.
     *
     * @param path File or folder path to check extended attributes support.
     * @return True if extended attributes or NTFS file alternative streams are supported, false otherwise.
     */
    public static boolean isExtendedAttributesSupported(String path) {
        return getExtendedAttributeSupport().isExtendedAttributeSupported(path);
    }

    private static ExtendedAttribute getExtendedAttributeSupport() {
        return ExtendedAttributeFactory.buildFileExtendedAttributeSupport();
    }

}
