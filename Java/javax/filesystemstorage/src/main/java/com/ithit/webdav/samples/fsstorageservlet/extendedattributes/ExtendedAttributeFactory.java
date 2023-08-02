package com.ithit.webdav.samples.fsstorageservlet.extendedattributes;

/**
 * Factory-singleton which creates a ExtendedAttribute instance.
 * Instance is valid for the current platform.
 */
final class ExtendedAttributeFactory {

    private ExtendedAttributeFactory() {
    }

    private static ExtendedAttribute extendedAttribute;

    /**
     * Builds a specific ExtendedAttribute for the current platform.
     *
     * @return Platform specific instance of ExtendedAttribute.
     */
    static synchronized ExtendedAttribute buildFileExtendedAttributeSupport() {
        if (extendedAttribute == null) {
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                extendedAttribute = new OSXExtendedAttribute();
            } else {
                extendedAttribute = new DefaultExtendedAttribute();
            }
        }
        return extendedAttribute;
    }

}
