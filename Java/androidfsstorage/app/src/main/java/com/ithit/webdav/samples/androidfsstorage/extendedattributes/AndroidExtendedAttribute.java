package com.ithit.webdav.samples.androidfsstorage.extendedattributes;

import com.ithit.webdav.samples.androidfsstorage.android.IDatabaseHandler;
import com.ithit.webdav.server.exceptions.ServerException;

/**
 * OS X extended attribute support using native API.
 */
class AndroidExtendedAttribute implements ExtendedAttribute {

    private static final String TEST_PROPERTY = "test";
    private IDatabaseHandler databaseHandler;

    AndroidExtendedAttribute(IDatabaseHandler databaseHandler) {
        this.databaseHandler = databaseHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setExtendedAttribute(String path, String attribName, String attribValue) throws ServerException {
        long res = databaseHandler.saveInfo(path, attribName, attribValue);
        if (res < 0) {
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
            return databaseHandler.getInfo(path, attribName);
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
        int result = databaseHandler.deleteInfo(path, attribName);
        if (result == 0) {
            throw new ServerException(
                    String.format("Removing attribute '%s' from file '%s' failed.", attribName, path));
        }
    }

    public boolean isExtendedAttributeSupported(String path) {
        boolean supports = true;
        try {
            setExtendedAttribute(path, TEST_PROPERTY, TEST_PROPERTY);
            deleteExtendedAttribute(path, TEST_PROPERTY);
        } catch (Exception e) {
            supports = false;
        }
        return supports;
    }
}
