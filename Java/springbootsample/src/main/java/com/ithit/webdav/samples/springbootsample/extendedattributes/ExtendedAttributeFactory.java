package com.ithit.webdav.samples.springbootsample.extendedattributes;

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
    synchronized static ExtendedAttribute buildFileExtendedAttributeSupport() {
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
