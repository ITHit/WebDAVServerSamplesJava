package com.ithit.webdav.samples.springboot.common;

import com.ithit.webdav.samples.springboot.SpringBootSampleApplication;
import com.ithit.webdav.samples.springboot.configuration.WebDavConfigurationProperties;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.system.ApplicationTemp;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@FieldDefaults(level = PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Component
public class ResourceReader {

    public static String STORAGE_FOLDER = "StorageRoot";
    private static String INDEX_FOLDER = "Index";
    private File root = new File(getRootFolder());
    ResourceLoader resourceLoader;
    WebDavConfigurationProperties properties;

    public void readFiles() {
        Resource[] resources;
        try {
            final String storageRoot = "Storage";
            String LOCAL_STATIC_RESOURCES_FOLDER_PATH = "classpath:" + storageRoot + "/**";
            resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                    .getResources(LOCAL_STATIC_RESOURCES_FOLDER_PATH);
            for (Resource resource : resources) {
                if (resource.exists() & resource.isReadable()) {
                    URL url = resource.getURL();
                    String urlString = url.toExternalForm();
                    String targetName = urlString.substring(urlString.indexOf(storageRoot));
                    File destination = new File(root, Paths.get(STORAGE_FOLDER,
                            properties.getRootContext(),
                            URLDecoder.decode(targetName.substring(storageRoot.length()), StandardCharsets.UTF_8.toString())).toString());
                    if (!destination.exists()) {
                        FileUtils.copyURLToFile(url, destination);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    public String getRootFolder() {
        ApplicationHome home = new ApplicationHome(SpringBootSampleApplication.class);
        return home.getDir().getAbsolutePath();
    }

    public String getDefaultIndexFolder() {
        ApplicationTemp temp = new ApplicationTemp(SpringBootSampleApplication.class);
        return temp.getDir(INDEX_FOLDER).getAbsolutePath();
    }

    public String getDefaultPath() {
        return Paths.get(getRootFolder(), STORAGE_FOLDER).toString();
    }
}
