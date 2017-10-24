package com.ithit.webdav.samples.androidfsstorage;

import android.app.Service;
import android.content.Intent;

import com.ithit.webdav.integration.android.AndroidLoggingContext;
import com.ithit.webdav.samples.androidfsstorage.android.NanoBroadcastReceiver;

/**
 * Represents logging messages to the application view on devices, running Android.
 */
public class AndroidLogContext extends AndroidLoggingContext {

    /**
     * Logging intent service.
     */
    private Service intentService;

    /**
     * Creates new instance of this class.
     * @param intentService Logging intent service.
     */
    public AndroidLogContext(Service intentService) {
        this.intentService = intentService;
    }

    /**
     * Logs message to the view.
     * @param message Text message.
     */
    @Override
    public void log(String message) {
        Intent broadcastIntent = new Intent(NanoBroadcastReceiver.LOG_OUTPUT);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra("message", message);
        intentService.sendBroadcast(broadcastIntent);
    }

    /**
     * Logs error message to the view.
     * @param message Text message.
     * @param throwable Error.
     */
    @Override
    public void log(String message, Throwable throwable) {
        Intent broadcastIntent = new Intent(NanoBroadcastReceiver.LOG_OUTPUT);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        StringBuilder trace = new StringBuilder();
        for (StackTraceElement ste: throwable.getStackTrace()) {
            trace.append(ste.toString());
        }
        broadcastIntent.putExtra("message", message + "\n\n" + trace);
        intentService.sendBroadcast(broadcastIntent);
    }
}
