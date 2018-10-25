package com.ithit.webdav.samples.fsstorageservlet.extendedattributes;

import com.ithit.webdav.server.exceptions.ServerException;

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
     * @param path        File or folder path to write attribute.
     * @param attribName  Attribute name.
     * @param attribValue Attribute value.
     * @throws ServerException Throw when file or attribute is not available.
     */
    void setExtendedAttribute(String path, String attribName, String attribValue) throws ServerException;

    /**
     * Reads extended attribute.
     *
     * @param path       File or folder path to read extended attribute.
     * @param attribName Attribute name.
     * @return Attribute value.
     * @throws ServerException Throw when file or attribute is no available.
     */
    String getExtendedAttribute(String path, String attribName) throws ServerException;


    /**
     * Deletes extended attribute.
     *
     * @param path       File or folder path to remove extended attribute.
     * @param attribName Attribute name.
     * @throws ServerException Throw when file or attribute is no available.
     */
    void deleteExtendedAttribute(String path, String attribName) throws ServerException;
}
