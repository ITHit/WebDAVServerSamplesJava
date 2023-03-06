package com.ithit.webdav.samples.collectionsync.filesystem;

import com.ithit.webdav.samples.collectionsync.filesystem.winapi.WindowsFileSystemItem;
import com.ithit.webdav.server.exceptions.ServerException;

/**
 * Helper utility class to get USN by path.
 */
public class FileSystemExtension {

    /**
     * Empty private constructor to use only static methods.
     */
    private FileSystemExtension() {}

    /**
     * Gets file or folder USN by path.
     * @param path File or folder path.
     * @return File or folder USN.
     * @throws ServerException In case of USN process exception.
     */
    public static long getUsn(String path) throws ServerException {
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            throw new UnsupportedOperationException("This feature is only available on Windows OS.");
        }
        return WindowsFileSystemItem.getUsnByPath(path);
    }
}
