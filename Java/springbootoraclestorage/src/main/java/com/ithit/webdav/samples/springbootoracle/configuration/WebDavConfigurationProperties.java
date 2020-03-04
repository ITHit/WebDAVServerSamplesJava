package com.ithit.webdav.samples.springbootoracle.configuration;

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
    static String INDEX_FOLDER = "Index";
    String license;
    boolean showExceptions;
    String rootContext;
    String rootWebSocket;
}
