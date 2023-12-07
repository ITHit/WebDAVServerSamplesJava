package com.ithit.webdav.samples.deltavservlet;

import com.ithit.webdav.integration.servlet.DavServletConfig;
import com.ithit.webdav.integration.servlet.HttpServletDav;
import com.ithit.webdav.integration.servlet.HttpServletDavException;
import com.ithit.webdav.integration.servlet.HttpServletDavRequest;
import com.ithit.webdav.integration.servlet.HttpServletDavResponse;
import com.ithit.webdav.integration.servlet.HttpServletLoggerImpl;
import com.ithit.webdav.server.Engine;
import com.ithit.webdav.server.Logger;
import com.ithit.webdav.server.deltav.AutoVersion;
import com.ithit.webdav.server.exceptions.DavException;
import com.ithit.webdav.server.exceptions.WebDavStatus;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This servlet processes WEBDAV requests.
 */
public class WebDavServlet extends HttpServletDav {
    private static String realPath;
    private static String servletContext;
    private Logger logger;
    private String license;
    private boolean showExceptions;
    private AutoVersion autoVersionMode;
    private boolean autoputUnderVersionControl;
    private static final String DEFAULT_INDEX_PATH = "WEB-INF/Index";
    private SearchFacade searchFacade;
    static final String START_TIME = "" + System.currentTimeMillis();

    /**
     * Return path of servlet location in file system to load resources.
     *
     * @return Real path.
     */
    static String getRealPath() {
        return realPath;
    }

    /**
     * Returns servlet URL context path.
     *
     * @return Context path.
     */
    static String getContext() {
        return servletContext;
    }

    /**
     * Servlet initialization logic. Reads license file here. Creates instance of {@link com.ithit.webdav.server.Engine}.
     *
     * @param servletConfig Config.
     * @throws HttpServletDavException if license file not found.
     */
    @Override
    public void initDav(DavServletConfig servletConfig) throws HttpServletDavException {
        logger = new HttpServletLoggerImpl(servletConfig.getServletContext());

        String licenseFile = servletConfig.getInitParameter("license");
        showExceptions = "true".equals(servletConfig.getInitParameter("showExceptions"));
        try {
            autoVersionMode = AutoVersion.valueOf(servletConfig.getInitParameter("autoVersionMode"));
        } catch (IllegalArgumentException e) {
            autoVersionMode = AutoVersion.NoAutoVersioning;
            logger.logError("Failed to parse autoversion parameter. Defaulting to NoAutoVersioning.", e);
        }
        autoputUnderVersionControl = "true".equals(servletConfig.getInitParameter("autoPutUnderVersionControl"));
        realPath = servletConfig.getServletContext().getRealPath("");
        servletContext = servletConfig.getServletContext().getContextPath();
        try {
            license = FileUtils.readFileToString(new File(licenseFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            license = "";
        }
        WebDavEngine engine = new WebDavEngine(logger, license);
        DataAccess dataAccess = new DataAccess(engine);
        String indexLocalPath = createIndexPath();
        String indexInterval = servletConfig.getInitParameter("index-interval");
        Integer interval = null;
        if (indexInterval != null) {
            try {
                interval = Integer.valueOf(indexInterval);
            } catch (NumberFormatException ignored) {}
        }
        searchFacade = new SearchFacade(dataAccess, logger);
        searchFacade.indexRootFolder(indexLocalPath, interval);
        dataAccess.closeConnection();
    }

    /**
     * Release all resources when stop the servlet
     */
    @Override
    public void destroy() {
        searchFacade.getIndexer().stop();
    }

    /**
     * Sets customs handlers. Gives control to {@link com.ithit.webdav.server.Engine}.
     *
     * @param httpServletRequest  Servlet request.
     * @param httpServletResponse Servlet response.
     * @throws IOException      in case of read write exceptions.
     */
    @Override
    protected void serviceDav(HttpServletDavRequest httpServletRequest, HttpServletDavResponse httpServletResponse)
            throws IOException {

        WebDavEngine engine = new WebDavEngine(logger, license);
        CustomFolderGetHandler handler = new CustomFolderGetHandler(engine.getResponseCharacterEncoding(), Engine.getVersion());
        CustomFolderGetHandler handlerHead = new CustomFolderGetHandler(engine.getResponseCharacterEncoding(), Engine.getVersion());
        handler.setPreviousHandler(engine.registerMethodHandler("GET", handler));
        handlerHead.setPreviousHandler(engine.registerMethodHandler("HEAD", handlerHead));

        engine.setAutoPutUnderVersionControl(autoputUnderVersionControl);
        engine.setAutoVersionMode(autoVersionMode);
        engine.setSearchFacade(searchFacade);
        DataAccess dataAccess = new DataAccess(engine);
        try {
            engine.service(httpServletRequest, httpServletResponse);
            dataAccess.commit();
        } catch (DavException e) {
            dataAccess.rollback();
            if (e.getStatus() == WebDavStatus.INTERNAL_ERROR) {
                logger.logError("Exception during request processing", e);
                if (showExceptions)
                    e.printStackTrace(new PrintWriter(httpServletResponse.getOutputStream()));
            }
        } finally {
            dataAccess.closeConnection();
        }
    }

    /**
     * Creates index folder if not exists.
     *
     * @return Absolute location of index folder.
     */
    private String createIndexPath() {
        Path indexLocalPath = Paths.get(realPath, DEFAULT_INDEX_PATH);
        if (Files.notExists(indexLocalPath)) {
            try {
                Files.createDirectory(indexLocalPath);
            } catch (IOException e) {
                return null;
            }
        }
        return indexLocalPath.toString();
    }
}
