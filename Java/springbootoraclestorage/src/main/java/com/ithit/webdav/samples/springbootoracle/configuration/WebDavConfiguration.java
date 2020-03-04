package com.ithit.webdav.samples.springbootoracle.configuration;

import com.ithit.webdav.samples.springbootoracle.SpringBootOracleSampleApplication;
import com.ithit.webdav.samples.springbootoracle.impl.CustomFolderGetHandler;
import com.ithit.webdav.samples.springbootoracle.impl.DataAccess;
import com.ithit.webdav.samples.springbootoracle.impl.SearchFacade;
import com.ithit.webdav.samples.springbootoracle.impl.WebDavEngine;
import com.ithit.webdav.samples.springbootoracle.websocket.SocketHandler;
import com.ithit.webdav.samples.springbootoracle.websocket.WebSocketServer;
import com.ithit.webdav.server.Engine;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.system.ApplicationTemp;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import static com.ithit.webdav.samples.springbootoracle.configuration.WebDavConfigurationProperties.INDEX_FOLDER;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EnableConfigurationProperties(WebDavConfigurationProperties.class)
@EnableWebMvc
@EnableWebSocket
@Configuration
public class WebDavConfiguration extends WebMvcConfigurerAdapter implements WebSocketConfigurer {
    final WebDavConfigurationProperties properties;
    final DataSource dataSource;
    @Value("classpath:handler/MyCustomHandlerPage.html")
    Resource customGetHandler;
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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registry.addResourceHandler("/wwwroot/**")
                .addResourceLocations("classpath:/wwwroot/", "/wwwroot/");
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(socketHandler, properties.getRootWebSocket()).setAllowedOrigins("*");
    }

    @Bean
    public WebDavEngine engine() {
        String license;
        try {
            license = FileUtils.readFileToString(new File(properties.getLicense()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            license = "";
        }
        final WebDavEngine webDavEngine = new WebDavEngine(license, properties.isShowExceptions());
        CustomFolderGetHandler handler = new CustomFolderGetHandler(webDavEngine.getResponseCharacterEncoding(), Engine.getVersion(), customGetHandler(), properties.getRootContext(), properties.getRootWebSocket());
        CustomFolderGetHandler handlerHead = new CustomFolderGetHandler(webDavEngine.getResponseCharacterEncoding(), Engine.getVersion(), customGetHandler(), properties.getRootContext(), properties.getRootWebSocket());
        handler.setPreviousHandler(webDavEngine.registerMethodHandler("GET", handler));
        handlerHead.setPreviousHandler(webDavEngine.registerMethodHandler("HEAD", handlerHead));
        String indexLocalPath = createIndexPath();
        DataAccess dataAccess = new DataAccess(webDavEngine, dataSource);
        webDavEngine.setDataAccess(dataAccess);
        if (indexLocalPath != null) {
            SearchFacade searchFacade = new SearchFacade(dataAccess, webDavEngine.getLogger());
            searchFacade.indexRootFolder(indexLocalPath, 2);
            webDavEngine.setSearchFacade(searchFacade);
        }
        webDavEngine.setWebSocketServer(new WebSocketServer(socketHandler.getSessions()));
        return webDavEngine;
    }

    @Bean
    public String customGetHandler() {
        return getStreamAsString(customGetHandler);
    }

    @SneakyThrows
    private String getStreamAsString(Resource customGetHandler) {
        try (InputStream is = customGetHandler.getInputStream()) {
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        }
    }

    /**
     * Creates index folder if not exists.
     *
     * @return Absolute location of index folder.
     */
    private String createIndexPath() {
        Path indexLocalPath = Paths.get(getDefaultIndexFolder());
        if (Files.notExists(indexLocalPath)) {
            try {
                Files.createDirectory(indexLocalPath);
            } catch (IOException e) {
                return null;
            }
        }
        return indexLocalPath.toString();
    }

    public String getDefaultIndexFolder() {
        ApplicationTemp temp = new ApplicationTemp(SpringBootOracleSampleApplication.class);
        return temp.getDir(INDEX_FOLDER).getAbsolutePath();
    }
}
