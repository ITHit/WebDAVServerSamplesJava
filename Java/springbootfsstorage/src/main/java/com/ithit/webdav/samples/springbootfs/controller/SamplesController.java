package com.ithit.webdav.samples.springbootfs.controller;

import com.ithit.webdav.integration.servlet.HttpServletDavRequest;
import com.ithit.webdav.integration.servlet.HttpServletDavResponse;
import com.ithit.webdav.samples.springbootfs.impl.WebDavEngine;
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


@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RestController
public class SamplesController {

    WebDavEngine engine;

    @RequestMapping(path = "${webdav.rootContext}**", produces = MediaType.ALL_VALUE)
    public void webdav(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        performDavRequest(httpServletRequest, httpServletResponse);
    }

    @RequestMapping(path = "${webdav.rootContext}**", produces = MediaType.ALL_VALUE, method = {RequestMethod.OPTIONS})
    public void options(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        performDavRequest(httpServletRequest, httpServletResponse);
    }

    private void performDavRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        HttpServletDavRequest davRequest = new HttpServletDavRequest(httpServletRequest) {
            @Override
            public String getServerPath() {
                return "/";
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
