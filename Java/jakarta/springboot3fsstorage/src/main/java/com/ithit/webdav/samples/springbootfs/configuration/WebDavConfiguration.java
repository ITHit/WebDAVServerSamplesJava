package com.ithit.webdav.samples.springbootfs.configuration;

import com.ithit.webdav.integration.extendedattributes.ExtendedAttributesExtension;
import com.ithit.webdav.samples.springbootfs.common.ResourceReader;
import com.ithit.webdav.samples.springbootfs.impl.CustomFolderGetHandler;
import com.ithit.webdav.samples.springbootfs.impl.SearchFacade;
import com.ithit.webdav.samples.springbootfs.impl.WebDavEngine;
import com.ithit.webdav.integration.spring.websocket.HandshakeHeadersInterceptor;
import com.ithit.webdav.integration.spring.websocket.SocketHandler;
import com.ithit.webdav.integration.spring.websocket.WebSocketServer;
import com.ithit.webdav.server.Engine;
import com.ithit.webdav.server.util.StringUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EnableConfigurationProperties(WebDavConfigurationProperties.class)
@EnableWebSocket
@Configuration
public class WebDavConfiguration extends WebMvcConfigurationSupport implements WebSocketConfigurer {
    final WebDavConfigurationProperties properties;
    final ResourceReader resourceReader;
    @Value("classpath:handler/MyCustomHandlerPage.html")
    Resource customGetHandler;
    @Value("classpath:handler/attributesErrorPage.html")
    Resource errorPage;
    private final SocketHandler socketHandler = new SocketHandler();

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Collections.singletonList("*"));
        configuration.setAllowedMethods(Arrays.asList("PROPFIND", "PROPPATCH", "COPY", "MOVE", "DELETE", "MKCOL", "LOCK", "UNLOCK", "PUT", "GETLIB", "VERSION-CONTROL", "CHECKIN", "CHECKOUT", "UNCHECKOUT", "REPORT", "UPDATE", "CANCELUPLOAD", "HEAD", "OPTIONS", "GET", "POST"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // -1 will allow to process static resources if main controller is running on the root.
        registry.setOrder(-1);
        registry.addResourceHandler("/wwwroot/**")
                .addResourceLocations("classpath:/wwwroot/", "/wwwroot/");
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(socketHandler, properties.getRootWebSocket()).addInterceptors(new HandshakeHeadersInterceptor()).setAllowedOrigins("*");
    }

    @RequestScope
    @Bean
    public WebDavEngine engine() {
        String rootLocalPath = rootLocalPath();
        String license;
        try {
            license = FileUtils.readFileToString(new File(properties.getLicense()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            license = "";
        }
        final WebDavEngine webDavEngine = new WebDavEngine(license, rootLocalPath, properties.isShowExceptions(), properties.getRootContext());
        final boolean extendedAttributesSupported = ExtendedAttributesExtension.isExtendedAttributesSupported(rootLocalPath);
        CustomFolderGetHandler handler = new CustomFolderGetHandler(webDavEngine.getResponseCharacterEncoding(), Engine.getVersion(), extendedAttributesSupported, customGetHandler(), errorPage(), properties.getRootContext());
        CustomFolderGetHandler handlerHead = new CustomFolderGetHandler(webDavEngine.getResponseCharacterEncoding(), Engine.getVersion(), extendedAttributesSupported, customGetHandler(), errorPage(), properties.getRootContext());
        handler.setPreviousHandler(webDavEngine.registerMethodHandler("GET", handler));
        handlerHead.setPreviousHandler(webDavEngine.registerMethodHandler("HEAD", handlerHead));
        String indexLocalPath = createIndexPath();
        if (rootLocalPath != null && indexLocalPath != null) {
            SearchFacade searchFacade = SearchFacade.getInstance(webDavEngine, webDavEngine.getLogger());
            searchFacade.indexRootFolder(rootLocalPath, indexLocalPath, 2);
            webDavEngine.setSearchFacade(searchFacade);
        }
        webDavEngine.setWebSocketServer(new WebSocketServer(socketHandler.getSessions()));
        return webDavEngine;
    }

    @Bean
    public String rootLocalPath() {
        return checkRootPath(properties.getRootFolder(), Paths.get(properties.getRootFolder()).normalize().toString());
    }

    @Bean
    public String customGetHandler() {
        return getStreamAsString(customGetHandler);
    }

    @Bean
    public String errorPage() {
        return getStreamAsString(errorPage);
    }

    @SneakyThrows
    private String getStreamAsString(Resource customGetHandler) {
        try (InputStream is = customGetHandler.getInputStream()) {
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        }
    }

    private String checkRootPath(String rootPath, String path) {
        String realPath = resourceReader.getRootFolder();
        if (StringUtil.isNullOrEmpty(rootPath)) {
            path = createDefaultPath();
        } else {
            if (Files.exists(Paths.get(rootPath))) {
                return path;
            }
            try {
                Path relative = Paths.get(realPath, rootPath);
                if (Files.exists(relative)) {
                    path = relative.toString();
                } else {
                    path = createDefaultPath();
                }
            } catch (Exception ignored) {
                path = createDefaultPath();
            }
        }
        return path;
    }

    private String createDefaultPath() {
        return resourceReader.getDefaultPath();
    }

    /**
     * Creates index folder if not exists.
     *
     * @return Absolute location of index folder.
     */
    private String createIndexPath() {
        Path indexLocalPath = Paths.get(resourceReader.getDefaultIndexFolder());
        if (Files.notExists(indexLocalPath)) {
            try {
                Files.createDirectory(indexLocalPath);
            } catch (IOException e) {
                return null;
            }
        }
        return indexLocalPath.toString();
    }
}
