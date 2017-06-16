package com.ithit.webdav.samples.deltavservlet;

import com.ithit.webdav.server.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * Helper class to read blobs.
 */
class DownloadUtil {

    /**
     * Writes the content of the blob to the specified stream.
     * <p>
     * Client application can request only a part of a file specifying <b>Range</b> header.
     * Download managers may use this header to download single file using several threads at a time.
     * </p>
     *
     * @param logger     Logger class.
     * @param output     Output stream.
     * @param blob       Blob to write to the stream.
     * @param startIndex Zero-bazed byte offset in file content at which to begin copying bytes to the output stream.
     * @param count      Number of bytes to be written to the output stream.
     * @throws IOException In case of blob reading exception.
     * @throws SQLException In case of an error.
     */
    static void readBlob(Logger logger, OutputStream output, Blob blob, long startIndex, long count) throws SQLException, IOException {
        if (blob != null) {
            try (InputStream stream = blob.getBinaryStream()) {
                int bufSize = 1048576; // 1Mb
                byte[] buf = new byte[bufSize];
                long retval;
                stream.skip(startIndex);
                while ((retval = stream.read(buf)) > 0) {
                    try {
                        if (retval > count) {
                            retval = count;
                        }
                        output.write(buf, 0, (int) retval);
                    } catch (IOException e) {
                        logger.logDebug("Remote host closed connection");
                        return;
                    }

                    startIndex += retval;
                    count -= retval;
                }
            }
        }
    }
}
