package com.ithit.webdav.samples.fsstorageservlet

import com.ithit.webdav.server.Folder
import com.ithit.webdav.server.HierarchyItem
import com.ithit.webdav.server.MethodHandler
import com.ithit.webdav.server.exceptions.DavException
import com.ithit.webdav.server.http.DavRequest
import com.ithit.webdav.server.http.DavResponse
import org.apache.commons.io.FileUtils
import java.io.IOException
import java.io.PrintStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

/**
 * This handler processes GET requests to folders returning custom HTML page.
 */
class CustomFolderGetHandler(private val charset: String, private val version: String) : MethodHandler {

    private var previousHandler: MethodHandler? = null
    private val pathToHTML = "WEB-INF/MyCustomHandlerPage.html"
    private val pathToErrorHTML = "WEB-INF/attributesErrorPage.html"

    @Throws(DavException::class, IOException::class)
    override fun processRequest(request: DavRequest, response: DavResponse, item: HierarchyItem?) {
        if (item is Folder) {
            val stream = PrintStream(response.outputStream, true, charset)
            response.setCharacterEncoding(charset)
            response.setContentType("text/html")
            if (!WebDavServlet.isSupportsUserDefinedAttributes) {
                val path = Paths.get(WebDavServlet.realPath!!, pathToErrorHTML)
                val lines = FileUtils.readFileToString(path.toFile(), Charset.defaultCharset())
                stream.println(lines)
            } else {
                val path = Paths.get(WebDavServlet.realPath!!, pathToHTML)
                val context = WebDavServlet.context!! + "/"
                var wsContext = context.replaceFirst("/".toRegex(), "")
                val ind = wsContext.lastIndexOf("/")
                if (ind >= 0) {
                    wsContext = StringBuilder(wsContext).replace(ind, ind + 1, "\\/").toString()
                }
                for (line in Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    val contextRootString = "<%context root%>"
                    var mutableLine = line
                    if (mutableLine.contains(contextRootString)) {
                        mutableLine = mutableLine.replace(contextRootString, context)
                    }
                    val versionNumber = "<%version%>"
                    if (mutableLine.contains(versionNumber)) {
                        mutableLine = mutableLine.replace(versionNumber, version)
                    }
                    val ws = "<%ws root%>"
                    if (mutableLine.contains(ws)) {
                        mutableLine = mutableLine.replace(ws, wsContext)
                    }
                    val version = "<%startTime%>"
                    if (mutableLine.contains(version)) {
                        mutableLine = mutableLine.replace(version, WebDavServlet.START_TIME)
                    }
                    stream.println(mutableLine)
                }
            }
            stream.flush()
        } else {
            previousHandler!!.processRequest(request, response, item)
        }
    }

    /**
     * Determines whether request body shall be logged.
     *
     * @return `true` if request body shall be logged.
     */
    override fun getLogInput(): Boolean {
        return false
    }

    /**
     * Determines whether response body shall be logged.
     *
     * @return `true` if response body shall be logged.
     */
    override fun getLogOutput(): Boolean {
        return false
    }

    /**
     * Determines whether response content length shall be calculated by engine.
     *
     * @return `true` if content length shall be calculated by engine.
     */
    override fun getCalculateContentLength(): Boolean {
        return false
    }

    /**
     * Set previous handler fo GET operation.
     *
     * @param methodHandler previous handler.
     */
    internal fun setPreviousHandler(methodHandler: MethodHandler) {
        previousHandler = methodHandler
    }
}
