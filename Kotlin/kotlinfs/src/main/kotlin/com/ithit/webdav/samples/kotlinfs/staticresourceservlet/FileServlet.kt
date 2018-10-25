/*
 * Copyright 2018 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.ithit.webdav.samples.kotlinfs.staticresourceservlet

import java.io.*
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import java.util.zip.GZIPOutputStream
import javax.servlet.ServletConfig
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * Servlet which processes static resources if root context is mapped to WebDAV servlet
 */
class FileServlet : HttpServlet() {
    private var folder: File? = null

    /**
     * Returns how long the resource may be cached by the client before it expires, in seconds.
     *
     *
     * The default implementation returns 30 days in seconds.
     * @return The client cache expire time in seconds (not milliseconds!).
     */
    private val expireTime: Long
        get() = DEFAULT_EXPIRE_TIME_IN_SECONDS

    // Actions --------------------------------------------------------------------------------------------------------

    override fun init(servletConfig: ServletConfig) {
        folder = File(servletConfig.servletContext.getRealPath("/"), "WEB-INF")
    }

    @Throws(IOException::class)
    override fun doHead(request: HttpServletRequest, response: HttpServletResponse) {
        doRequest(request, response, true)
    }

    @Throws(IOException::class)
    override fun doGet(request: HttpServletRequest, response: HttpServletResponse) {
        doRequest(request, response, false)
    }

    @Throws(IOException::class)
    private fun doRequest(request: HttpServletRequest, response: HttpServletResponse, head: Boolean) {
        response.reset()
        val resource: Resource

        try {
            resource = Resource(getFile(request))
        } catch (e: IllegalArgumentException) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST)
            return
        }

        if (resource.file == null) {
            handleFileNotFound(response)
            return
        }

        if (preconditionFailed(request, resource)) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED)
            return
        }

        setCacheHeaders(response, resource, expireTime)

        if (notModified(request, resource)) {
            response.status = HttpServletResponse.SC_NOT_MODIFIED
            return
        }

        val ranges = getRanges(request, resource)

        if (ranges == null) {
            response.setHeader("Content-Range", "bytes */" + resource.length)
            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE)
            return
        }

        if (!ranges.isEmpty()) {
            response.status = HttpServletResponse.SC_PARTIAL_CONTENT
        } else {
            ranges.add(Range(0, resource.length - 1)) // Full content.
        }

        var contentType = setContentHeaders(request, response, resource, ranges)

        if (head) {
            return
        }
        // Checking if client supports gzip
        var acceptsGzip = false
        if (DEFAULT_MIMETYPES.contains(contentType!!.split(";".toRegex(), 2).toTypedArray()[0])) {
            val acceptEncoding = request.getHeader("Accept-Encoding")
            acceptsGzip = acceptEncoding != null && accepts(acceptEncoding, "gzip")
            contentType += ";charset=UTF-8"
        }
        writeContent(response, resource, ranges, contentType, acceptsGzip)
    }

    /**
     * Returns the file associated with the given HTTP servlet request.
     * If this method throws [IllegalArgumentException], then the servlet will return a HTTP 400 error.
     * If this method returns `null`, or if [File.isFile] returns `false`, then the
     * servlet will invoke [.handleFileNotFound].
     * @param request The involved HTTP servlet request.
     * @return The file associated with the given HTTP servlet request.
     * @throws IllegalArgumentException When the request is mangled in such way that it's not recognizable as a valid
     * file request. The servlet will then return a HTTP 400 error.
     */
    @Throws(IllegalArgumentException::class)
    private fun getFile(request: HttpServletRequest): File {
        val servletPath = request.servletPath
        val pathInfo = request.pathInfo

        if (servletPath == null || servletPath.isEmpty() || pathInfo == null || pathInfo.isEmpty() || "/" == pathInfo) {
            throw IllegalArgumentException()
        }
        return Paths.get(folder!!.absolutePath, servletPath, pathInfo).toFile()
    }

    /**
     * Handles the case when the file is not found.
     *
     *
     * The default implementation sends a HTTP 404 error.
     * @param response The involved HTTP servlet response.
     * @throws IOException When something fails at I/O level.
     */
    @Throws(IOException::class)
    private fun handleFileNotFound(response: HttpServletResponse) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND)
    }

    /**
     * Returns the content type associated with the given HTTP servlet request and file.
     *
     *
     * The default implementation delegates [File.getName] to [javax.servlet.ServletContext.getMimeType] with a
     * fallback default value of `application/octet-stream`.
     * @param request The involved HTTP servlet request.
     * @param file The involved file.
     * @return The content type associated with the given HTTP servlet request and file.
     */
    private fun getContentType(request: HttpServletRequest, file: File): String? {
        return coalesce(request.servletContext.getMimeType(file.name), "application/octet-stream")
    }

    /**
     * Returns `true` if we must force a "Save As" dialog based on the given HTTP servlet request and content
     * type as obtained from [.getContentType].
     *
     *
     * The default implementation will return `true` if the content type does **not** start with
     * `text` or `image`, and the `Accept` request header is either `null`
     * or does not match the given content type.
     * @param request The involved HTTP servlet request.
     * @param contentType The content type of the involved file.
     * @return `true` if we must force a "Save As" dialog based on the given HTTP servlet request and content
     * type.
     */
    private fun isAttachment(request: HttpServletRequest, contentType: String?): Boolean {
        val accept = request.getHeader("Accept")
        return !startsWithOneOf(contentType, "text", "image") && (accept == null || !accepts(accept, contentType))
    }

    /**
     * Returns the file name to be used in `Content-Disposition` header.
     * This does not need to be URL-encoded as this will be taken care of.
     *
     *
     * The default implementation returns [File.getName].
     * @param file The involved file.
     * @return The file name to be used in `Content-Disposition` header.
     */
    private fun getAttachmentName(file: File): String {
        return file.name
    }

    // Sub-actions ----------------------------------------------------------------------------------------------------

    /**
     * Returns true if it's a conditional request which must return 412.
     */
    private fun preconditionFailed(request: HttpServletRequest, resource: Resource): Boolean {
        val match = request.getHeader("If-Match")
        val unmodified = request.getDateHeader("If-Unmodified-Since")
        return if (match != null) !matches(match, resource.eTag) else unmodified != -1L && modified(unmodified, resource.lastModified)
    }

    /**
     * Set cache headers.
     */
    private fun setCacheHeaders(response: HttpServletResponse, resource: Resource, expires: Long) {
        setCacheHeaders(response, expires)
        response.setHeader("ETag", resource.eTag)
        response.setDateHeader("Last-Modified", resource.lastModified)
    }

    /**
     * Returns true if it's a conditional request which must return 304.
     */
    private fun notModified(request: HttpServletRequest, resource: Resource): Boolean {
        val noMatch = request.getHeader("If-None-Match")
        val modified = request.getDateHeader("If-Modified-Since")
        return if (noMatch != null) matches(noMatch, resource.eTag) else modified != -1L && !modified(modified, resource.lastModified)
    }

    /**
     * Get requested ranges. If this is null, then we must return 416. If this is empty, then we must return full file.
     */
    private fun getRanges(request: HttpServletRequest, resource: Resource): MutableList<Range>? {
        val ranges = ArrayList<Range>(1)
        val rangeHeader = request.getHeader("Range")

        if (rangeHeader == null) {
            return ranges
        } else if (!RANGE_PATTERN.matcher(rangeHeader).matches()) {
            return null // Syntax error.
        }

        val ifRange = request.getHeader("If-Range")

        if (ifRange != null && ifRange != resource.eTag) {
            try {
                val ifRangeTime = request.getDateHeader("If-Range")

                if (ifRangeTime != -1L && modified(ifRangeTime, resource.lastModified)) {
                    return ranges
                }
            } catch (ifRangeHeaderIsInvalid: IllegalArgumentException) {
                return ranges
            }

        }

        for (rangeHeaderPart in rangeHeader.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val range = parseRange(rangeHeaderPart, resource.length)
                    ?: return null // Logic error.

            ranges.add(range)
        }

        return ranges
    }

    /**
     * Parse range header part. Returns null if there's a logic error (i.e. start after end).
     */
    private fun parseRange(range: String, length: Long): Range? {
        var start = sublong(range, 0, range.indexOf('-'))
        var end = sublong(range, range.indexOf('-') + 1, range.length)

        if (start == -1L) {
            start = length - end
            end = length - 1
        } else if (end == -1L || end > length - 1) {
            end = length - 1
        }

        return if (start > end) {
            null // Logic error.
        } else Range(start, end)

    }

    /**
     * Set content headers.
     */
    private fun setContentHeaders(request: HttpServletRequest, response: HttpServletResponse, resource: Resource, ranges: List<Range>): String? {
        val contentType = getContentType(request, resource.file!!)
        val filename = getAttachmentName(resource.file)
        val attachment = isAttachment(request, contentType)
        response.setHeader("Content-Disposition", formatContentDispositionHeader(filename, attachment))
        response.setHeader("Accept-Ranges", "bytes")

        if (ranges.size == 1) {
            val range = ranges[0]
            response.contentType = contentType
            if (response.status == HttpServletResponse.SC_PARTIAL_CONTENT) {
                response.setHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + resource.length)
            }
        } else {
            response.contentType = "multipart/byteranges; boundary=$MULTIPART_BOUNDARY"
        }

        return contentType
    }

    /**
     * Write given file to response with given content type and ranges.
     */
    @Throws(IOException::class)
    private fun writeContent(response: HttpServletResponse, resource: Resource, ranges: List<Range>, contentType: String, acceptsGzip: Boolean) {
        val output = response.outputStream

        if (ranges.size == 1) {
            val range = ranges[0]
            if (acceptsGzip) {
                // The browser accepts GZIP, so GZIP the content.
                response.setHeader("Content-Encoding", "gzip")
                stream(resource.file, GZIPOutputStream(output, DEFAULT_STREAM_BUFFER_SIZE), range.start, range.length)
            } else {
                response.setHeader("Content-Length", range.length.toString())
                stream(resource.file, output, range.start, range.length)
            }
        } else {
            for (range in ranges) {
                output.println()
                output.println("--$MULTIPART_BOUNDARY")
                output.println("Content-Type: $contentType")
                output.println("Content-Range: bytes " + range.start + "-" + range.end + "/" + resource.length)
                stream(resource.file, output, range.start, range.length)
            }

            output.println()
            output.println("--$MULTIPART_BOUNDARY--")
        }
    }

    /**
     * Returns the first non-`null` object of the argument list, or `null` if there is no such
     * element.
     * @param <T> The generic object type.
     * @param objects The argument list of objects to be tested for non-`null`.
     * @return The first non-`null` object of the argument list, or `null` if there is no such
     * element.
    </T> */
    @SafeVarargs
    private fun <T> coalesce(vararg objects: T): T? {
        for (`object` in objects) {
            if (`object` != null) {
                return `object`
            }
        }
        return null
    }

    /**
     * Returns `true` if the given string starts with one of the given prefixes.
     * @param string The object to be checked if it starts with one of the given prefixes.
     * @param prefixes The argument list of prefixes to be checked
     * @return `true` if the given string starts with one of the given prefixes.
     */
    private fun startsWithOneOf(string: String?, vararg prefixes: String): Boolean {
        for (prefix in prefixes) {
            if (string?.startsWith(prefix)!!) {
                return true
            }
        }
        return false
    }

    /**
     *
     * Set the cache headers. If the `expires` argument is larger than 0 seconds, then the following headers
     * will be set:
     *
     *  * `Cache-Control: public,max-age=[expiration time in seconds],must-revalidate`
     *  * `Expires: [expiration date of now plus expiration time in seconds]`
     *
     *
     * Else the method will delegate to [.setNoCacheHeaders].
     * @param response The HTTP servlet response to set the headers on.
     * @param expires The expire time in seconds (not milliseconds!).
     */
    private fun setCacheHeaders(response: HttpServletResponse, expires: Long) {
        if (expires > 0) {
            response.setHeader("Cache-Control", "public,max-age=$expires,must-revalidate")
            response.setDateHeader("Expires", System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expires))
            response.setHeader("Pragma", "") // Explicitly set pragma to prevent container from overriding it.
        } else {
            setNoCacheHeaders(response)
        }
    }

    /**
     *
     * Set the no-cache headers. The following headers will be set:
     *
     *  * `Cache-Control: no-cache,no-store,must-revalidate`
     *  * `Expires: [expiration date of 0]`
     *  * `Pragma: no-cache`
     *
     * Set the no-cache headers.
     * @param response The HTTP servlet response to set the headers on.
     */
    private fun setNoCacheHeaders(response: HttpServletResponse) {
        response.setHeader("Cache-Control", "no-cache,no-store,must-revalidate")
        response.setDateHeader("Expires", 0)
        response.setHeader("Pragma", "no-cache") // Backwards compatibility for HTTP 1.0.
    }

    /**
     *
     * Format an UTF-8 compatible content disposition header for the given filename and whether it's an attachment.
     * @param filename The filename to appear in "Save As" dialogue.
     * @param attachment Whether the content should be provided as an attachment or inline.
     * @return An UTF-8 compatible content disposition header.
     */
    private fun formatContentDispositionHeader(filename: String, attachment: Boolean): String {
        return String.format("%s;filename=\"%2\$s\"; filename*=UTF-8''%2\$s", if (attachment) "attachment" else "inline", encodeURI(filename))
    }

    /**
     * URI-encode the given string using UTF-8. URIs (paths and filenames) have different encoding rules as compared to
     * URL query string parameters. [URLEncoder] is actually only for www (HTML) form based query string parameter
     * values (as used when a webbrowser submits a HTML form). URI encoding has a lot in common with URL encoding, but
     * the space has to be %20 and some chars doesn't necessarily need to be encoded.
     * @param string The string to be URI-encoded using UTF-8.
     * @return The given string, URI-encoded using UTF-8, or `null` if `null` was given.
     * @throws UnsupportedOperationException When this platform does not support UTF-8.
     */
    private fun encodeURI(string: String?): String? {
        return if (string == null) {
            null
        } else encodeURL(string)!!
                .replace("+", "%20")
                .replace("%21", "!")
                .replace("%27", "'")
                .replace("%28", "(")
                .replace("%29", ")")
                .replace("%7E", "~")
    }

    /**
     * Stream the given input to the given output via NIO [Channels] and a directly allocated NIO
     * [ByteBuffer]. Both the input and output streams will implicitly be closed after streaming,
     * regardless of whether an exception is been thrown or not.
     * @param input The input stream.
     * @param output The output stream.
     * @return The length of the written bytes.
     * @throws IOException When an I/O error occurs.
     */
    @Throws(IOException::class)
    private fun stream(input: InputStream, output: OutputStream): Long {
        Channels.newChannel(input).use { inputChannel ->
            Channels.newChannel(output).use { outputChannel ->
                val buffer = ByteBuffer.allocateDirect(DEFAULT_STREAM_BUFFER_SIZE)
                var size: Long = 0

                while (inputChannel.read(buffer) != -1) {
                    buffer.flip()
                    size += outputChannel.write(buffer).toLong()
                    buffer.clear()
                }

                return size
            }
        }
    }

    /**
     * Stream a specified range of the given file to the given output via NIO [Channels] and a directly allocated
     * NIO [ByteBuffer]. The output stream will only implicitly be closed after streaming when the specified range
     * represents the whole file, regardless of whether an exception is been thrown or not.
     * @param file The file.
     * @param output The output stream.
     * @param start The start position (offset).
     * @param length The (intented) length of written bytes.
     * @return The (actual) length of the written bytes. This may be smaller when the given length is too large.
     * @throws IOException When an I/O error occurs.
     */
    @Throws(IOException::class)
    private fun stream(file: File?, output: OutputStream, start: Long, length: Long): Long {
        if (start == 0L && length >= file!!.length()) {
            return stream(FileInputStream(file), output)
        }

        (Files.newByteChannel(file!!.toPath(), StandardOpenOption.READ) as FileChannel).use { fileChannel ->
            val outputChannel = Channels.newChannel(output)
            val buffer = ByteBuffer.allocateDirect(DEFAULT_STREAM_BUFFER_SIZE)
            var size: Long = 0

            while (fileChannel.read(buffer, start + size) != -1) {
                buffer.flip()

                if (size + buffer.limit() > length) {
                    buffer.limit((length - size).toInt())
                }

                size += outputChannel.write(buffer).toLong()

                if (size >= length) {
                    break
                }

                buffer.clear()
            }

            return size
        }
    }


    // Nested classes -------------------------------------------------------------------------------------------------

    /**
     * Convenience class for a file resource.
     */
    private class Resource(file: File?) {
        val file: File?
        val length: Long
        val lastModified: Long
        val eTag: String?

        init {
            if (file != null && file.isFile) {
                this.file = file
                length = file.length()
                lastModified = file.lastModified()
                eTag = String.format(ETAG, encodeURL(file.name), lastModified)
            } else {
                this.file = null
                length = 0
                lastModified = 0
                eTag = null
            }
        }

    }

    /**
     * Convenience class for a byte range.
     */
    private class Range(val start: Long, val end: Long) {
        val length: Long = end - start + 1

    }

    companion object {

        // Constants ------------------------------------------------------------------------------------------------------

        private const val serialVersionUID = 1L
        private const val DEFAULT_STREAM_BUFFER_SIZE = 10240
        private val DEFAULT_EXPIRE_TIME_IN_SECONDS = TimeUnit.DAYS.toSeconds(1)
        private val ONE_SECOND_IN_MILLIS = TimeUnit.SECONDS.toMillis(1)
        private const val ETAG = "W/\"%s-%s\""
        private val RANGE_PATTERN = Pattern.compile("^bytes=[0-9]*-[0-9]*(,[0-9]*-[0-9]*)*$")
        private val MULTIPART_BOUNDARY = UUID.randomUUID().toString()
        private val DEFAULT_MIMETYPES = HashSet(Arrays.asList("text/plain", "text/html", "text/xml", "text/css", "text/javascript", "text/csv", "text/rtf",
                "application/xml", "application/xhtml+xml", "application/javascript", "application/json",
                "image/svg+xml"))

        // Helpers --------------------------------------------------------------------------------------------------------

        /**
         * Returns true if the given match header matches the given ETag value.
         */
        private fun matches(matchHeader: String, eTag: String?): Boolean {
            val matchValues = matchHeader.split("\\s*,\\s*".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            Arrays.sort(matchValues)
            return Arrays.binarySearch(matchValues, eTag) > -1 || Arrays.binarySearch(matchValues, "*") > -1
        }

        /**
         * Returns true if the given modified header is older than the given last modified value.
         */
        private fun modified(modifiedHeader: Long, lastModified: Long): Boolean {
            return modifiedHeader + ONE_SECOND_IN_MILLIS <= lastModified // That second is because the header is in seconds, not millis.
        }

        /**
         * Returns a substring of the given string value from the given begin index to the given end index as a long.
         * If the substring is empty, then -1 will be returned.
         */
        private fun sublong(value: String, beginIndex: Int, endIndex: Int): Long {
            val substring = value.substring(beginIndex, endIndex)
            return if (substring.isEmpty()) -1 else java.lang.Long.parseLong(substring)
        }

        /**
         * Returns true if the given accept header accepts the given value.
         */
        private fun accepts(acceptHeader: String, toAccept: String?): Boolean {
            val acceptValues = acceptHeader.split("\\s*([,;])\\s*".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            Arrays.sort(acceptValues)
            return (Arrays.binarySearch(acceptValues, toAccept) > -1
                    || Arrays.binarySearch(acceptValues, toAccept!!.replace("/.*$".toRegex(), "/*")) > -1
                    || Arrays.binarySearch(acceptValues, "*/*") > -1)
        }

        /**
         * URL-encode the given string using UTF-8.
         * @param string The string to be URL-encoded using UTF-8.
         * @return The given string, URL-encoded using UTF-8, or `null` if `null` was given.
         * @throws UnsupportedOperationException When this platform does not support UTF-8.
         */
        private fun encodeURL(string: String?): String? {
            if (string == null) {
                return null
            }
            try {
                return URLEncoder.encode(string, StandardCharsets.UTF_8.name())
            } catch (e: UnsupportedEncodingException) {
                throw UnsupportedOperationException("UTF-8 is apparently not supported on this platform.", e)
            }

        }
    }

}
