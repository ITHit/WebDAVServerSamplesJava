package com.ithit.webdav.samples.oraclestorageservlet;

import com.ithit.webdav.integration.servlet.HttpServletDavRequest;
import com.ithit.webdav.integration.servlet.HttpServletDavResponse;
import com.ithit.webdav.integration.servlet.HttpServletLoggerImpl;
import com.ithit.webdav.server.Engine;
import com.ithit.webdav.server.Logger;
import com.ithit.webdav.server.MimeType;
import com.ithit.webdav.server.exceptions.DavException;
import com.ithit.webdav.server.exceptions.WebDavStatus;
import com.ithit.webdav.server.util.StringUtil;

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
    private static String realPath;
    private static String servletContext;
    private Logger logger;
    private boolean showExceptions;
    private static final String DEFAULT_INDEX_PATH = "WEB-INF/Index";
    private String license;
    private SearchFacade searchFacade;
    private String resourcePath;

    /**
     * Reads license file content.
     *
     * @param fileName License file location.
     * @return String license content.
     * @throws ServletException in case of any errors.
     */
    private static String getContents(String fileName) throws ServletException {
        StringBuilder contents = new StringBuilder();

        try (BufferedReader input = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = input.readLine()) != null) {
                contents.append(line);
                contents.append(System.getProperty("line.separator"));
            }
        } catch (IOException ignored) {
       }

        return contents.toString();
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
     * Servlet initialization logic. Reads license file here. Creates instance of {@link com.ithit.webdav.server.Engine}.
     *
     * @param servletConfig Config.
     * @throws ServletException if license file not found.
     */
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        String licenseFile = servletConfig.getInitParameter("license");
        showExceptions = "true".equals(servletConfig.getInitParameter("showExceptions"));
        resourcePath = servletConfig.getInitParameter("resources");
        license = getContents(licenseFile);
        realPath = servletConfig.getServletContext().getRealPath("");
        servletContext = servletConfig.getServletContext().getContextPath();
        logger = new HttpServletLoggerImpl(servletConfig.getServletContext());
        WebDavEngine engine = new WebDavEngine(logger, license);
        DataAccess dataAccess = new DataAccess(engine);
        engine.setDataAccess(dataAccess);
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
        CustomFolderGetHandler handler = new CustomFolderGetHandler(engine.getResponseCharacterEncoding(), Engine.getVersion());
        CustomFolderGetHandler handlerHead = new CustomFolderGetHandler(engine.getResponseCharacterEncoding(), Engine.getVersion());
        handler.setPreviousHandler(engine.registerMethodHandler("GET", handler));
        handlerHead.setPreviousHandler(engine.registerMethodHandler("HEAD", handlerHead));

        engine.setServletRequest(davRequest);
        engine.setSearchFacade(searchFacade);
        HttpSession session = httpServletRequest.getSession();
        session.setAttribute("engine", engine);
        DataAccess dataAccess = new DataAccess(engine);
        engine.setDataAccess(dataAccess);

        try {
            engine.service(davRequest, davResponse);
            dataAccess.commit();
        } catch (DavException e) {
            if (e.getStatus() == WebDavStatus.INTERNAL_ERROR) {
                logger.logError("Exception during request processing", e);
                if (showExceptions)
                    e.printStackTrace(new PrintStream(davResponse.getOutputStream()));
            }
        } finally {
            dataAccess.closeConnection();
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
