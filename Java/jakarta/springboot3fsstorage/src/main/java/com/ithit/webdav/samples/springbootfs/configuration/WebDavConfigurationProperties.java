package com.ithit.webdav.samples.springbootfs.configuration;

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
    String license;
    boolean showExceptions;
    String rootFolder;
    String rootContext;
    String rootWebSocket;
}
