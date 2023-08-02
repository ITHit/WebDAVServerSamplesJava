package com.ithit.webdav.samples.fsstorageservlet.extendedattributes

import com.ithit.webdav.server.exceptions.ServerException

/**
 * Provides support for reading, writing and removing of extended attributes.
 */
interface ExtendedAttribute {

    /**
     * Determines whether extended attributes are supported.
     *
     * @param path File or folder path to check attribute support.
     * @return True if extended attributes are supported, false otherwise.
     */
    fun isExtendedAttributeSupported(path: String): Boolean {
        var supports = true
        try {
            setExtendedAttribute(path, TEST_PROPERTY, TEST_PROPERTY)
            deleteExtendedAttribute(path, TEST_PROPERTY)
        } catch (e: Exception) {
            supports = false
        }

        return supports
    }

    /**
     * Write the extended attribute to the file.
     *
     * @param path        File or folder path to write attribute.
     * @param attribName  Attribute name.
     * @param attribValue Attribute value.
     * @throws ServerException Throw when file or attribute is not available.
     */
    @Throws(ServerException::class)
    fun setExtendedAttribute(path: String, attribName: String, attribValue: String)

    /**
     * Reads extended attribute.
     *
     * @param path       File or folder path to read extended attribute.
     * @param attribName Attribute name.
     * @return Attribute value.
     * @throws ServerException Throw when file or attribute is no available.
     */
    @Throws(ServerException::class)
    fun getExtendedAttribute(path: String, attribName: String): String?


    /**
     * Deletes extended attribute.
     *
     * @param path       File or folder path to remove extended attribute.
     * @param attribName Attribute name.
     * @throws ServerException Throw when file or attribute is no available.
     */
    @Throws(ServerException::class)
    fun deleteExtendedAttribute(path: String, attribName: String)

    companion object {

        const val TEST_PROPERTY = "test"
    }
}
