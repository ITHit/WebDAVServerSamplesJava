package com.ithit.webdav.samples.springbootfs.msofba;

import com.ithit.webdav.samples.springbootfs.configuration.WebDavConfigurationProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Component
@ConditionalOnProperty("azure.activedirectory.tenant-id")
public class MSOFBAFilter extends GenericFilterBean {

    final RequestCache cache = new HttpSessionRequestCache();
    final WebDavConfigurationProperties properties;
    @Value("${server.error.path:/error}")
    String errorPath;

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        if (!applyMSOFBAResponse(req, res)) {
            chain.doFilter(request, response);
        }
    }

    private boolean applyMSOFBAResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!isAuthenticated() && isOFBAAccepted(request)) {
            // We need to save original request so the office after authentication will redirect to it.
            saveOriginalRequest(request, response);
            response.setHeader("X-FORMS_BASED_AUTH_REQUIRED", String.format("%s://%s:%s/oauth2/authorization/azure", request.getScheme(), request.getServerName(), request.getLocalPort()));
            response.setHeader("X-FORMS_BASED_AUTH_RETURN_URL", String.format("%s://%s:%s%s", request.getScheme(), request.getServerName(), request.getLocalPort(), properties.getRootContext()));
            response.setHeader("FORMS_BASED_AUTH_DIALOG_SIZE", "800x600");
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return true;
        }
        return false;
    }

    private boolean isAuthenticated() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    private boolean isOFBAAccepted(HttpServletRequest request) {
        // In case application provided X-FORMS_BASED_AUTH_ACCEPTED header
        String ofbaAccepted = request.getHeader("X-FORMS_BASED_AUTH_ACCEPTED");
        if ((ofbaAccepted != null) && ofbaAccepted.equals("T")) {
            return true;
        }

        // Microsoft Office does not submit X-FORMS_BASED_AUTH_ACCEPTED header, but it still supports MS-OFBA,
        // Microsoft Office includes "Microsoft Office" string into User-Agent header
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null && userAgent.contains("Microsoft Office");
    }

    /**
     * Saves the original request in-memory cache.
     */
    private void saveOriginalRequest(HttpServletRequest request, HttpServletResponse response) {
        if (!request.getRequestURI().equals(errorPath)) {
            cache.saveRequest(request, response);
        }
    }
}
