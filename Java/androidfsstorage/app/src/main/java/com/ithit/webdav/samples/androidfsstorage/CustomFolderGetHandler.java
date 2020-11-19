package com.ithit.webdav.samples.androidfsstorage;

import com.ithit.webdav.server.Folder;
import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.MethodHandler;
import com.ithit.webdav.server.exceptions.DavException;
import com.ithit.webdav.server.DavRequest;
import com.ithit.webdav.server.DavResponse;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * This handler processes GET requests to folders returning custom HTML page.
 */
public class CustomFolderGetHandler implements MethodHandler {

    private final Config config;
    private MethodHandler previousHandler;
    private String charset;
    private String version;

    public CustomFolderGetHandler(String charset, String version, Config config) {
        this.charset = charset;
        this.version = version;
        this.config = config;
    }

    @Override
    public void processRequest(DavRequest request, DavResponse response, HierarchyItem item)
            throws DavException, IOException {
        if (item instanceof Folder) {
            PrintStream stream = new PrintStream(response.getOutputStream(), true, charset);
            response.setCharacterEncoding(charset);
            response.setContentType("text/html");
            if (!AndroidWebDavServer.isSupportsUserDefinedAttributes()) {
                for (String line: config.getErrorPage()) {
                    stream.println(line);
                }
            } else {
                for (String line : config.getMainPage()) {
                    String versionNumber = "<%version%>";
                    if (line.contains(versionNumber)) {
                        line = line.replace(versionNumber, version);
                    }
                    stream.println(line);
                }
            }
            stream.flush();
        } else {
            if (request.getRequestURI().contains("wwwroot/AjaxIntegrationTests.html")) {
                processTextResource(response, "text/html", config.getTestPage());
            } else if (request.getRequestURI().contains("wwwroot/AjaxFileBrowser.html")) {
                processTextResource(response, "text/html", config.getBrowserPage());
            } else if (request.getRequestURI().contains("wwwroot/css/webdav-layout.css")) {
                processTextResource(response, "text/css", config.getCss());
            } else if (request.getRequestURI().contains("wwwroot/js/webdav-gridview.js")) {
                processTextResource(response, "application/javascript", config.getJsGrid());
            } else if (request.getRequestURI().contains("wwwroot/js/webdav-uploader.js")) {
                processTextResource(response, "application/javascript", config.getJsUploader());
            } else if (request.getRequestURI().contains("wwwroot/js/webdav-websocket.js")) {
                processTextResource(response, "application/javascript", config.getJsWebSocket());
            } else if (request.getRequestURI().contains("wwwroot/js/node_modules/webdav.client/ITHitWebDAVClient.js")) {
                processTextResource(response, "application/javascript", config.getJsClient());
            } else if (request.getRequestURI().contains("wwwroot/js/node_modules/webdav.client/Plugins/ITHitEditDocumentOpener.msi")) {
                processBinaryResource(response, config.getWindowsOpener());
            } else if (request.getRequestURI().contains("wwwroot/js/node_modules/webdav.client/Plugins/ITHitEditDocumentOpener.deb")) {
                processBinaryResource(response, config.getDepOpener());
            } else if (request.getRequestURI().contains("wwwroot/js/node_modules/webdav.client/Plugins/ITHitEditDocumentOpener.pkg")) {
                processBinaryResource(response, config.getPkgOpener());
            } else if (request.getRequestURI().contains("wwwroot/js/node_modules/webdav.client/Plugins/ITHitEditDocumentOpener.rpm")) {
                processBinaryResource(response, config.getRpmOpener());
            } else {
                previousHandler.processRequest(request, response, item);
            }
        }
    }

    private void processBinaryResource(DavResponse response, byte[] resource) throws IOException {
        OutputStream os = response.getOutputStream();
        response.setContentType("application/octet-stream");
        response.setContentLength(resource.length);
        IOUtils.write(resource, os);
    }

    private void processTextResource(DavResponse response, String contentType, List<String> resourceLines) throws IOException {
        PrintStream stream = new PrintStream(response.getOutputStream(), true, charset);
        response.setCharacterEncoding(charset);
        response.setContentType(contentType);
        for (String line : resourceLines) {
            stream.println(line);
        }
        stream.flush();
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
    void setPreviousHandler(MethodHandler methodHandler) {
        previousHandler = methodHandler;
    }
}
