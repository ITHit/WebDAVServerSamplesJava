package com.ithit.webdav.samples.fsstorageservlet.extendedattributes;

import com.ithit.webdav.server.exceptions.ServerException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;


/**
 * ExtendedAttribute for most platforms using Java's UserDefinedFileAttributeView
 * for extended file attributes.
 */
class DefaultExtendedAttribute implements ExtendedAttribute {

    /**
     * {@inheritDoc}
     */
    @Override
    public void setExtendedAttribute(String path, String attribName, String attribValue) throws ServerException {
        UserDefinedFileAttributeView view = Files
                .getFileAttributeView(Paths.get(path), UserDefinedFileAttributeView.class);
        try {
            view.write(attribName, Charset.defaultCharset().encode(attribValue));
        } catch (IOException e) {
            throw new ServerException(String.format("Writing attribute '%s' with value '%s' to file '%s' failed.", attribName, attribValue, path), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getExtendedAttribute(String path, String attribName) throws ServerException {
        UserDefinedFileAttributeView view = Files
                .getFileAttributeView(Paths.get(path), UserDefinedFileAttributeView.class);
        ByteBuffer buf;
        try {
            if (view.list().isEmpty()) {
                return null;
            }

            buf = ByteBuffer.allocate(view.size(attribName));
            view.read(attribName, buf);
            buf.flip();
            return Charset.defaultCharset().decode(buf).toString();
        } catch (FileSystemException ignore) {
        } catch (IOException e) {
            throw new ServerException(String.format("Reading attribute '%s' from file '%s' failed.", attribName, path), e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteExtendedAttribute(String path, String attribName) throws ServerException {
        UserDefinedFileAttributeView view = Files
                .getFileAttributeView(Paths.get(path), UserDefinedFileAttributeView.class);
        try {
            view.delete(attribName);
        } catch (IOException e) {
            throw new ServerException(String.format("Deleting attribute '%s' from file '%s' failed.", attribName, path), e);
        }
    }
}
