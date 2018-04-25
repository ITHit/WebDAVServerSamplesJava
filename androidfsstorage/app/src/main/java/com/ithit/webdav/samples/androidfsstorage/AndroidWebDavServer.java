package com.ithit.webdav.samples.androidfsstorage;

import com.ithit.webdav.integration.android.AndroidDavRequest;
import com.ithit.webdav.integration.android.AndroidDavResponse;
import com.ithit.webdav.samples.androidfsstorage.extendedattributes.ExtendedAttributesExtension;
import com.ithit.webdav.server.exceptions.DavException;
import com.ithit.webdav.server.exceptions.WebDavStatus;

import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * HTTP server based on NanoHTTPD, whoch handles all WebDAV requests.
 */
public class AndroidWebDavServer extends NanoHTTPD {

    private Config config;
    private static String rootPath = "";
    private static boolean supportsUserDefinedAttributes;

    private void init() {
        supportsUserDefinedAttributes = ExtendedAttributesExtension.getExtendedAttributeSupport(config.getDatabaseHandler()).isExtendedAttributeSupported(rootPath);
    }

    public AndroidWebDavServer(Config config) throws IOException {
        super(config.getServerName(), config.getPort());
        this.config = config;
        rootPath = config.getLocation();
        init();
        start(60000, false);
    }

    /**
     * Returns root folder for the WebDav.
     *
     * @return Root folder.
     */
    static String getRootLocalPath() {
        return rootPath;
    }

    /**
     * Returns whether file system registered for the root folder supports User Defined Attributes.
     *
     * @return <b>True</b> if file system registered for root folder support User Defined Attributes,
     * <b>false</b> otherwise.
     */
    static boolean isSupportsUserDefinedAttributes() {
        return supportsUserDefinedAttributes;
    }

    @Override
    public Response serve(IHTTPSession session) {
        WebDavEngine engine = new WebDavEngine(config.getLogger(), config.getLicense());
        AndroidDavRequest davRequest = new AndroidDavRequest(session, config);
        AndroidDavResponse davResponse = new AndroidDavResponse();
        CustomFolderGetHandler handler = new CustomFolderGetHandler(engine.getResponseCharacterEncoding(), engine.getVersion(), config);
        CustomFolderGetHandler handlerHead = new CustomFolderGetHandler(engine.getResponseCharacterEncoding(), engine.getVersion(), config);
        handler.setPreviousHandler(engine.registerMethodHandler("GET", handler));
        handlerHead.setPreviousHandler(engine.registerMethodHandler("HEAD", handlerHead));
        engine.setServletRequest(davRequest);

        try {
            engine.service(davRequest, davResponse);
        } catch (DavException e) {
            if (e.getStatus() == WebDavStatus.INTERNAL_ERROR) {
                config.getLogger().logError("Exception during request processing", e);
            }
        } catch (IOException e) {
            config.getLogger().logError(e.getLocalizedMessage(), e);
        }
        Response response = NanoHTTPD.newFixedLengthResponse(new Status(davResponse.getStatus().getCode(), davResponse.getStatus().getDescription()), davResponse.getContentType(), davResponse.getInputStream(), davResponse.getLength());
        for (Map.Entry<String, String> en: davResponse.getHeaders().entrySet()) {
            response.addHeader(en.getKey(), en.getValue());
        }
        try {
            davRequest.getInputStream().close();
        } catch (IOException e) {
            config.getLogger().logError(e.getLocalizedMessage(), e);
        }
        return response;
    }

    /**
     * Status implementation for NanoHTTPD. Please note getDescription method implementation.
     */
    private static class Status implements Response.IStatus {
        private int code;
        private String description;

        private Status(int code, String description) {
            this.code = code;
            this.description = description;
        }

        @Override
        public String getDescription() {
            return "" + this.code + " " + this.description;
        }

        @Override
        public int getRequestStatus() {
            return code;
        }
    }
}
