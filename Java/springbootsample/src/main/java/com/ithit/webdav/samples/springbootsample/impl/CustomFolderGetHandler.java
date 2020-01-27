package com.ithit.webdav.samples.springbootsample.impl;

import com.ithit.webdav.server.Folder;
import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.MethodHandler;
import com.ithit.webdav.server.exceptions.DavException;
import com.ithit.webdav.server.http.DavRequest;
import com.ithit.webdav.server.http.DavResponse;

import java.io.IOException;
import java.io.PrintStream;

/**
 * This handler processes GET requests to folders returning custom HTML page.
 */
public class CustomFolderGetHandler implements MethodHandler {

    private MethodHandler previousHandler;
    private String charset;
    private String version;
    private boolean customAttributeSupported;
    private String customPage;
    private String errorPage;

    public CustomFolderGetHandler(String charset, String version, boolean customAttributeSupported, String customPage, String errorPage) {
        this.charset = charset;
        this.version = version;
        this.customAttributeSupported = customAttributeSupported;
        this.customPage = customPage;
        this.errorPage = errorPage;
    }

    @Override
    public void processRequest(DavRequest request, DavResponse response, HierarchyItem item)
            throws DavException, IOException {
        if (item instanceof Folder) {
            PrintStream stream = new PrintStream(response.getOutputStream(), true, charset);
            response.setCharacterEncoding(charset);
            response.setContentType("text/html");
            if (!customAttributeSupported) {
                stream.println(errorPage);
            } else {
                String versionNumber = "<%version%>";
                if (customPage.contains(versionNumber)) {
                    customPage = customPage.replace(versionNumber, version);
                }
                String version = "<%startTime%>";
                if (customPage.contains(version)) {
                    customPage = customPage.replace(version, "" + System.currentTimeMillis());
                }
                stream.println(customPage);
            }
            stream.flush();
        } else {
            previousHandler.processRequest(request, response, item);
        }
    }

    /**
     * Determines whether request body shall be logged.
     *
     * @return {@code true} if request body shall be logged.
     */
    public boolean getLogInput() {
        return false;
    }

    /**
     * Determines whether response body shall be logged.
     *
     * @return {@code true} if response body shall be logged.
     */
    public boolean getLogOutput() {
        return false;
    }

    /**
     * Determines whether response content length shall be calculated by engine.
     *
     * @return {@code true} if content length shall be calculated by engine.
     */
    public boolean getCalculateContentLength() {
        return false;
    }

    /**
     * Set previous handler fo GET operation.
     *
     * @param methodHandler previous handler.
     */
    public void setPreviousHandler(MethodHandler methodHandler) {
        previousHandler = methodHandler;
    }
}
