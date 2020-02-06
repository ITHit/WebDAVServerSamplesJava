package com.ithit.webdav.samples.springboot.controller;

import com.ithit.webdav.samples.springboot.configuration.WebDavConfigurationProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static com.ithit.webdav.samples.springboot.configuration.WebDavConfigurationProperties.ROOT_ATTRIBUTE;


@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
@Order(1)
public class DavFilter implements Filter {

    WebDavConfigurationProperties properties;

    @Override
    public void doFilter(
    ServletRequest request,
    ServletResponse response,
    FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        if ((req.getMethod().equalsIgnoreCase("PROPFIND") || req.getMethod().equalsIgnoreCase("OPTIONS"))
                && properties.getRootContext().contains(req.getRequestURI())
                && properties.getRootContext().length() - 2 > req.getRequestURI().length()) {
            request.setAttribute(ROOT_ATTRIBUTE, req.getRequestURI());
            request.getRequestDispatcher(properties.getRootContext()).include(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}
