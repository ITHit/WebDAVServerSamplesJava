package com.ithit.webdav.samples.collectionsync.filesystem.winapi;

public class WindowsFileSystemItemId {

    private final int volumeId;
    private final long fileId;

    public WindowsFileSystemItemId(int volumeId, long fileId) {
        this.volumeId = volumeId;
        this.fileId = fileId;
    }

    public int getVolumeId() {
        return volumeId;
    }

    public long getFileId() {
        return fileId;
    }

    public String serialize() {
        return String.format("%s-%s", volumeId, fileId);
    }

    public static WindowsFileSystemItemId deserialize(String serialized) {
        final String[] split = serialized.split("-");
        if (split.length != 2) {
            return null;
        }
        try {
            return new WindowsFileSystemItemId(Integer.parseInt(split[0]), Long.parseLong(split[1]));
        } catch (NumberFormatException nfe) {
            return null;
        }
    }
}
