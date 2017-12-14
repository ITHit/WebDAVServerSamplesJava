package com.ithit.webdav.samples.androidfsstorage.android;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.ithit.webdav.samples.androidfsstorage.AndroidNanoLogger;
import com.ithit.webdav.samples.androidfsstorage.AndroidWebDavServer;
import com.ithit.webdav.samples.androidfsstorage.Config;
import com.ithit.webdav.server.Logger;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Configures and starts {@link AndroidWebDavServer}.
 */
public class NanoIntentService extends Service {

    private AndroidWebDavServer androidWebDavServer;

    /**
     * Creates new instance of this class.
     */
    public NanoIntentService() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger logger = new AndroidNanoLogger(this);

        InputStream getPage = null;
        InputStream errorPage = null;
        InputStream testPage = null;
        InputStream browserPage = null;
        try {
            getPage = getApplicationContext().getAssets().open("MyCustomHandlerPage.html");
            errorPage = getApplicationContext().getAssets().open("attributesErrorPage.html");
            testPage = getApplicationContext().getAssets().open("wwwroot/AjaxIntegrationTests.html");
            browserPage = getApplicationContext().getAssets().open("wwwroot/AjaxFileBrowser.html");
            List<String> getPageLines = IOUtils.readLines(getPage, StandardCharsets.UTF_8);
            List<String> errorPageLines = IOUtils.readLines(errorPage, StandardCharsets.UTF_8);
            List<String> testPageLines = IOUtils.readLines(testPage, StandardCharsets.UTF_8);
            List<String> browserPageLines = IOUtils.readLines(browserPage, StandardCharsets.UTF_8);
            AndroidConfigurationHelper configurationHelper = new AndroidConfigurationHelper(getAssets(), "webdavsettings.json");
            String license = configurationHelper.getJsonValueCollection().get("License");
            // Copy storage files directory from application assets to application files folder.
            initUserStorage("Storage");
            String fileLocation = getApplicationContext().getFilesDir().getPath() + "/Storage";
            Bundle extras = intent.getExtras();
            if (extras != null) {
                fileLocation = extras.getString(MainActivity.FILE_LOCATION, fileLocation);
            }
            String ipAddress = getIPAddress();
            androidWebDavServer = new AndroidWebDavServer(new Config(ipAddress, 8181, license, fileLocation, getPageLines, errorPageLines, testPageLines, browserPageLines, logger, new DatabaseHandlerImpl(this)));
            logger.logDebug("To access WebDAV server open browser at any machine in your network at the following URL: " +
                    "http://" + ipAddress + ":8181");
        } catch (Exception e) {
            logger.logError(e.getLocalizedMessage(), e);
        } finally {
            IOUtils.closeQuietly(getPage);
            IOUtils.closeQuietly(errorPage);
            IOUtils.closeQuietly(testPage);
            IOUtils.closeQuietly(browserPage);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        androidWebDavServer.stop();
    }

    private void initUserStorage(String path) {
        AssetManager assetManager = this.getAssets();
        String assets[];
        try {
            assets = assetManager.list(path);
            if (assets.length == 0) {
                copyFileFromAssets(path);
            } else {
                String fullPath = getApplicationContext().getFilesDir().getPath() + "/" + path;
                File dir = new File(fullPath);
                if (!dir.exists())
                    dir.mkdir();
                for (String asset : assets) {
                    initUserStorage(path + "/" + asset);
                }
            }
        } catch (IOException ex) {
            Log.e("tag", "I/O Exception", ex);
        }
    }

    private void copyFileFromAssets(String filename) {
        AssetManager assetManager = this.getAssets();
        InputStream in;
        OutputStream out;
        try {
            String newFileName = getApplicationContext().getFilesDir().getPath() + "/" + filename;
            in = assetManager.open(filename);
            out = new FileOutputStream(newFileName);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) {
            Log.e("tag", e.getMessage());
        }

    }

    private static String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':')<0;
                        if (isIPv4)
                            return sAddr;
                    }
                }
            }
        } catch (Exception ignored) { }
        return "";
    }
}
