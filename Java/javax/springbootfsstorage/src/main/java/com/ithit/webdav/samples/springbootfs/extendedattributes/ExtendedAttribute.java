package com.ithit.webdav.samples.springbootfs.extendedattributes;

import java.io.IOException;

/**
 * Provides support for reading, writing and removing of extended attributes.
 */
public interface ExtendedAttribute {

    String TEST_PROPERTY = "test";

    /**
     * Determines whether extended attributes are supported.
     *
     * @param path File or folder path to check attribute support.
     * @return True if extended attributes are supported, false otherwise.
     */
    default boolean isExtendedAttributeSupported(String path) {
        boolean supports = true;
        try {
            setExtendedAttribute(path, TEST_PROPERTY, TEST_PROPERTY);
            deleteExtendedAttribute(path, TEST_PROPERTY);
        } catch (Exception e) {
            supports = false;
        }
        return supports;
    }

    /**
     * Write the extended attribute to the file.
     *
     * @param path          File or folder path to write attribute.
     * @param attribName    Attribute name.
     * @param attribValue   Attribute value.
     * @throws IOException  If file is not available or write attribute was unsuccessful.
     */
    void setExtendedAttribute(String path, String attribName, String attribValue) throws IOException;

    /**
     * Reads extended attribute.
     *
     * @param path          File or folder path to read extended attribute.
     * @param attribName    Attribute name.
     * @return              Attribute value or null if attribute doesn't exist.
     * @throws IOException  If file is not available or read attribute was unsuccessful.
     */
    String getExtendedAttribute(String path, String attribName) throws IOException;


    /**
     * Deletes extended attribute.
     *
     * @param path          File or folder path to remove extended attribute.
     * @param attribName    Attribute name.
     * @throws IOException  If file is not available or delete attribute was unsuccessful.
     */
    void deleteExtendedAttribute(String path, String attribName) throws IOException;

}
