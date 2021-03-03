package com.ithit.webdav.samples.fsstorageservlet

import com.ithit.webdav.integration.servlet.HttpServletDavRequest
import com.ithit.webdav.integration.servlet.HttpServletDavResponse
import com.ithit.webdav.integration.servlet.HttpServletLoggerImpl
import com.ithit.webdav.samples.fsstorageservlet.extendedattributes.ExtendedAttributesExtension
import com.ithit.webdav.server.Engine
import com.ithit.webdav.server.Logger
import com.ithit.webdav.server.exceptions.DavException
import com.ithit.webdav.server.exceptions.WebDavStatus
import com.ithit.webdav.server.util.StringUtil
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

const val DEFAULT_ROOT_PATH = "WEB-INF/Storage"
const val DEFAULT_INDEX_PATH = "WEB-INF/Index"

/**
 * This servlet processes WEBDAV requests.
 */
class WebDavServlet : HttpServlet() {
    private var logger: Logger? = null
    private var showExceptions: Boolean = false
    private var searchFacade: SearchFacade? = null
    private var license: String? = null

    /**
     * Servlet initialization logic. Reads license file here. Creates instance of [com.ithit.webdav.server.Engine].
     *
     * @param servletConfig Config.
     * @throws ServletException if license file not found.
     */
    @Throws(ServletException::class)
    override fun init(servletConfig: ServletConfig) {
        super.init(servletConfig)

        val licenseFile = servletConfig.getInitParameter("license")
        showExceptions = java.lang.Boolean.parseBoolean(servletConfig.getInitParameter("showExceptions"))
        license = try {
            FileUtils.readFileToString(File(licenseFile), StandardCharsets.UTF_8)
        } catch (e: IOException) {
            ""
        }

        logger = HttpServletLoggerImpl(servletConfig.servletContext)
        realPath = servletConfig.servletContext.getRealPath("/")
        context = servletConfig.servletContext.contextPath
        rootLocalPath = servletConfig.getInitParameter("root")
        checkRootPath(rootLocalPath)
        val indexLocalPath = createIndexPath()
        isSupportsUserDefinedAttributes = ExtendedAttributesExtension.isExtendedAttributesSupported(Paths.get(rootLocalPath).toString())
        val engine = WebDavEngine(logger as HttpServletLoggerImpl, license)
        val indexInterval = servletConfig.getInitParameter("index-interval")
        var interval: Int? = null
        if (indexInterval != null) {
            try {
                interval = Integer.valueOf(indexInterval)
            } catch (ignored: NumberFormatException) {
            }
        }
        if (rootLocalPath != null && indexLocalPath != null) {
            searchFacade = SearchFacade(engine, logger as HttpServletLoggerImpl)
            searchFacade!!.indexRootFolder(rootLocalPath!!, indexLocalPath, interval)
        }
    }

    /**
     * Release all resources when stop the servlet
     */
    override fun destroy() {
        searchFacade!!.indexer!!.stop()
    }

    private fun checkRootPath(rootPath: String?) {
        val path = Paths.get(realPath!!, DEFAULT_ROOT_PATH)
        if (StringUtil.isNullOrEmpty(rootPath)) {
            rootLocalPath = path.toString()
        } else {
            if (Files.exists(Paths.get(rootPath))) {
                return
            }
            rootLocalPath = try {
                if (Files.exists(Paths.get(realPath!!, rootPath))) {
                    Paths.get(realPath!!, rootPath).toString()
                } else {
                    path.toString()
                }
            } catch (ignored: Exception) {
                path.toString()
            }
        }
    }

    /**
     * Creates index folder if not exists.
     *
     * @return Absolute location of index folder.
     */
    private fun createIndexPath(): String? {
        val indexLocalPath = Paths.get(realPath!!, DEFAULT_INDEX_PATH)
        if (Files.notExists(indexLocalPath)) {
            try {
                Files.createDirectory(indexLocalPath)
            } catch (e: IOException) {
                return null
            }
        }
        return indexLocalPath.toString()
    }

    /**
     * Sets customs handlers. Gives control to [com.ithit.webdav.server.Engine].
     *
     * @param httpServletRequest  Servlet request.
     * @param httpServletResponse Servlet response.
     * @throws ServletException in case of unexpected exceptions.
     * @throws IOException      in case of read write exceptions.
     */
    @Throws(ServletException::class, IOException::class)
    override fun service(httpServletRequest: HttpServletRequest, httpServletResponse: HttpServletResponse) {
        val engine = WebDavEngine(logger, license)
        val davRequest = HttpServletDavRequest(httpServletRequest)
        val davResponse = HttpServletDavResponse(httpServletResponse)
        val handler = CustomFolderGetHandler(engine.responseCharacterEncoding, Engine.getVersion())
        val handlerHead = CustomFolderGetHandler(engine.responseCharacterEncoding, Engine.getVersion())
        handler.setPreviousHandler(engine.registerMethodHandler("GET", handler))
        handlerHead.setPreviousHandler(engine.registerMethodHandler("HEAD", handlerHead))
        engine.searchFacade = searchFacade

        val session = httpServletRequest.session
        session.setAttribute("engine", engine)
        try {
            engine.service(davRequest, davResponse)
        } catch (e: DavException) {
            if (e.status === WebDavStatus.INTERNAL_ERROR) {
                logger!!.logError("Exception during request processing", e)
                if (showExceptions)
                    e.printStackTrace(PrintStream(davResponse.outputStream))
            }
        }
    }

    companion object {

        /**
         * Return path of servlet location in file system to load resources.
         *
         * @return Real path.
         */
        internal var realPath: String? = null
            private set
        /**
         * Returns servlet URL context path.
         *
         * @return Context path.
         */
        internal var context: String? = null
            private set
        /**
         * Returns root folder for the WebDav.
         *
         * @return Root folder.
         */
        internal var rootLocalPath: String? = null
            private set
        /**
         * Returns whether file system registered for the root folder supports User Defined Attributes.
         *
         * @return **True** if file system registered for root folder support User Defined Attributes,
         * **false** otherwise.
         */
        internal var isSupportsUserDefinedAttributes: Boolean = false
            private set
        internal val START_TIME = "" + System.currentTimeMillis()
    }
}
