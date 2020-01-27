package com.ithit.webdav.samples.springbootsample.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static com.ithit.webdav.samples.springbootsample.configuration.WebDavConfigurationProperties.WEBDAV_CONTEXT;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
@Order(1)
public class DavFilter implements Filter {

    @Override
    public void doFilter(
    ServletRequest request,
    ServletResponse response,
    FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        if ((req.getMethod().equalsIgnoreCase("PROPFIND") || req.getMethod().equalsIgnoreCase("OPTIONS")) && WEBDAV_CONTEXT.length() - 2 > req.getRequestURI().length()) {
            request.setAttribute("root", true);
            request.getRequestDispatcher(WEBDAV_CONTEXT).include(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}
