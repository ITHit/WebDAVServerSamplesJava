package com.ithit.webdav.samples.fsstorageservlet.extendedattributes;

import com.ithit.webdav.server.exceptions.ServerException;
import com.sun.jna.platform.mac.XAttrUtil;

/**
 * OS X extended attribute support using native API.
 */
class OSXExtendedAttribute implements ExtendedAttribute {

    /**
     * {@inheritDoc}
     */
    @Override
    public void setExtendedAttribute(String path, String attribName, String attribValue) throws ServerException {
        int result = XAttrUtil.setXAttr(path, attribName, attribValue);
        if (result == -1) {
            throw new ServerException(
                    String.format("Writing attribute '%s' with value '%s' to file '%s' failed.", attribName, attribValue, path));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getExtendedAttribute(String path, String attribName) throws ServerException {
        try {
            return XAttrUtil.getXAttr(path, attribName);
        } catch (Exception e) {
            throw new ServerException(
                    String.format("Reading attribute '%s' from file '%s' failed.", attribName, path));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteExtendedAttribute(String path, String attribName) throws ServerException {
        int result = XAttrUtil.removeXAttr(path, attribName);
        if (result == -1) {
            throw new ServerException(
                    String.format("Removing attribute '%s' from file '%s' failed.", attribName, path));
        }
    }
}
