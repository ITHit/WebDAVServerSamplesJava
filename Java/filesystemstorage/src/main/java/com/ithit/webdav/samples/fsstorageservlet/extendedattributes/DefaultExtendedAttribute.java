package com.ithit.webdav.samples.fsstorageservlet.extendedattributes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.List;

/**
 * ExtendedAttribute for most platforms using Java's UserDefinedFileAttributeView
 * for extended file attributes.
 */
class DefaultExtendedAttribute implements ExtendedAttribute {

    /**
     * {@inheritDoc}
     */
    @Override
    public void setExtendedAttribute(String path, String attribName, String attribValue) throws IOException {
        UserDefinedFileAttributeView view = Files
                .getFileAttributeView(Paths.get(path), UserDefinedFileAttributeView.class);
        view.write(attribName, Charset.defaultCharset().encode(attribValue));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getExtendedAttribute(String path, String attribName) throws IOException {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(Paths.get(path), UserDefinedFileAttributeView.class);

        List<String> attrNames = view.list();
        for (String existAttrName : attrNames) {
            if (existAttrName.equals(attribName)) {
                ByteBuffer buf = ByteBuffer.allocate(view.size(attribName));
                view.read(attribName, buf);
                buf.flip();
                return Charset.defaultCharset().decode(buf).toString();
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteExtendedAttribute(String path, String attribName) throws IOException {
        UserDefinedFileAttributeView view = Files
                .getFileAttributeView(Paths.get(path), UserDefinedFileAttributeView.class);
        view.delete(attribName);
    }

}
