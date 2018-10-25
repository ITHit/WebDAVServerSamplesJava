package com.ithit.webdav.samples.oraclestorageservlet;

import com.ithit.webdav.server.Folder;
import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.MethodHandler;
import com.ithit.webdav.server.exceptions.DavException;
import com.ithit.webdav.server.http.DavRequest;
import com.ithit.webdav.server.http.DavResponse;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This handler processes GET requests to folders returning custom HTML page.
 */
public class CustomFolderGetHandler implements MethodHandler {

    private MethodHandler previousHandler;
    private String charset;
    private String version;
    private String pathToHTML = "WEB-INF/MyCustomHandlerPage.html";

    public CustomFolderGetHandler(String charset, String version) {
        this.charset = charset;
        this.version = version;
    }

    public void processRequest(DavRequest request, DavResponse response, HierarchyItem item)
            throws DavException, IOException {

        if (item instanceof Folder) {
            response.setCharacterEncoding(charset);
            response.setContentType("text/html");
            PrintStream stream = new PrintStream(response.getOutputStream(), true, charset);
            Path path = Paths.get(WebDavServlet.getRealPath(), pathToHTML);
            String context = WebDavServlet.getContext() + "/";
            String wsContext = context.replaceFirst("/", "");
            int ind = wsContext.lastIndexOf("/");
            if (ind >= 0) {
                wsContext = new StringBuilder(wsContext).replace(ind, ind + 1, "\\/").toString();
            }
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String contextRootString = "<%context root%>";
                if (line.contains(contextRootString)) {
                    line = line.replace(contextRootString, context);
                }
                String versionNumber = "<%version%>";
                if (line.contains(versionNumber)) {
                    line = line.replace(versionNumber, version);
                }
                String ws = "<%ws root%>";
                if (line.contains(ws)) {
                    line = line.replace(ws, wsContext);
                }
                String version = "<%startTime%>";
                if (line.contains(version)) {
                    line = line.replace(version, WebDavServlet.START_TIME);
                }
                stream.println(line);
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
    void setPreviousHandler(MethodHandler methodHandler) {
        previousHandler = methodHandler;
    }
}
