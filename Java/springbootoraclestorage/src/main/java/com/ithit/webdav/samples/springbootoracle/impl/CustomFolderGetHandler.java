package com.ithit.webdav.samples.springbootoracle.impl;

import com.ithit.webdav.server.Folder;
import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.MethodHandler;
import com.ithit.webdav.server.exceptions.DavException;
import com.ithit.webdav.server.DavRequest;
import com.ithit.webdav.server.DavResponse;

import java.io.IOException;
import java.io.PrintStream;

/**
 * This handler processes GET requests to folders returning custom HTML page.
 */
public class CustomFolderGetHandler implements MethodHandler {

    private MethodHandler previousHandler;
    private final String charset;
    private final String version;
    private String customPage;
    private final String rootContext;
    private final String rootWebSocket;

    public CustomFolderGetHandler(String charset, String version, String customPage, String rootContext, String rootWebSocket) {
        this.charset = charset;
        this.version = version;
        this.customPage = customPage;
        this.rootContext = rootContext;
        this.rootWebSocket = rootWebSocket;
    }

    @Override
    public void processRequest(DavRequest request, DavResponse response, HierarchyItem item)
            throws DavException, IOException {
        if (item instanceof Folder) {
            PrintStream stream = new PrintStream(response.getOutputStream(), true, charset);
            response.setCharacterEncoding(charset);
            response.setContentType("text/html");
            String versionNumber = "<%version%>";
            if (customPage.contains(versionNumber)) {
                customPage = customPage.replace(versionNumber, version);
            }
            String contextRoot = "<%context root%>";
            if (customPage.contains(contextRoot)) {
                customPage = customPage.replace(contextRoot, rootContext);
            }
            String wsRoot = "<%ws root%>";
            if (customPage.contains(wsRoot)) {
                customPage = customPage.replace(wsRoot, rootWebSocket);
            }
            String startTime = "<%startTime%>";
            if (customPage.contains(startTime)) {
                customPage = customPage.replace(startTime, "" + System.currentTimeMillis());
            }
            stream.println(customPage);
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
