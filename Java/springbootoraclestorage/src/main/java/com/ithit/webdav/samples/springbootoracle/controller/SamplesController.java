package com.ithit.webdav.samples.springbootoracle.controller;

import com.ithit.webdav.integration.servlet.HttpServletDavRequest;
import com.ithit.webdav.integration.servlet.HttpServletDavResponse;
import com.ithit.webdav.samples.springbootoracle.impl.DataAccess;
import com.ithit.webdav.samples.springbootoracle.impl.WebDavEngine;
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
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintStream;


@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RestController
public class SamplesController {

    WebDavEngine engine;
    DataSource dataSource;

    @RequestMapping(path = "${webdav.rootContext}**", produces = MediaType.ALL_VALUE, headers = "Connection!=Upgrade")
    public void webdav(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        performDavRequest(httpServletRequest, httpServletResponse);
    }

    @RequestMapping(path = "${webdav.rootContext}**", produces = MediaType.ALL_VALUE, method = {RequestMethod.OPTIONS}, headers = "Connection!=Upgrade")
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
        DataAccess dataAccess = new DataAccess(engine, dataSource);
        try {
            engine.setDataAccess(dataAccess);
            engine.service(davRequest, davResponse);
            dataAccess.commit();
        } catch (DavException e) {
            if (e.getStatus() == WebDavStatus.INTERNAL_ERROR) {
                engine.getLogger().logError("Exception during request processing", e);
                if (engine.isShowExceptions())
                    e.printStackTrace(new PrintStream(davResponse.getOutputStream()));
            }
        } finally {
            dataAccess.closeConnection();
        }
    }

}
