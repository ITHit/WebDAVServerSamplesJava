package com.ithit.webdav.samples.collectionsync.filesystem.winapi;

import com.ithit.webdav.server.exceptions.ServerException;
import com.ithit.webdav.server.util.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;

import java.io.Closeable;

/**
 * Represents windows file system file or folder. Provides functions that are not available via Java API.
 */
public class WindowsFileSystemItem implements Closeable {

    private static final int FILE_READ_ATTRIBUTES = WinNT.FILE_READ_ATTRIBUTES;
    private static final int FILE_FLAG_BACKUP_SEMANTICS = WinNT.FILE_FLAG_BACKUP_SEMANTICS;
    private static final int OPEN_EXISTING = WinNT.OPEN_EXISTING;
    private static final int READ_WRITE_DELETE = 1 | 2 | 4;

    private static final int FSCTL_READ_FILE_USN_DATA =
            WinioctlUtil.CTL_CODE(Winioctl.FILE_DEVICE_FILE_SYSTEM, 58,
                    Winioctl.METHOD_NEITHER, Winioctl.FILE_ANY_ACCESS);
    private static final int OUT_BUFFER_SIZE = 512;

    /**
     * File or folder handle.
     */
    private final WinNT.HANDLE fileHandle;

    /**
     * Creates instance of this class.
     * @param fileHandle File or folder handle.
     */
    private WindowsFileSystemItem(final WinNT.HANDLE fileHandle) {
        this.fileHandle = fileHandle;
    }

    /**
     * Gets file or folder USN by path.
     * @param path File or folder path.
     * @return File or folder USN.
     * @throws ServerException In case of USN process exception.
     */
    public static long getUsnByPath(String path) throws ServerException {
        try (WindowsFileSystemItem item = open(path, FILE_READ_ATTRIBUTES, OPEN_EXISTING, READ_WRITE_DELETE)) {
            try (Memory memory = new Memory(OUT_BUFFER_SIZE)) {
                memory.clear();
                IntByReference numBytesReturned = new IntByReference(0);
                boolean success = Kernel32.INSTANCE.DeviceIoControl(item.fileHandle,
                        FSCTL_READ_FILE_USN_DATA, null, 0,
                        memory, (int) memory.size(),
                        numBytesReturned, null);
                if (success) {
                    UsnRecordV2 usnRecordV2 = new UsnRecordV2(memory.share(8));
                    usnRecordV2.read();
                    return usnRecordV2.Usn;
                } else {
                    throw new ServerException(path + ": " + Kernel32Util.getLastErrorMessage());
                }
            }
        }
    }

    /**
     * Closes the file or folder handle.
     */
    @Override
    public void close() {
        Kernel32Util.closeHandle(fileHandle);
    }

    private static WindowsFileSystemItem open(String path, int access, int mode, int share) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        final WinNT.HANDLE fHandle = Kernel32.INSTANCE.CreateFile(path, access, share,
            new WinBase.SECURITY_ATTRIBUTES(), mode, FILE_FLAG_BACKUP_SEMANTICS, null);
        return new WindowsFileSystemItem(fHandle);
    }

    /**
     * USN data structure for native call.
     */
    @Structure.FieldOrder({"RecordLength", "MajorVersion", "MinorVersion", "FileReferenceNumber",
        "ParentFileReferenceNumber", "Usn"})
    public static class UsnRecordV2 extends Structure {

        public UsnRecordV2(Pointer p) {
            super(p);
        }

        public int RecordLength;
        public short MajorVersion;
        public short MinorVersion;
        public int FileReferenceNumber;
        public int ParentFileReferenceNumber;
        public long Usn;
    }
}
