package com.ithit.webdav.samples.kotlinfs.extendedattributes

import com.ithit.webdav.server.exceptions.ServerException
import com.sun.jna.platform.mac.XAttrUtil

/**
 * OS X extended attribute support using native API.
 */
internal class OSXExtendedAttribute : ExtendedAttribute {

    /**
     * {@inheritDoc}
     */
    @Throws(ServerException::class)
    override fun setExtendedAttribute(path: String, attribName: String, attribValue: String) {
        val result = XAttrUtil.setXAttr(path, attribName, attribValue)
        if (result == -1) {
            throw ServerException(
                    String.format("Writing attribute '%s' with value '%s' to file '%s' failed.", attribName, attribValue, path))
        }
    }

    /**
     * {@inheritDoc}
     */
    @Throws(ServerException::class)
    override fun getExtendedAttribute(path: String, attribName: String): String {
        try {
            return XAttrUtil.getXAttr(path, attribName)
        } catch (e: Exception) {
            throw ServerException(
                    String.format("Reading attribute '%s' from file '%s' failed.", attribName, path))
        }

    }

    /**
     * {@inheritDoc}
     */
    @Throws(ServerException::class)
    override fun deleteExtendedAttribute(path: String, attribName: String) {
        val result = XAttrUtil.removeXAttr(path, attribName)
        if (result == -1) {
            throw ServerException(
                    String.format("Removing attribute '%s' from file '%s' failed.", attribName, path))
        }
    }
}
