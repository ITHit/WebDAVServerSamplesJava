package com.ithit.webdav.samples.springbootsample.configuration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "webdav")
public class WebDavConfigurationProperties {
    public static final String WEBDAV_CONTEXT = "/webdav/";
    String license;
    boolean showExceptions;
    String rootFolder;
}
