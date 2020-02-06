package com.ithit.webdav.samples.springboot.controller;

import com.ithit.webdav.integration.servlet.HttpServletDavRequest;
import com.ithit.webdav.integration.servlet.HttpServletDavResponse;
import com.ithit.webdav.samples.springboot.configuration.WebDavConfigurationProperties;
import com.ithit.webdav.samples.springboot.impl.WebDavEngine;
import com.ithit.webdav.server.exceptions.DavException;
import com.ithit.webdav.server.exceptions.WebDavStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintStream;

import static com.ithit.webdav.samples.springboot.configuration.WebDavConfigurationProperties.ROOT_ATTRIBUTE;


@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RestController
public class SamplesController {

    WebDavEngine engine;
    WebDavConfigurationProperties properties;

    @RequestMapping(path = "${webdav.rootContext}**", produces = MediaType.ALL_VALUE)
    public void webdav(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        performDavRequest(httpServletRequest, httpServletResponse);
    }

    @RequestMapping(path = "${webdav.rootContext}**", produces = MediaType.ALL_VALUE, method = {RequestMethod.OPTIONS})
    public void options(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        performDavRequest(httpServletRequest, httpServletResponse);
    }

    private void performDavRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        final Object originalRequest = httpServletRequest.getAttribute(ROOT_ATTRIBUTE);
        boolean isRoot = originalRequest != null;
        HttpServletDavRequest davRequest = new HttpServletDavRequest(httpServletRequest) {
            @Override
            public String getServerPath() {
                return isRoot ? originalRequest.toString() : properties.getRootContext();
            }

            @Override
            public String getRequestURI() {
                return isRoot ? originalRequest.toString() : super.getRequestURI();
            }
        };
        HttpServletDavResponse davResponse = new HttpServletDavResponse(httpServletResponse);
        try {
            engine.setServletRequest(davRequest);
            engine.service(davRequest, davResponse);
        } catch (DavException e) {
            if (e.getStatus() == WebDavStatus.INTERNAL_ERROR) {
                engine.getLogger().logError("Exception during request processing", e);
                if (engine.isShowExceptions())
                    e.printStackTrace(new PrintStream(davResponse.getOutputStream()));
            }
        }
    }

}
