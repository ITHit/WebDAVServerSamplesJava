package com.ithit.webdav.samples.springbootfs.common;

import com.ithit.webdav.samples.springbootfs.SpringBoot3SampleApplication;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.system.ApplicationTemp;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@FieldDefaults(level = PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Component
public class ResourceReader {

    private static final String STORAGE_FOLDER = "Storage";
    private static final String INDEX_FOLDER = "Index";

    public String getRootFolder() {
        ApplicationHome home = new ApplicationHome(SpringBoot3SampleApplication.class);
        return home.getDir().getAbsolutePath();
    }

    public String getDefaultIndexFolder() {
        ApplicationTemp temp = new ApplicationTemp(SpringBoot3SampleApplication.class);
        return temp.getDir(INDEX_FOLDER).getAbsolutePath();
    }

    public String getDefaultPath() {
        return Paths.get(getRootFolder(), STORAGE_FOLDER).toString();
    }
}
