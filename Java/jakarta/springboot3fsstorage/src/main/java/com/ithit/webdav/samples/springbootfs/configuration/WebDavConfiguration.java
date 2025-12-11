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

import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
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
@EnableWebSecurity
@Configuration
public class WebDavConfiguration extends WebMvcConfigurationSupport implements WebSocketConfigurer {
    final WebDavConfigurationProperties properties;
    final ResourceReader resourceReader;
    @Value("classpath:handler/MyCustomHandlerPage.html")
    Resource customGetHandler;
    @Value("classpath:handler/attributesErrorPage.html")
    Resource errorPage;
    private final SocketHandler socketHandler = new SocketHandler();
    private volatile SearchFacade searchFacade;

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
    public HttpFirewall allowWebDavHttpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        firewall.setAllowBackSlash(true);
        firewall.setAllowedHttpMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH",
                "PROPFIND", "PROPPATCH", "COPY", "MOVE", "MKCOL", "LOCK", "UNLOCK",
                "REPORT", "CHECKIN", "CHECKOUT", "UNCHECKOUT", "VERSION-CONTROL",
                "UPDATE", "GETLIB", "CANCELUPLOAD", "SEARCH"
        ));
        return firewall;
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
    public WebDavEngine engine(String rootLocalPath,
            @Qualifier("customGetHandler") String customGetHandlerContent,
            @Qualifier("errorPage") String errorPageContent) {
        String license = readLicense();
        final WebDavEngine webDavEngine = new WebDavEngine(license, rootLocalPath, properties.isShowExceptions(), properties.getRootContext());

        registerHandlers(webDavEngine, rootLocalPath, customGetHandlerContent, errorPageContent);

        // Використовуємо єдиний екземпляр SearchFacade
        initSearchFacade(webDavEngine, rootLocalPath);
        if (this.searchFacade != null) {
            webDavEngine.setSearchFacade(this.searchFacade);
        }

        webDavEngine.setWebSocketServer(new WebSocketServer(socketHandler.getSessions()));
        return webDavEngine;
    }

    /**
     * Thread-safe lazy initialization of SearchFacade.
     * Ensures indexing runs only once, not per request.
     */
    private void initSearchFacade(WebDavEngine engine, String rootLocalPath) {
        if (this.searchFacade == null) {
            synchronized (this) {
                if (this.searchFacade == null) {
                    String indexLocalPath = createIndexPath();
                    if (rootLocalPath != null && indexLocalPath != null) {
                        SearchFacade facade = SearchFacade.getInstance(engine, engine.getLogger());
                        facade.indexRootFolder(rootLocalPath, indexLocalPath, 2);
                        this.searchFacade = facade;
                    }
                }
            }
        }
    }

    @PreDestroy
    public void onShutdown() {
        if (this.searchFacade != null && this.searchFacade.getIndexer() != null) {
            System.out.println("Stopping SearchFacade and releasing index locks...");
            this.searchFacade.getIndexer().stop();
        }
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        if (properties.isCookieAuthEnabled()) {
            http
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers(properties.getRootContext() + "**").authenticated()
                            .anyRequest().permitAll()
                    )
                    // If you want 401 response code, otherwise remove this block for 403
                    .exceptionHandling(exception -> exception
                            .authenticationEntryPoint((request, response, authException) -> {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                            })
                    )
                    .addFilterBefore(new CookieAuthenticationFilter(properties.getAuthCookieName()), UsernamePasswordAuthenticationFilter.class);
        } else {
            http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        }

        return http.build();
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

    private String readLicense() {
        try {
            return FileUtils.readFileToString(new File(properties.getLicense()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private void registerHandlers(WebDavEngine engine, String rootLocalPath, String customGetHandlerContent, String errorPageContent) {
        final boolean extendedAttributesSupported = ExtendedAttributesExtension.isExtendedAttributesSupported(rootLocalPath);

        CustomFolderGetHandler handler = createFolderHandler(engine, extendedAttributesSupported, customGetHandlerContent, errorPageContent);
        CustomFolderGetHandler handlerHead = createFolderHandler(engine, extendedAttributesSupported, customGetHandlerContent, errorPageContent);

        handler.setPreviousHandler(engine.registerMethodHandler("GET", handler));
        handlerHead.setPreviousHandler(engine.registerMethodHandler("HEAD", handlerHead));
    }

    private CustomFolderGetHandler createFolderHandler(WebDavEngine engine, boolean extAttrSupported, String handlerContent, String errorContent) {
        return new CustomFolderGetHandler(
                engine.getResponseCharacterEncoding(),
                Engine.getVersion(),
                extAttrSupported,
                handlerContent,
                errorContent,
                properties.getRootContext()
        );
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
