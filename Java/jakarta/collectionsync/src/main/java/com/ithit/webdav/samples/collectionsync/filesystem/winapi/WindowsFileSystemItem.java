package com.ithit.webdav.samples.collectionsync.filesystem.winapi;

import com.ithit.webdav.server.exceptions.ServerException;
import com.ithit.webdav.server.util.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;

import java.io.Closeable;

/**
 * Represents Windows file system file or folder. Provides functions that are not available via Java API.
 */
public class WindowsFileSystemItem implements Closeable {

    private static final int FILE_READ_ATTRIBUTES = WinNT.FILE_READ_ATTRIBUTES;
    private static final int GENERIC_READ = WinNT.GENERIC_READ;
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
     *
     * @param fileHandle File or folder handle.
     */
    private WindowsFileSystemItem(final WinNT.HANDLE fileHandle) {
        this.fileHandle = fileHandle;
    }

    /**
     * Gets file or folder USN by path.
     *
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
                    Kernel32.UsnRecordV2 usnRecordV2 = new Kernel32.UsnRecordV2(memory.share(8));
                    usnRecordV2.read();
                    return usnRecordV2.Usn;
                } else {
                    throw new ServerException(path + ": " + Kernel32Util.getLastErrorMessage());
                }
            }
        }
    }

    public static WindowsFileSystemItemId getId(String path) {
        try (WindowsFileSystemItem item = open(path, FILE_READ_ATTRIBUTES, OPEN_EXISTING, READ_WRITE_DELETE)) {
            Kernel32.FILE_ID_INFO fii;
            try (Memory p = new Memory(24)) {
                Kernel32.INSTANCE.GetFileInformationByHandleEx(item.fileHandle, WinBase.FileIdInfo, p, new WinDef.DWORD(p.size()));
                fii = new Kernel32.FILE_ID_INFO(p);
                return new WindowsFileSystemItemId(fii.VolumeSerialNumber, fii.FileId.Identifier);
            }
        }
    }

    public static String getPathByItemId(String volumeName, long itemId) {
        try (WindowsFileSystemItem item = openById(volumeName, itemId)) {
            if (item == null) {
                return null;
            }
            int requiredLength = Kernel32.INSTANCE.GetFinalPathNameByHandle(item.fileHandle, null, 0, 0);
            if (requiredLength != 0) {
                try (Memory pathBuilder = new Memory(requiredLength * 2L)) {
                    Kernel32.INSTANCE.GetFinalPathNameByHandle(item.fileHandle, pathBuilder, requiredLength, 0);
                    return pathBuilder.getWideString(8);
                }
            }
            return null;
        }
    }

    private static WindowsFileSystemItem openById(String volumeName, long itemId) {
        try (WindowsFileSystemItem volume = open(volumeName, GENERIC_READ, OPEN_EXISTING, READ_WRITE_DELETE)) {
            if (volume.fileHandle == null || invalidHandle(volume.fileHandle)) {
                return null;
            }
            Kernel32.FILE_ID_DESCRIPTOR fileIdDesc = new Kernel32.FILE_ID_DESCRIPTOR(128, 0, new Kernel32.FILE_ID_DESCRIPTOR.DUMMYUNIONNAME(itemId));
            WinNT.HANDLE handle = Kernel32.INSTANCE.OpenFileById(volume.fileHandle, fileIdDesc, FILE_READ_ATTRIBUTES, 1 | 2 | 4, null, FILE_FLAG_BACKUP_SEMANTICS);
            if (handle == null || invalidHandle(handle)) {
                return null;
            }
            return new WindowsFileSystemItem(handle);
        }
    }

    private static boolean invalidHandle(WinNT.HANDLE fileHandle) {
        return fileHandle.toString().startsWith("const");
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

}
