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
    private final Logger logger;
    private IDatabaseHandler databaseHandler;

    public Config(String serverName, int port, String license, String location, List<String> mainPage, List<String> errorPage, List<String> testPage, List<String> browserPage, Logger logger, IDatabaseHandler databaseHandler) {
        this.serverName = serverName;
        this.port = port;
        this.license = license;
        this.location = location;
        this.mainPage = mainPage;
        this.errorPage = errorPage;
        this.testPage = testPage;
        this.browserPage = browserPage;
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

    Logger getLogger() {
        return logger;
    }

    IDatabaseHandler getDatabaseHandler() {
        return databaseHandler;
    }
}
