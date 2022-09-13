package com.ithit.webdav.samples.springboots3.s3;

import com.ithit.webdav.samples.springboots3.impl.FileImpl;
import com.ithit.webdav.samples.springboots3.impl.FolderImpl;
import com.ithit.webdav.samples.springboots3.impl.WebDavEngine;
import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.util.StringUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@AllArgsConstructor
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
/*
  Amazon S3 client
 */
public class DataClient {

    S3Client s3Client;
    String bucket;
    String context;
    static final String FOLDER = "application/x-directory";

    /**
     * Locates object in S3 by original WebDAV path. Returns null if nothing is found.
     * @param originalPath - WebDAV path.
     * @param engine - WebDAV engine.
     * @return - {@link HierarchyItemImpl} or null if nothing is found.
     */
    public HierarchyItem locateObject(final String originalPath, WebDavEngine engine) {
        String key = getFolderContext(originalPath);
        boolean root = key.equals("");
        try {
            if (root) {
                return FolderImpl.getFolder(originalPath, "ROOT", 0, 0, engine);
            } else {
                HeadObjectResponse response;
                String name = getName(key);
                try {
                    response = s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
                    final long modified = response.lastModified().toEpochMilli();
                    return FolderImpl.getFolder(originalPath, name, modified, modified, engine);
                } catch (NoSuchKeyException ex) {
                    key = getContext(originalPath);
                    response = s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
                    final long modified = response.lastModified().toEpochMilli();
                    final Long contentLength = response.contentLength();
                    return FileImpl.getFile(originalPath, name, modified, modified, contentLength == null ? 0 : contentLength, engine);
                }
            }
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Returns all child of the specified key in S3.
     * @param originalPath WebDAV context path.
     * @param engine - WebDAV engine.
     * @return - list of {@link HierarchyItem}.
     */
    public List<HierarchyItem> getChildren(final String originalPath, WebDavEngine engine) {
        String key = getContext(originalPath);
        final ListObjectsV2Request objectsV2Request = ListObjectsV2Request.builder().bucket(bucket).prefix(key).delimiter("/").build();
        final ListObjectsV2Response response = s3Client.listObjectsV2(objectsV2Request);
        final ArrayList<HierarchyItem> items = new ArrayList<>();
        for (CommonPrefix commonPrefix: response.commonPrefixes()) {
            String name = StringUtil.trimEnd(commonPrefix.prefix().replace(key, ""), "/");
            items.add(FolderImpl.getFolder(context + commonPrefix.prefix(), name, 0, 0, engine));
        }
        for (S3Object s3Object: response.contents()) {
            String name = s3Object.key().replace(key, "");
            if (Objects.equals(name, "")) {
                continue;
            }
            final long created = s3Object.lastModified().toEpochMilli();
            final Long contentLength = s3Object.size();
            items.add(FileImpl.getFile(context + s3Object.key(), name, created, created, contentLength == null ? 0 : contentLength, engine));
        }
        return items;
    }

    /**
     * Downloads object by key fro S3.
     * @param originalPath WebDAV context path.
     * @return InputStream of the object.
     */
    public ResponseInputStream<GetObjectResponse> getObject(final String originalPath) {
        String key = getContext(originalPath);
        return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    /**
     * Gets metadata of the object by key.
     * @param originalPath WebDAV context path.
     * @param metaKey metadata key.
     * @return Metadata value.
     */
    public String getMetadata(String originalPath, String metaKey) {
        String key = getContext(originalPath);
        if (key.equals("")) {
            return null;
        }
        final HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket(bucket).key(key).build();
        final HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
        return headObjectResponse.metadata().get(metaKey.toLowerCase());
    }

    /**
     * Updates or sets new metadata of the object.
     * @param originalPath WebDAV context path.
     * @param metaKey metadata key.
     * @param metadata metadata value or null if you want to remove it.
     */
    public void setMetadata(String originalPath, String metaKey, String metadata) {
        String key = getContext(originalPath);
        String encodedUrl = encodeKey(key);
        Map<String, String> md = loadExistingMetadata(key);
        updateMetadata(metaKey, metadata, md);
        CopyObjectRequest copyReq = CopyObjectRequest.builder()
                .copySource(encodedUrl)
                .destinationBucket(bucket)
                .destinationKey(key)
                .metadata(md)
                .metadataDirective(MetadataDirective.REPLACE)
                .build();
        s3Client.copyObject(copyReq);
    }

    /**
     * Stores or updates existing object at the specified key.
     * @param originalPath WebDAV context path.
     * @param content InputStream of the object.
     * @param contentType object content type
     * @param totalFileLength content length
     */
    public void storeObject(String originalPath, InputStream content, String contentType, long totalFileLength) {
        String key = getContext(originalPath);
        Map<String, String> metadata = loadExistingMetadata(key);
        final PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentLength(totalFileLength)
                .contentType(contentType)
                .metadata(metadata)
                .build();
        final RequestBody requestBody = content != null ? RequestBody.fromInputStream(content, totalFileLength) : RequestBody.empty();
        s3Client.putObject(request, requestBody);
    }

    /**
     * Creates new folder in S3.
     * @param originalPath WebDAV context path.
     */
    public void createFolder(String originalPath) {
        storeObject(originalPath, null, FOLDER, 0);
    }

    /**
     * Deletes object by key.
     * @param originalPath WebDAV context path.
     */
    public void delete(String originalPath) {
        String key = getContext(originalPath);
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    /**
     * Copies object within S3.
     * @param originalPath source WebDAV context path.
     * @param originalDestKey destination WebDAV context path.
     */
    public void copy(String originalPath, String originalDestKey) {
        String key = getContext(originalPath);
        String destKey = getContext(originalDestKey);
        String encodedUrl = encodeKey(key);
        CopyObjectRequest copyReq = CopyObjectRequest.builder()
                .copySource(encodedUrl)
                .destinationBucket(bucket)
                .destinationKey(destKey)
                .build();
        s3Client.copyObject(copyReq);
    }

    private String getFolderContext(String originalPath) {
        String path = "";
        if (!context.startsWith(originalPath)) {
            path = originalPath.replace(context, "");
            if (!path.endsWith("/")) {
                path += "/";
            }
        }
        return path;
    }

    private String getContext(String originalPath) {
        String path = "";
        if (!context.startsWith(originalPath)) {
            path = originalPath.replace(context, "");
        }
        return path;
    }

    private Map<String, String> loadExistingMetadata(String key) {
        Map<String, String> md = new HashMap<>();
        try {
            md = new HashMap<>(s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()).metadata());
        } catch (Exception ignored) {}
        return md;
    }

    private void updateMetadata(String metaKey, String metadata, Map<String, String> md) {
        if (metadata != null) {
            md.put(metaKey.toLowerCase(), metadata);
        } else {
            md.remove(metaKey.toLowerCase());
        }
    }

    private String encodeKey(String key) {
        String encodedUrl = null;
        try {
            encodedUrl = URLEncoder.encode(bucket + "/" + key, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ignored) {
        }
        return encodedUrl;
    }

    private String getName(String key) {
        key = StringUtil.trimEnd(key, "/");
        return key.substring(key.lastIndexOf('/') + 1);
    }
}
