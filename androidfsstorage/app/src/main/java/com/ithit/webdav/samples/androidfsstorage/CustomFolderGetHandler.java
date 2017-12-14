package com.ithit.webdav.samples.androidfsstorage;

import com.ithit.webdav.server.Folder;
import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.MethodHandler;
import com.ithit.webdav.server.exceptions.DavException;
import com.ithit.webdav.server.http.DavRequest;
import com.ithit.webdav.server.http.DavResponse;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * This handler processes GET requests to folders returning custom HTML page.
 */
public class CustomFolderGetHandler implements MethodHandler {

    private MethodHandler previousHandler;
    private String charset;
    private String version;
    private List<String> mainPage;
    private List<String> errorPage;
    private List<String> testPage;
    private List<String> browserPage;

    public CustomFolderGetHandler(String charset, String version, List<String> mainPage, List<String> errorPage, List<String> testPage, List<String> browserPage) {
        this.charset = charset;
        this.version = version;
        this.mainPage = mainPage;
        this.errorPage = errorPage;
        this.testPage = testPage;
        this.browserPage = browserPage;
    }

    @Override
    public void processRequest(DavRequest request, DavResponse response, HierarchyItem item)
            throws DavException, IOException {
        if (item instanceof Folder) {
            PrintStream stream = new PrintStream(response.getOutputStream(), true, charset);
            response.setCharacterEncoding(charset);
            response.setContentType("text/html");
            if (!AndroidWebDavServer.isSupportsUserDefinedAttributes()) {
                for (String line: errorPage) {
                    stream.println(line);
                }
            } else {
                for (String line : mainPage) {
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
                PrintStream stream = new PrintStream(response.getOutputStream(), true, charset);
                response.setCharacterEncoding(charset);
                response.setContentType("text/html");
                for (String line: testPage) {
                    stream.println(line);
                }
                stream.flush();
            } else if (request.getRequestURI().contains("wwwroot/AjaxFileBrowser.html")) {
                PrintStream stream = new PrintStream(response.getOutputStream(), true, charset);
                response.setCharacterEncoding(charset);
                response.setContentType("text/html");
                for (String line: browserPage) {
                    stream.println(line);
                }
                stream.flush();
            } else {
                previousHandler.processRequest(request, response, item);
            }
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
    void setPreviousHandler(MethodHandler methodHandler) {
        previousHandler = methodHandler;
    }
}
