package com.ithit.webdav.samples.springboots3.configuration;

import com.ithit.webdav.samples.springboots3.impl.CustomFolderGetHandler;
import com.ithit.webdav.samples.springboots3.impl.WebDavEngine;
import com.ithit.webdav.samples.springboots3.s3.DataClient;
import com.ithit.webdav.samples.springboots3.websocket.SocketHandler;
import com.ithit.webdav.samples.springboots3.websocket.WebSocketServer;
import com.ithit.webdav.server.Engine;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EnableConfigurationProperties(WebDavConfigurationProperties.class)
@EnableWebSocket
@Configuration
public class WebDavConfiguration extends WebMvcConfigurationSupport implements WebSocketConfigurer {
    final WebDavConfigurationProperties properties;
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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // -1 will allow to process static resources if main controller is running on the root.
        registry.setOrder(-1);
        registry.addResourceHandler("/wwwroot/**")
                .addResourceLocations("classpath:/wwwroot/", "/wwwroot/");
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(socketHandler, properties.getRootWebSocket()).setAllowedOrigins("*");
    }

    @RequestScope
    @Bean
    public WebDavEngine engine() {
        String license;
        try {
            license = FileUtils.readFileToString(new File(properties.getLicense()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            license = "";
        }
        final WebDavEngine webDavEngine = new WebDavEngine(license, properties.isShowExceptions(), dataClient());
        CustomFolderGetHandler handler = new CustomFolderGetHandler(webDavEngine.getResponseCharacterEncoding(), Engine.getVersion(), true, customGetHandler(), errorPage(), properties.getRootContext());
        CustomFolderGetHandler handlerHead = new CustomFolderGetHandler(webDavEngine.getResponseCharacterEncoding(), Engine.getVersion(), true, customGetHandler(), errorPage(), properties.getRootContext());
        handler.setPreviousHandler(webDavEngine.registerMethodHandler("GET", handler));
        handlerHead.setPreviousHandler(webDavEngine.registerMethodHandler("HEAD", handlerHead));
        webDavEngine.setWebSocketServer(new WebSocketServer(socketHandler.getSessions()));
        return webDavEngine;
    }

    @Bean
    public String customGetHandler() {
        return getStreamAsString(customGetHandler);
    }

    @Bean
    public String errorPage() {
        return getStreamAsString(errorPage);
    }

    @Bean
    public DataClient dataClient() {
        return new DataClient(s3Client(), properties.getS3().getBucket(), properties.getRootContext());
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials()))
                .region(Region.of(properties.getS3().getRegion()))
                .build();
    }

    @Bean
    public AwsCredentials awsCredentials() {
        return AwsBasicCredentials.create(properties.getS3().getAccessKey(), properties.getS3().getSecretAccessKey());
    }

    @SneakyThrows
    private String getStreamAsString(Resource customGetHandler) {
        try (InputStream is = customGetHandler.getInputStream()) {
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        }
    }
}
