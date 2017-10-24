package com.ithit.webdav.samples.androidfsstorage.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Sends and outputs logs on activity.
 */
public class NanoBroadcastReceiver extends BroadcastReceiver {

    public static final String LOG_OUTPUT = "LOG_OUTPUT";

    /**
     * Activity instance (is used to update view).
     */
    private MainActivity activity;

    /**
     * Creates new instance of this class.
     * @param activity Activity instance to update textview.
     */
    public NanoBroadcastReceiver(MainActivity activity) {
        this.activity = activity;
    }

    /**
     * Creates new instance of this class.
     */
    public NanoBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        activity.output(intent.getStringExtra("message"));
    }
}
