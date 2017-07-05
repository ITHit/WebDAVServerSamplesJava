package com.ithit.webdav.samples.fsstorageservlet;

import com.ithit.webdav.server.DefaultLoggerImpl;
import com.ithit.webdav.server.Logger;
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
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;

/**
 * This servlet processes WEBDAV requests.
 */
public class WebDavServlet extends HttpServlet {

    private static final long serialVersionUID = 4668224632937178086L;
    private static final String DEFAULT_ROOT_PATH = "WEB-INF/Storage";
    private static final String DEFAULT_INDEX_PATH = "WEB-INF/Index";
    private static final String TEST_PROPERTY = "test";
    private static String realPath;
    private static String servletContext;
    private static String rootLocalPath = null;
    private static boolean supportsUserDefinedAttributes;
    private Logger logger;
    private boolean showExceptions;
    private WebDavEngine engine;

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
        String license;
        try {
            license = FileUtils.readFileToString(new File(licenseFile));
        } catch (IOException e) {
            throw new ServletException("File not found: " + licenseFile, e);
        }
        logger = new DefaultLoggerImpl(servletConfig.getServletContext());
        realPath = servletConfig.getServletContext().getRealPath("/");
        servletContext = servletConfig.getServletContext().getContextPath();
        rootLocalPath = servletConfig.getInitParameter("root");
        checkRootPath(rootLocalPath);
        String indexLocalPath = createIndexPath();
        supportsUserDefinedAttributes = checkSupportOfUserDefinedFileAttributes();
        engine = new WebDavEngine(logger, license);
        CustomFolderGetHandler handler = new CustomFolderGetHandler(engine.getResponseCharacterEncoding(), engine.getVersion());
        CustomFolderGetHandler handlerHead = new CustomFolderGetHandler(engine.getResponseCharacterEncoding(), engine.getVersion());
        handler.setPreviousHandler(engine.registerMethodHandler("GET", handler));
        handlerHead.setPreviousHandler(engine.registerMethodHandler("HEAD", handlerHead));
        String indexInterval = servletConfig.getInitParameter("index-interval");
        Integer interval = null;
        if (indexInterval != null) {
            try {
                interval = Integer.valueOf(indexInterval);
            } catch (NumberFormatException ignored) {}
        }
        if (rootLocalPath != null && indexLocalPath != null) {
            engine.indexRootFolder(rootLocalPath, indexLocalPath, interval);
        }
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
        return  indexLocalPath.toString();
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
        HttpSession session = httpServletRequest.getSession();
        session.setAttribute("engine", engine);

        engine.setServletRequest(httpServletRequest);

        try {
            engine.service(httpServletRequest, httpServletResponse);
        } catch (DavException e) {
            if (e.getStatus() == WebDavStatus.INTERNAL_ERROR) {
                logger.logError("Exception during request processing", e);
                if (showExceptions)
                    e.printStackTrace(new PrintStream(httpServletResponse.getOutputStream()));
            }
        }
    }

    /**
     * Checks whether file system registered for the root folder supports User Defined Attributes.
     *
     * @return <b>True</b> if the file system registered for the root folder supports User Defined Attributes,
     * <b>false</b> otherwise.
     */
    private boolean checkSupportOfUserDefinedFileAttributes() {
        boolean supports = true;
        try {
            UserDefinedFileAttributeView view = Files.getFileAttributeView(Paths.get(rootLocalPath), UserDefinedFileAttributeView.class);
            if (view != null) {
                ByteBuffer bufferActiveLocks = Charset.defaultCharset().encode(CharBuffer.wrap(TEST_PROPERTY));
                view.write(TEST_PROPERTY, bufferActiveLocks);
                view.delete(TEST_PROPERTY);
            } else {
                supports = false;
            }
        } catch (IOException e) {
            supports = false;
        }
        return supports;
    }
}
