package com.ithit.webdav.samples.fsstorageservlet;

import com.ithit.webdav.integration.servlet.HttpDavLoggingContext;
import com.ithit.webdav.integration.servlet.HttpServletDavRequest;
import com.ithit.webdav.integration.servlet.HttpServletDavResponse;
import com.ithit.webdav.samples.fsstorageservlet.extendedattributes.ExtendedAttributesExtension;
import com.ithit.webdav.server.DefaultLoggerImpl;
import com.ithit.webdav.server.Logger;
import com.ithit.webdav.server.MimeType;
import com.ithit.webdav.server.exceptions.DavException;
import com.ithit.webdav.server.exceptions.WebDavStatus;
import com.ithit.webdav.server.util.StringUtil;
import org.apache.commons.io.FileUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * This servlet processes WEBDAV requests.
 */
public class WebDavServlet extends HttpServlet {

    private static final long serialVersionUID = 4668224632937178086L;
    private static final String DEFAULT_ROOT_PATH = "WEB-INF/Storage";
    private static final String DEFAULT_INDEX_PATH = "WEB-INF/Index";
    private static String realPath;
    private static String servletContext;
    private static String rootLocalPath = null;
    private static boolean supportsUserDefinedAttributes;
    private Logger logger;
    private boolean showExceptions;
    private SearchFacade searchFacade;
    private String license;
    private String resourcePath;

    /**
     * Returns root folder for the WebDav.
     *
     * @return Root folder.
     */
    static String getRootLocalPath() {
        return rootLocalPath;
    }

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
     * Returns whether file system registered for the root folder supports User Defined Attributes.
     *
     * @return <b>True</b> if file system registered for root folder support User Defined Attributes,
     * <b>false</b> otherwise.
     */
    static boolean isSupportsUserDefinedAttributes() {
        return supportsUserDefinedAttributes;
    }

    /**
     * Servlet initialization logic. Reads license file here. Creates instance of {@link com.ithit.webdav.server.Engine}.
     *
     * @param servletConfig Config.
     * @throws ServletException if license file not found.
     */
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        String licenseFile = servletConfig.getInitParameter("license");
        showExceptions = Boolean.parseBoolean(servletConfig.getInitParameter("showExceptions"));
        try {
            license = FileUtils.readFileToString(new File(licenseFile));
        } catch (IOException e) {
            license = "";
        }
        logger = new DefaultLoggerImpl(new HttpDavLoggingContext(servletConfig.getServletContext()));
        realPath = servletConfig.getServletContext().getRealPath("/");
        servletContext = servletConfig.getServletContext().getContextPath();
        rootLocalPath = servletConfig.getInitParameter("root");
        resourcePath = servletConfig.getInitParameter("resources");
        checkRootPath(rootLocalPath);
        String indexLocalPath = createIndexPath();
        supportsUserDefinedAttributes = ExtendedAttributesExtension.isExtendedAttributesSupported(Paths.get(getRootLocalPath()).toString());
        WebDavEngine engine = new WebDavEngine(logger, license);
        String indexInterval = servletConfig.getInitParameter("index-interval");
        Integer interval = null;
        if (indexInterval != null) {
            try {
                interval = Integer.valueOf(indexInterval);
            } catch (NumberFormatException ignored) {}
        }
        if (rootLocalPath != null && indexLocalPath != null) {
            searchFacade = new SearchFacade(engine, logger);
            searchFacade.indexRootFolder(rootLocalPath, indexLocalPath, interval);
        }
    }

    /**
     * Release all resources when stop the servlet
     */
    @Override
    public void destroy() {
        searchFacade.getIndexer().stop();
    }

    private void checkRootPath(String rootPath) {
        Path path = Paths.get(realPath, DEFAULT_ROOT_PATH);
        if (StringUtil.isNullOrEmpty(rootPath)) {
            rootLocalPath = path.toString();
        } else {
            if (Files.exists(Paths.get(rootPath))) {
                return;
            }
            try {
                if (Files.exists(Paths.get(realPath, rootPath))) {
                    rootLocalPath = Paths.get(realPath, rootPath).toString();
                } else {
                    rootLocalPath = path.toString();
                }
            } catch (Exception ignored) {
                rootLocalPath = path.toString();
            }
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

    /**
     * Sets customs handlers. Gives control to {@link com.ithit.webdav.server.Engine}.
     *
     * @param httpServletRequest  Servlet request.
     * @param httpServletResponse Servlet response.
     * @throws ServletException in case of unexpected exceptions.
     * @throws IOException      in case of read write exceptions.
     */
    @Override
    protected void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws ServletException, IOException {
        WebDavEngine engine = new WebDavEngine(logger, license);
        if (!StringUtil.isNullOrEmpty(resourcePath)) {
            if (StringUtil.trimStart(httpServletRequest.getRequestURI(), "/").startsWith(StringUtil.trimStart(resourcePath, "/"))) {
                processResources(httpServletRequest, httpServletResponse, engine.getResponseCharacterEncoding());
            }
        }
        HttpServletDavRequest davRequest = new HttpServletDavRequest(httpServletRequest);
        HttpServletDavResponse davResponse = new HttpServletDavResponse(httpServletResponse);
        CustomFolderGetHandler handler = new CustomFolderGetHandler(engine.getResponseCharacterEncoding(), engine.getVersion());
        CustomFolderGetHandler handlerHead = new CustomFolderGetHandler(engine.getResponseCharacterEncoding(), engine.getVersion());
        handler.setPreviousHandler(engine.registerMethodHandler("GET", handler));
        handlerHead.setPreviousHandler(engine.registerMethodHandler("HEAD", handlerHead));
        engine.setServletRequest(davRequest);
        engine.setSearchFacade(searchFacade);

        HttpSession session = httpServletRequest.getSession();
        session.setAttribute("engine", engine);
        try {
            engine.service(davRequest, davResponse);
        } catch (DavException e) {
            if (e.getStatus() == WebDavStatus.INTERNAL_ERROR) {
                logger.logError("Exception during request processing", e);
                if (showExceptions)
                    e.printStackTrace(new PrintStream(davResponse.getOutputStream()));
            }
        }
    }

    private void processResources(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, String  charset) throws IOException {
        Path path = Paths.get(WebDavServlet.getRealPath(), "WEB-INF", httpServletRequest.getRequestURI());
        try( OutputStream out = httpServletResponse.getOutputStream()) {
            if (!httpServletRequest.getRequestURI().contains("Plugins")) {
                PrintStream stream = new PrintStream(out, true, charset);
                httpServletResponse.setCharacterEncoding(charset);
                httpServletResponse.setContentType(MimeType.getInstance().getMimeType(getFileExtension(path.toFile())));
                List<String> lines = Files.readAllLines(path, Charset.defaultCharset());
                for (String s : lines) {
                    stream.println(s);
                }
            } else {
                httpServletResponse.setContentType("application/octet-stream");
                String fileName = path.getFileName().toString();
                httpServletResponse.setHeader("Content-disposition", "attachment; filename=" + fileName);
                File file = path.toFile();
                httpServletResponse.setContentLength((int) file.length());
                try (FileInputStream in = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }
            }
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        try {
            return name.substring(name.lastIndexOf(".") + 1);
        } catch (Exception e) {
            return "";
        }
    }
}
