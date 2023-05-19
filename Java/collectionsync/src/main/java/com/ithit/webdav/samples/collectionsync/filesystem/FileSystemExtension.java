package com.ithit.webdav.samples.collectionsync.filesystem;

import com.ithit.webdav.samples.collectionsync.filesystem.winapi.WindowsFileSystemItem;
import com.ithit.webdav.server.exceptions.ServerException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
        validateOS();
        return WindowsFileSystemItem.getUsnByPath(path);
    }

    /**
     * Returns id for the path.
     * @param path Path.
     * @return id.
     */
    public static String getId(String path) {
        validateOS();
        return Base64.getEncoder().encodeToString(WindowsFileSystemItem.getId(path).serialize().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns full path by file id.
     * @param itemId item ID.
     * @return file path.
     */
    public static String getPathByItemId(String itemId) {
        validateOS();
        return WindowsFileSystemItem.getPathByItemId(new String(Base64.getDecoder().decode(itemId), StandardCharsets.UTF_8));
    }

    private static void validateOS() {
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            throw new UnsupportedOperationException("This feature is only available on Windows OS.");
        }
    }
}
