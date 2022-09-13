package com.ithit.webdav.samples.fsstorageservlet.extendedattributes

import com.ithit.webdav.server.exceptions.ServerException

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Paths
import java.nio.file.attribute.UserDefinedFileAttributeView


/**
 * ExtendedAttribute for most platforms using Java's UserDefinedFileAttributeView
 * for extended file attributes.
 */
internal class DefaultExtendedAttribute : ExtendedAttribute {

    /**
     * {@inheritDoc}
     */
    @Throws(ServerException::class)
    override fun setExtendedAttribute(path: String, attribName: String, attribValue: String) {
        val view = Files
                .getFileAttributeView(Paths.get(path), UserDefinedFileAttributeView::class.java)
        try {
            view.write(attribName, Charset.defaultCharset().encode(attribValue))
        } catch (e: IOException) {
            throw ServerException(String.format("Writing attribute '%s' with value '%s' to file '%s' failed.", attribName, attribValue, path), e)
        }

    }

    /**
     * {@inheritDoc}
     */
    @Throws(ServerException::class)
    override fun getExtendedAttribute(path: String, attribName: String): String? {
        val view = Files
                .getFileAttributeView(Paths.get(path), UserDefinedFileAttributeView::class.java)
        val buf: ByteBuffer
        return try {
            buf = ByteBuffer.allocate(view.size(attribName))
            view.read(attribName, buf)
            buf.flip()
            Charset.defaultCharset().decode(buf).toString()
        } catch (ignored: NoSuchFileException) {
            null
        } catch (e: IOException) {
            throw ServerException(String.format("Reading attribute '%s' from file '%s' failed.", attribName, path), e)
        }
    }

    /**
     * {@inheritDoc}
     */
    @Throws(ServerException::class)
    override fun deleteExtendedAttribute(path: String, attribName: String) {
        val view = Files
                .getFileAttributeView(Paths.get(path), UserDefinedFileAttributeView::class.java)
        try {
            view.delete(attribName)
        } catch (e: IOException) {
            throw ServerException(String.format("Deleting attribute '%s' from file '%s' failed.", attribName, path), e)
        }

    }
}
