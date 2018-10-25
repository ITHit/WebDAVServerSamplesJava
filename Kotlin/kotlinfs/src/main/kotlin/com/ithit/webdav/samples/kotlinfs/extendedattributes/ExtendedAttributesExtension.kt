package com.ithit.webdav.samples.kotlinfs.extendedattributes

import com.ithit.webdav.server.exceptions.ServerException

/**
 * Helper extension methods for custom attributes.
 */
object ExtendedAttributesExtension {

    private val extendedAttributeSupport: ExtendedAttribute
        get() = ExtendedAttributeFactory.buildFileExtendedAttributeSupport()!!

    /**
     * Gets extended attribute or null if attribute or file not found.
     *
     * @param path       File or folder path to get extended attributes.
     * @param attribName Attribute name.
     * @return Attribute value or null if attribute or file not found.
     * @throws ServerException Throw when file or attribute is no available.
     */
    @Throws(ServerException::class)
    fun getExtendedAttribute(path: String, attribName: String): String? {
        return extendedAttributeSupport.getExtendedAttribute(path, attribName)
    }

    /**
     * Sets extended attribute.
     *
     * @param path        File or folder path to set extended attributes.
     * @param attribName  Attribute name.
     * @param attribValue Attribute value.
     * @throws ServerException Throw when file or attribute is no available.
     */
    @Throws(ServerException::class)
    fun setExtendedAttribute(path: String, attribName: String, attribValue: String) {
        extendedAttributeSupport.setExtendedAttribute(path, attribName, attribValue)
    }

    /**
     * Checks extended attribute existence.
     *
     * @param path       File or folder path to look for extended attributes.
     * @param attribName Attribute name.
     * @return True if attribute exist, false otherwise.
     * @throws ServerException Throw when file or attribute is no available.
     */
    @Throws(ServerException::class)
    fun hasExtendedAttribute(path: String, attribName: String): Boolean {
        return extendedAttributeSupport.getExtendedAttribute(path, attribName) != null
    }

    /**
     * Deletes extended attribute.
     *
     * @param path       File or folder path to delete extended attributes.
     * @param attribName Attribute name.
     * @throws ServerException Throw when file or attribute is no available.
     */
    @Throws(ServerException::class)
    fun deleteExtendedAttribute(path: String, attribName: String) {
        extendedAttributeSupport.deleteExtendedAttribute(path, attribName)
    }

    /**
     * Determines whether extended attributes are supported.
     *
     * @param path File or folder path to check extended attributes support.
     * @return True if extended attributes or NTFS file alternative streams are supported, false otherwise.
     */
    fun isExtendedAttributesSupported(path: String): Boolean {
        return extendedAttributeSupport.isExtendedAttributeSupported(path)
    }
}
