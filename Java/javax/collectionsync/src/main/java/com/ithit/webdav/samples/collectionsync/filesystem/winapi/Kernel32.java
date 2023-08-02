package com.ithit.webdav.samples.collectionsync.filesystem.winapi;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface Kernel32 extends StdCallLibrary, WinNT, Wincon {
    /** The instance. */
    Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class, W32APIOptions.DEFAULT_OPTIONS);

    WinNT.HANDLE OpenFileById(WinNT.HANDLE hVolumeHint, Structure lpFileId, int dwDesiredAccess, int dwShareMode, WinBase.SECURITY_ATTRIBUTES lpSecurityAttributes, int dwFlagsAndAttributes);

    int GetFinalPathNameByHandle(WinNT.HANDLE hFile, Memory lpszFilePath, int cchFilePath, int dwFlags);

    /**
     * Retrieves file information for the specified file.
     * To set file information using a file handle, see SetFileInformationByHandle.
     * @param hFile
     *                 A handle to the file that contains the information to be retrieved.
     * @param FileInformationClass
     *                 A FILE_INFO_BY_HANDLE_CLASS enumeration value that specifies the type of
     *                 information to be retrieved.
     * @param lpFileInformation
     *                 A pointer to the buffer that receives the requested file information.
     *                 The structure that is returned corresponds to the class that is specified
     *                 by FileInformationClass.
     * @param dwBufferSize
     *                 The size of the lpFileInformation buffer, in bytes.
     * @return If the function succeeds, the return value is nonzero and file information
     *            data is contained in the buffer pointed to by the lpFileInformation parameter.
     *         If the function fails, the return value is zero. To get extended error
     *         information, call GetLastError.
     */
    boolean GetFileInformationByHandleEx(HANDLE hFile, int FileInformationClass, Pointer lpFileInformation, DWORD dwBufferSize);

    /**
     * The GetLastError function retrieves the calling thread's last-error code
     * value. The last-error code is maintained on a per-thread basis. Multiple
     * threads do not overwrite each other's last-error code.
     *
     * @return The return value is the calling thread's last-error code value.
     */
    int GetLastError();

    /**
     * The CreateFile function creates or opens a file, file stream, directory,
     * physical disk, volume, console buffer, tape drive, communications
     * resource, mailslot, or named pipe. The function returns a handle that can
     * be used to access an object.
     *
     * @param lpFileName
     *            A pointer to a null-terminated string that specifies the name
     *            of an object to create or open.
     * @param dwDesiredAccess
     *            The access to the object, which can be read, write, or both.
     * @param dwShareMode
     *            The sharing mode of an object, which can be read, write, both,
     *            or none.
     * @param lpSecurityAttributes
     *            A pointer to a SECURITY_ATTRIBUTES structure that determines
     *            whether or not the returned handle can be inherited by child
     *            processes. If lpSecurityAttributes is NULL, the handle cannot
     *            be inherited.
     * @param dwCreationDisposition
     *            An action to take on files that exist and do not exist.
     * @param dwFlagsAndAttributes
     *            The file attributes and flags.
     * @param hTemplateFile
     *            Handle to a template file with the GENERIC_READ access right.
     *            The template file supplies file attributes and extended
     *            attributes for the file that is being created. This parameter
     *            can be NULL.
     * @return If the function succeeds, the return value is an open handle to a
     *         specified file. If a specified file exists before the function
     *         call and dwCreationDisposition is CREATE_ALWAYS or OPEN_ALWAYS, a
     *         call to GetLastError returns ERROR_ALREADY_EXISTS, even when the
     *         function succeeds. If a file does not exist before the call,
     *         GetLastError returns 0 (zero). If the function fails, the return
     *         value is INVALID_HANDLE_VALUE. To get extended error information,
     *         call GetLastError.
     */
    WinNT.HANDLE CreateFile(String lpFileName, int dwDesiredAccess, int dwShareMode,
                            WinBase.SECURITY_ATTRIBUTES lpSecurityAttributes,
                            int dwCreationDisposition, int dwFlagsAndAttributes,
                            Number hTemplateFile);

    /**
     * Closes an open object handle.
     *
     * @param hObject
     *            Handle to an open object. This parameter can be a pseudo
     *            handle or INVALID_HANDLE_VALUE.
     * @return If the function succeeds, the return value is nonzero. If the
     *         function fails, the return value is zero. To get extended error
     *         information, call {@code GetLastError}.
     * @see <A HREF="https://msdn.microsoft.com/en-us/library/windows/desktop/ms724211(v=vs.85).aspx">CloseHandle</A>
     */
    boolean CloseHandle(WinNT.HANDLE hObject);

    /**
     * @param lpRootPathName A string that contains the root directory of the
     * volume to be described. If this parameter is {@code null}, the root of
     * the current directory is used. A trailing backslash is required. For example,
     * you specify &quot;\\MyServer\MyShare\&quot;, or &quot;C:\&quot;.
     * @param lpVolumeNameBuffer If not {@code null} then receives the name of
     * the specified volume. The buffer size is specified by the <tt>nVolumeNameSize</tt>
     * parameter.
     * @param nVolumeNameSize The length of the volume name buffer - max. size is
     * {@link WinDef#MAX_PATH} + 1 - ignored if no volume name buffer provided
     * @param lpVolumeSerialNumber Receives the volume serial number - can be
     * {@code null} if the serial number is not required
     * @param lpMaximumComponentLength Receives the maximum length of a file name
     * component that the underlying file system supports - can be {@code null}
     * if this data is not required
     * @param lpFileSystemFlags Receives flags associated with the file system
     *  - can be {@code null} if this data is not required
     * @param lpFileSystemNameBuffer If not {@code null} then receives the name
     * of the file system. The buffer size is specified by the <tt>nFileSystemNameSize</tt>
     * parameter.
     * @param nFileSystemNameSize The length of the file system name buffer -
     * max. size is {@link WinDef#MAX_PATH} + 1 - ignored if no file system name
     * buffer provided
     * @return {@code true} if succeeds. If fails then call {@link #GetLastError()}
     * to get extended error information
     * @see <a href="https://msdn.microsoft.com/en-us/library/windows/desktop/aa364993(v=vs.85).aspx">GetVolumeInformation</a>
     */
    boolean GetVolumeInformation(String lpRootPathName,
                                 char[] lpVolumeNameBuffer, int nVolumeNameSize,
                                 IntByReference lpVolumeSerialNumber,
                                 IntByReference lpMaximumComponentLength,
                                 IntByReference lpFileSystemFlags,
                                 char[] lpFileSystemNameBuffer, int nFileSystemNameSize);

    /**
     * Sends a control code directly to a specified device driver, causing the
     * corresponding device to perform the corresponding operation.
     *
     * @param hDevice
     *            A handle to the device on which the operation is to be
     *            performed. The device is typically a volume, directory, file,
     *            or stream. To retrieve a device handle, use the CreateFile
     *            function. For more information, see Remarks.
     *
     * @param dwIoControlCode
     *            The control code for the operation. This value identifies the
     *            specific operation to be performed and the type of device on
     *            which to perform it. For a list of the control codes, see
     *            Remarks. The documentation for each control code provides
     *            usage details for the lpInBuffer, nInBufferSize, lpOutBuffer,
     *            and nOutBufferSize parameters.
     *
     * @param lpInBuffer
     *            A pointer to the input buffer that contains the data required
     *            to perform the operation. The format of this data depends on
     *            the value of the dwIoControlCode parameter. This parameter can
     *            be NULL if dwIoControlCode specifies an operation that does
     *            not require input data.
     *
     * @param nInBufferSize
     *            The size of the input buffer, in bytes.
     *
     * @param lpOutBuffer
     *            A pointer to the output buffer that is to receive the data
     *            returned by the operation. The format of this data depends on
     *            the value of the dwIoControlCode parameter. This parameter can
     *            be NULL if dwIoControlCode specifies an operation that does
     *            not return data.
     *
     * @param nOutBufferSize
     *            The size of the output buffer, in bytes.
     *
     * @param lpBytesReturned
     *            A pointer to a variable that receives the size of the data
     *            stored in the output buffer, in bytes. If the output buffer is
     *            too small to receive any data, the call fails, GetLastError
     *            returns ERROR_INSUFFICIENT_BUFFER, and lpBytesReturned is
     *            zero. If the output buffer is too small to hold all of the
     *            data but can hold some entries, some drivers will return as
     *            much data as fits. In this case, the call fails, GetLastError
     *            returns ERROR_MORE_DATA, and lpBytesReturned indicates the
     *            amount of data received. Your application should call
     *            DeviceIoControl again with the same operation, specifying a
     *            new starting point. If lpOverlapped is NULL, lpBytesReturned
     *            cannot be NULL. Even when an operation returns no output data
     *            and lpOutBuffer is NULL, DeviceIoControl makes use of
     *            lpBytesReturned. After such an operation, the value of
     *            lpBytesReturned is meaningless. If lpOverlapped is not NULL,
     *            lpBytesReturned can be NULL. If this parameter is not NULL and
     *            the operation returns data, lpBytesReturned is meaningless
     *            until the overlapped operation has completed. To retrieve the
     *            number of bytes returned, call GetOverlappedResult. If hDevice
     *            is associated with an I/O completion port, you can retrieve
     *            the number of bytes returned by calling
     *            GetQueuedCompletionStatus.
     *
     * @param lpOverlapped
     *            A pointer to an OVERLAPPED structure. If hDevice was opened
     *            without specifying FILE_FLAG_OVERLAPPED, lpOverlapped is
     *            ignored. If hDevice was opened with the FILE_FLAG_OVERLAPPED
     *            flag, the operation is performed as an overlapped
     *            (asynchronous) operation. In this case, lpOverlapped must
     *            point to a valid OVERLAPPED structure that contains a handle
     *            to an event object. Otherwise, the function fails in
     *            unpredictable ways. For overlapped operations, DeviceIoControl
     *            returns immediately, and the event object is signaled when the
     *            operation has been completed. Otherwise, the function does not
     *            return until the operation has been completed or an error
     *            occurs.
     *
     * @return If the function succeeds, the return value is nonzero.
     *
     *         If the function fails, the return value is zero. To get extended
     *         error information, call GetLastError.
     */
    boolean DeviceIoControl(HANDLE hDevice, int dwIoControlCode,
                            Pointer lpInBuffer, int nInBufferSize, Pointer lpOutBuffer,
                            int nOutBufferSize, IntByReference lpBytesReturned,
                            Pointer lpOverlapped);

    @Structure.FieldOrder({"VolumeSerialNumber", "FileId"})
    public static class FILE_ID_INFO extends Structure {

        @FieldOrder({"Identifier"})
        public static class FILE_ID_128 extends Structure {
            public long Identifier;

            public FILE_ID_128() {
                super();
            }

            public FILE_ID_128(Pointer memory) {
                super(memory);
                read();
            }

            public FILE_ID_128(long Identifier) {
                this.Identifier = Identifier;
                write();
            }
        }

        /**
         * The serial number of the volume that contains a file.
         */
        public int VolumeSerialNumber;

        public FILE_ID_128 FileId;

        public static int sizeOf()
        {
            return Native.getNativeSize(FILE_ID_INFO.class, null);
        }

        public FILE_ID_INFO() {
            super();
        }

        public FILE_ID_INFO(Pointer memory) {
            super(memory);
            read();
        }

        public FILE_ID_INFO(int VolumeSerialNumber,
                            FILE_ID_128 FileId) {
            this.VolumeSerialNumber = VolumeSerialNumber;
            this.FileId = FileId;
            write();
        }
    }

    @Structure.FieldOrder({"dwSize", "Type", "Id"})
    public static class FILE_ID_DESCRIPTOR extends Structure {

        public int dwSize;
        public int Type;
        public DUMMYUNIONNAME Id;

        @FieldOrder({"FileId", "ObjectId", "ExtendedFileId"})
        public static class DUMMYUNIONNAME extends Structure {

            public long FileId;
            public Guid.GUID ObjectId;
            public FILE_ID_INFO.FILE_ID_128 ExtendedFileId;

            public DUMMYUNIONNAME() {
                super();
            }

            public
            DUMMYUNIONNAME(long Id) {
                this.FileId = Id;
                write();
            }
        }

        public FILE_ID_DESCRIPTOR(int dwSize, int type,
                                  DUMMYUNIONNAME id) {
            this.dwSize = dwSize;
            this.Type = type;
            this.Id = id;
            write();
        }
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
