package com.ithit.webdav.samples.androidfsstorage.extendedattributes;

import com.ithit.webdav.samples.androidfsstorage.android.IDatabaseHandler;

/**
 * Factory-singleton which creates a ExtendedAttribute instance.
 * Instance is valid for the current platform.
 */
class ExtendedAttributeFactory {

    private ExtendedAttributeFactory() {
    }

    private static ExtendedAttribute extendedAttribute = null;

    /**
     * Builds a specific ExtendedAttribute for the current platform.
     *
     * @return Platform specific instance of ExtendedAttribute.
     */
    synchronized static ExtendedAttribute buildFileExtendedAttributeSupport(IDatabaseHandler databaseHandler) {
        if (extendedAttribute == null) {
                extendedAttribute = new AndroidExtendedAttribute(databaseHandler);
        }
        return extendedAttribute;
    }

}
