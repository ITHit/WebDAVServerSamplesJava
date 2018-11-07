package com.ithit.webdav.samples.fsstorageservlet

import com.ithit.webdav.server.File
import com.ithit.webdav.server.Folder
import com.ithit.webdav.server.HierarchyItem
import com.ithit.webdav.server.Property
import com.ithit.webdav.server.exceptions.*
import com.ithit.webdav.server.paging.OrderProperty
import com.ithit.webdav.server.paging.PageResults
import com.ithit.webdav.server.quota.Quota
import com.ithit.webdav.server.search.Search
import com.ithit.webdav.server.search.SearchOptions
import org.apache.commons.io.FileUtils
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.stream.Collectors
import java.util.stream.StreamSupport

/**
 * Represents a folder in the File system repository.
 */
internal class FolderImpl
/**
 * Initializes a new instance of the [FolderImpl] class.
 *
 * @param name     Name of hierarchy item.
 * @param path     Relative to WebDAV root folder path.
 * @param created  Creation time of the hierarchy item.
 * @param modified Modification time of the hierarchy item.
 * @param engine   Instance of current [WebDavEngine]
 */
private constructor(name: String, path: String, created: Long, modified: Long,
                    engine: WebDavEngine) : HierarchyItemImpl(name, path, created, modified, engine), Folder, Search, Quota {

    /**
     * Creates new [FileImpl] file with the specified name in this folder.
     *
     * @param name Name of the file to create.
     * @return Reference to created [File].
     * @throws LockedException This folder was locked. Client did not provide the lock token.
     * @throws ServerException In case of an error.
     */
    @Throws(LockedException::class, ServerException::class)
    override fun createFile(name: String): FileImpl? {
        ensureHasToken()

        val fullPath = Paths.get(this.fullPath.toString(), name)
        if (!Files.exists(fullPath)) {
            try {
                Files.createFile(fullPath)
            } catch (e: IOException) {
                throw ServerException(e)
            }
            engine.webSocketServer?.notifyRefresh(path)
            return FileImpl.getFile(path + encode(name), engine)
        }
        return null
    }

    /**
     * Creates new [FolderImpl] folder with the specified name in this folder.
     *
     * @param name Name of the folder to create.
     * @throws LockedException This folder was locked. Client did not provide the lock token.
     * @throws ServerException In case of an error.
     */
    @Throws(LockedException::class, ServerException::class)
    override fun createFolder(name: String) {
        ensureHasToken()

        val fullPath = Paths.get(this.fullPath.toString(), name)
        if (!Files.exists(fullPath)) {
            try {
                Files.createDirectory(fullPath)
                engine.webSocketServer?.notifyRefresh(path)
            } catch (e: IOException) {
                throw ServerException(e)
            }
        }
    }

    /**
     * Gets the array of this folder's children.
     *
     * @param propNames List of properties to retrieve with the children. They will be queried by the engine later.
     * @param offset The number of items to skip before returning the remaining items.
     * @param nResults The number of items to return.
     * @param orderProps List of order properties requested by the client.
     * @return Instance of [PageResults] class that contains items on a requested page and total number of items in a folder.
     * @throws ServerException In case of an error.
     */
    @Throws(ServerException::class)
    override fun getChildren(propNames: List<Property>, offset: Long?, nResults: Long?, orderProps: List<OrderProperty>?): PageResults {
        val decodedPath = HierarchyItemImpl.decodeAndConvertToPath(path)
        val fullFolderPath = Paths.get(rootFolder!! + decodedPath)
        var children: MutableList<HierarchyItemImpl> = ArrayList()
        var total: Long? = null
        try {
            Files.newDirectoryStream(fullFolderPath).use { ds ->
                var paths = StreamSupport.stream<Path>(ds.spliterator(), false).collect(Collectors.toList())
                paths = sortChildren(paths, orderProps)
                for (p in paths) {
                    val childPath = path + encode(p.fileName.toString())
                    val item = engine.getHierarchyItem(childPath) as HierarchyItemImpl
                    children.add(item)
                }
                total = paths.size.toLong()
                if (offset != null && nResults != null) {
                    children = children.stream().skip(offset).limit(nResults).collect(Collectors.toList())
                }
            }
        } catch (e: IOException) {
            engine.logger?.logError(e.message, e)
        }
        return PageResults(children, total)
    }

    @Throws(LockedException::class, MultistatusException::class, ServerException::class)
    override fun delete() {
        ensureHasToken()
        try {
            removeIndex(fullPath, this)
            FileUtils.deleteDirectory(fullPath.toFile())
            engine.webSocketServer?.notifyDelete(path)
        } catch (e: IOException) {
            throw ServerException(e)
        }
    }

    @Throws(LockedException::class, MultistatusException::class, ServerException::class)
    override fun copyTo(folder: Folder, destName: String, deep: Boolean) {
        (folder as FolderImpl).ensureHasToken()

        val relUrl = HierarchyItemImpl.decodeAndConvertToPath(folder.path)
        val destinationFolder = Paths.get(rootFolder!!, relUrl).toString()
        if (isRecursive(relUrl)) {
            throw ServerException("Cannot copy to subfolder", WebDavStatus.FORBIDDEN)
        }
        if (!Files.exists(Paths.get(destinationFolder)))
            throw ServerException()
        try {
            val sourcePath = this.fullPath
            val destinationFullPath = Paths.get(destinationFolder, destName)
            FileUtils.copyDirectory(sourcePath.toFile(), destinationFullPath.toFile())
            engine.webSocketServer?.notifyRefresh(folder.path)
            addIndex(destinationFullPath, folder.path + destName, destName)
        } catch (e: IOException) {
            throw ServerException(e)
        }
        setName(destName)
    }

    /**
     * Check whether current folder is the parent to the destination.
     *
     * @param destFolder Path to the destination folder.
     * @return True if current folder is parent for the destination, false otherwise.
     * @throws ServerException in case of any server exception.
     */
    @Throws(ServerException::class)
    private fun isRecursive(destFolder: String): Boolean {
        return destFolder.startsWith(path.replace("/", java.io.File.separator))
    }

    /**
     * Sorts array of FileSystemInfo according to the specified order.
     * @param paths Array of files and folders to sort.
     * @param orderProps Sorting order.
     * @return Sorted list of files and folders.
     */
    private fun sortChildren(paths: List<Path>, orderProps: List<OrderProperty>?): List<Path> {
        var localPaths = paths
        if (orderProps != null && !orderProps.isEmpty()) {
            var index = 0
            var comparator: Comparator<Path>? = null
            for (orderProperty in orderProps) {
                var tempComp: Comparator<Path>? = null
                if ("is-directory" == orderProperty.property.name) {
                    val sortFunc = { item: Path -> item.toFile().isDirectory }
                    tempComp = Comparator.comparing<Path, Boolean>(sortFunc)
                }
                if ("quota-used-bytes" == orderProperty.property.name) {
                    val sortFunc = { item: Path -> item.toFile().length() }
                    tempComp = Comparator.comparing<Path, Long>(sortFunc)
                }
                if ("getlastmodified" == orderProperty.property.name) {
                    val sortFunc = { item: Path -> item.toFile().lastModified() }
                    tempComp = Comparator.comparing<Path, Long>(sortFunc)
                }
                if ("displayname" == orderProperty.property.name) {
                    val sortFunc = { item: Path -> item.fileName.toString() }
                    tempComp = Comparator.comparing<Path, String>(sortFunc)
                }
                if ("getcontenttype" == orderProperty.property.name) {
                    val sortFunc = { item: Path -> getExtension(item.fileName.toString()) }
                    tempComp = Comparator.comparing(sortFunc)
                }
                if (tempComp != null) {
                    comparator = if (index++ == 0) {
                        if (orderProperty.isAscending) {
                            tempComp
                        } else {
                            tempComp.reversed()
                        }
                    } else {
                        if (orderProperty.isAscending) {
                            if (comparator != null) comparator.thenComparing(tempComp) else tempComp
                        } else {
                            if (comparator != null) comparator.thenComparing(tempComp.reversed()) else tempComp.reversed()
                        }
                    }
                }
            }
            if (comparator != null) {
                localPaths = localPaths.stream().sorted(comparator).collect(Collectors.toList())
            }
        }
        return localPaths
    }

    private fun getExtension(name: String): String {
        val periodIndex = name.lastIndexOf('.')
        return if (periodIndex == -1) "" else name.substring(periodIndex + 1)

    }

    @Throws(LockedException::class, ConflictException::class, MultistatusException::class, ServerException::class)
    override fun moveTo(folder: Folder, destName: String) {
        ensureHasToken()
        (folder as FolderImpl).ensureHasToken()
        val destinationFolder = Paths.get(rootFolder!!, HierarchyItemImpl.decodeAndConvertToPath(folder.path)).toString()
        if (!Files.exists(Paths.get(destinationFolder)))
            throw ConflictException()
        val sourcePath = this.fullPath
        val destinationFullPath = Paths.get(destinationFolder, destName)
        try {
            FileUtils.copyDirectory(sourcePath.toFile(), destinationFullPath.toFile())
            delete()
            addIndex(destinationFullPath, folder.path + destName, destName)
        } catch (e: IOException) {
            throw ServerException(e)
        }

        setName(destName)
        engine.webSocketServer?.notifyDelete(path)
        engine.webSocketServer?.notifyRefresh(folder.path)
    }

    /**
     * Returns list of items that correspond to search request.
     *
     * @param searchString A phrase to search.
     * @param options      Search parameters.
     * @param propNames    List of properties to retrieve with the children. They will be queried by the engine later.
     * @param offset The number of items to skip before returning the remaining items.
     * @param nResults The number of items to return.
     * @return Instance of [PageResults] class that contains items on a requested page and total number of items in search results.
     */
    override fun search(searchString: String, options: SearchOptions, propNames: List<Property>, offset: Long?, nResults: Long?): PageResults {
        val results = LinkedList<HierarchyItem>()
        val searcher = engine.searchFacade!!.searcher ?: return PageResults(results, null)
        var snippet = false
        if (propNames.stream().filter { x -> SNIPPET.equals(x.name, ignoreCase = true) }.findFirst().orElse(null) != null) {
            snippet = true
        }
        val searchResult: Map<String, String>
        try {
            val decodedPath = HierarchyItemImpl.decode(path)
            searchResult = searcher.search(searchString, options, decodedPath, snippet)
            for ((key, value) in searchResult) {
                try {
                    val item = engine.getHierarchyItem(key)
                    if (item != null) {
                        if (snippet && item is FileImpl) {
                            item.snippet = value
                        }
                        results.add(item)
                    }
                } catch (ex: Exception) {
                    engine.logger?.logError("Error during search.", ex)
                }

            }
        } catch (e: ServerException) {
            engine.logger?.logError("Error during search.", e)
        }

        return PageResults(if (offset != null && nResults != null) results.stream().skip(offset).limit(nResults).collect(Collectors.toList()) as LinkedList<HierarchyItem> else results, results.size.toLong())
    }

    /**
     * Returns free bytes available to current user.
     *
     * @return Returns free bytes available to current user.
     */
    override fun getAvailableBytes(): Long {
        return fullPath.toFile().freeSpace
    }

    /**
     * Returns used bytes by current user.
     *
     * @return Returns used bytes by current user.
     */
    override fun getUsedBytes(): Long {
        val total = fullPath.toFile().totalSpace
        return total - availableBytes
    }

    private fun removeIndex(sourcePath: Path, itSelf: FolderImpl) {
        val filesToDelete = ArrayList<HierarchyItem>()
        engine.searchFacade!!.getFilesToIndex(sourcePath.toFile().listFiles(), filesToDelete, WebDavServlet.rootLocalPath!!)
        filesToDelete.add(itSelf)
        for (hi in filesToDelete) {
            try {
                engine.searchFacade!!.indexer!!.deleteIndex(hi)
            } catch (e: Exception) {
                engine.logger?.logError("Cannot delete index.", e)
            }

        }
    }

    private fun addIndex(sourcePath: Path, path: String, name: String) {
        val filesToIndex = ArrayList<HierarchyItem>()
        engine.searchFacade!!.getFilesToIndex(sourcePath.toFile().listFiles(), filesToIndex, WebDavServlet.rootLocalPath!!)
        engine.searchFacade!!.indexer!!.indexFile(name, HierarchyItemImpl.decode(path), null, null)
        for (hi in filesToIndex) {
            try {
                engine.searchFacade!!.indexer!!.indexFile(hi.name, HierarchyItemImpl.decode(hi.path), null, hi)
            } catch (e: Exception) {
                engine.logger?.logError("Cannot index.", e)
            }

        }
    }

    companion object {

        /**
         * Returns folder that corresponds to path.
         *
         * @param path   Encoded path relative to WebDAV root.
         * @param engine Instance of [WebDavEngine]
         * @return Folder instance or null if physical folder not found in file system.
         * @throws ServerException in case of exception
         */
        @Throws(ServerException::class)
        fun getFolder(path: String, engine: WebDavEngine): FolderImpl? {
            var view: BasicFileAttributes? = null
            val fullPath: Path
            var name: String? = null
            try {
                val root = path == "/"
                val pathFragment = HierarchyItemImpl.decodeAndConvertToPath(path)
                val rootFolder = rootFolder
                fullPath = if (root) Paths.get(rootFolder) else Paths.get(rootFolder!!, pathFragment)
                if (Files.exists(fullPath)) {
                    name = if (root) "ROOT" else Paths.get(pathFragment).fileName.toString()
                    view = Files.getFileAttributeView<BasicFileAttributeView>(fullPath, BasicFileAttributeView::class.java).readAttributes()
                }
                if (view == null || !view.isDirectory) {
                    return null
                }
            } catch (e: IOException) {
                throw ServerException()
            }

            val created = view.creationTime().toMillis()
            val modified = view.lastModifiedTime().toMillis()
            return FolderImpl(name!!, fixPath(path), created, modified, engine)
        }

        private fun fixPath(path: String): String {
            var localPaths = path
            if (localPaths.substring(localPaths.length - 1) != "/") {
                localPaths += "/"
            }
            return localPaths
        }
    }
}
