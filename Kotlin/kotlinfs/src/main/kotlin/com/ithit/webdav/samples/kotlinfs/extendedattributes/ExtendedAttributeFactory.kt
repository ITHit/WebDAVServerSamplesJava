package com.ithit.webdav.samples.kotlinfs.extendedattributes

/**
 * Factory-singleton which creates a ExtendedAttribute instance.
 * Instance is valid for the current platform.
 */
internal object ExtendedAttributeFactory {

    private var extendedAttribute: ExtendedAttribute? = null

    /**
     * Builds a specific ExtendedAttribute for the current platform.
     *
     * @return Platform specific instance of ExtendedAttribute.
     */
    @Synchronized
    fun buildFileExtendedAttributeSupport(): ExtendedAttribute? {
        if (extendedAttribute == null) {
            extendedAttribute = if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                OSXExtendedAttribute()
            } else {
                DefaultExtendedAttribute()
            }
        }
        return extendedAttribute
    }

}
