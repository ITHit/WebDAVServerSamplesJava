package com.ithit.webdav.samples.springbootsample.common;

import com.ithit.webdav.samples.springbootsample.SpringBootSampleApplication;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.system.ApplicationHome;
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

    public static final String STORAGE_FOLDER = "Storage";
    private final File root = new File(getRootFolder());
    private static final String LOCAL_STATIC_RESOURCES_FOLDER_PATH = "classpath:" + STORAGE_FOLDER + "/**";
    final ResourceLoader resourceLoader;

    public void readFiles() {
        Resource[] resources;
        try {
            resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                    .getResources(LOCAL_STATIC_RESOURCES_FOLDER_PATH);
            for (Resource resource : resources) {
                if (resource.exists() & resource.isReadable()) {
                    URL url = resource.getURL();
                    String urlString = url.toExternalForm();
                    String targetName = urlString.substring(urlString.indexOf("Storage"));
                    File destination = new File(root, URLDecoder.decode(targetName, StandardCharsets.UTF_8.toString()));
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

    public String getDefaultPath() {
        return Paths.get(getRootFolder(), STORAGE_FOLDER).toString();
    }
}
