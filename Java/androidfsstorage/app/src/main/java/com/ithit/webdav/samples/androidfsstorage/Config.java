package com.ithit.webdav.samples.androidfsstorage;

import com.ithit.webdav.integration.android.AndroidServerConfig;
import com.ithit.webdav.samples.androidfsstorage.android.IDatabaseHandler;
import com.ithit.webdav.server.Logger;

import java.util.List;

/**
 * Extended Android config based on integration config.
 */
public class Config extends AndroidServerConfig {
    private final String serverName;
    private final int port;
    private final String license;
    private final String location;
    private final List<String> mainPage;
    private final List<String> errorPage;
    private final List<String> testPage;
    private final List<String> browserPage;
    private final List<String> css;
    private final List<String> jsGrid;
    private final List<String> jsUploader;
    private final List<String> jsWebSocket;
    private final List<String> jsClient;
    private final byte[] windowsOpener;
    private final byte[] depOpener;
    private final byte[] pkgOpener;
    private final byte[] rpmOpener;
    private final Logger logger;
    private IDatabaseHandler databaseHandler;

    public Config(String serverName, int port, String license, String location, List<String> mainPage, List<String> errorPage, List<String> testPage, List<String> browserPage,
                  List<String> css, List<String> jsGrid, List<String> jsUploader, List<String> jsWebSocket, List<String> jsClient, byte[] windowsOpener, byte[] depOpener, byte[] pkgOpener, byte[] rpmOpener, Logger logger, IDatabaseHandler databaseHandler) {
        this.serverName = serverName;
        this.port = port;
        this.license = license;
        this.location = location;
        this.mainPage = mainPage;
        this.errorPage = errorPage;
        this.testPage = testPage;
        this.browserPage = browserPage;
        this.css = css;
        this.jsGrid = jsGrid;
        this.jsUploader = jsUploader;
        this.jsWebSocket = jsWebSocket;
        this.jsClient = jsClient;
        this.windowsOpener = windowsOpener;
        this.depOpener = depOpener;
        this.pkgOpener = pkgOpener;
        this.rpmOpener = rpmOpener;
        this.logger = logger;
        this.databaseHandler = databaseHandler;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public int getPort() {
        return port;
    }

    String getLicense() {
        return license;
    }

    String getLocation() {
        return location;
    }

    List<String> getMainPage() {
        return mainPage;
    }

    List<String> getErrorPage() {
        return errorPage;
    }

    List<String> getTestPage() {
        return testPage;
    }

    List<String> getBrowserPage() {
        return browserPage;
    }

    public List<String> getCss() {
        return css;
    }

    public List<String> getJsGrid() {
        return jsGrid;
    }

    public List<String> getJsUploader() {
        return jsUploader;
    }

    public List<String> getJsWebSocket() {
        return jsWebSocket;
    }

    public List<String> getJsClient() {
        return jsClient;
    }

    public byte[] getWindowsOpener() {
        return windowsOpener;
    }

    public byte[] getDepOpener() {
        return depOpener;
    }

    public byte[] getPkgOpener() {
        return pkgOpener;
    }

    public byte[] getRpmOpener() {
        return rpmOpener;
    }

    Logger getLogger() {
        return logger;
    }

    IDatabaseHandler getDatabaseHandler() {
        return databaseHandler;
    }
}
