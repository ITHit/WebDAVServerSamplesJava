package com.ithit.webdav.samples.springbootfs.extendedattributes;

import com.sun.jna.platform.mac.XAttrUtil;

import java.io.IOException;

/**
 * OS X extended attribute support using native API.
 */
class OSXExtendedAttribute implements ExtendedAttribute {

    /**
     * {@inheritDoc}
     */
    @Override
    public void setExtendedAttribute(String path, String attribName, String attribValue) throws IOException {
        int result = XAttrUtil.setXAttr(path, attribName, attribValue);
        if (result == -1) {
            throw new IOException(
                    String.format("Writing attribute '%s' with value '%s' to file '%s' failed.", attribName, attribValue, path));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getExtendedAttribute(String path, String attribName) throws IOException {
        try {
            return XAttrUtil.getXAttr(path, attribName);
        } catch (Exception e) {
            throw new IOException(
                    String.format("Reading attribute '%s' from file '%s' failed.", attribName, path));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteExtendedAttribute(String path, String attribName) throws IOException {
        int result = XAttrUtil.removeXAttr(path, attribName);
        if (result == -1) {
            throw new IOException(
                    String.format("Removing attribute '%s' from file '%s' failed.", attribName, path));
        }
    }
}
