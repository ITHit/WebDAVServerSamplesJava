package com.ithit.webdav.samples.androidfsstorage.android;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String FILE_LOCATION = "fileLocation";
    /**
     * BroadcastReceiver instance, which outputs logs on the view.
     */
    private NanoBroadcastReceiver receiver;
    private TextView label;
    private Intent nanoIntent;
    private Button mapVideoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        label = findViewById(R.id.logOutput);
        Button clearButton = findViewById(R.id.btn_clearLog);
        clearButton.setOnClickListener(this);
        mapVideoButton = findViewById(R.id.btn_mapVideo);
        mapVideoButton.setOnClickListener(this);
        nanoIntent = new Intent(this, NanoIntentService.class);
        startService(nanoIntent);
        registerBroadcastReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        new DatabaseHandlerImpl(this).deleteAll();
    }

    /**
     * Registers {@link android.content.BroadcastReceiver} in the application.
     */
    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter(NanoBroadcastReceiver.LOG_OUTPUT);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new NanoBroadcastReceiver(this);
        registerReceiver(receiver, filter);
    }

    /**
     * Outputs message to the view.
     * @param message Text for output.
     */
    public void output(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                label.append(message + "\n\n");
                final ScrollView scrollView = findViewById(R.id.scrollOutput);
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    /**
     * Handles buttons clicks.
     * @param v Click source.
     */
    @Override
    public void onClick(final View v) {
        if (v.getId() == R.id.btn_clearLog) {
            label.setText("");
        }
        if (v.getId() == R.id.btn_mapVideo) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this,
                        new String[] {
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        1);
            } else {
                restartService();
            }
        }
    }

    /**
     * Restarts the intent service.
     */
    private void restartService() {
        if (mapVideoButton.getText().equals(getResources().getString(R.string.map_media))) {
            nanoIntent.putExtra(FILE_LOCATION, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath());
            mapVideoButton.setText(getResources().getString(R.string.map_demo));
        } else {
            nanoIntent.removeExtra(FILE_LOCATION);
            mapVideoButton.setText(getResources().getString(R.string.map_media));
        }
        stopService(nanoIntent);
        startService(nanoIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == 1 && grantResults.length == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                restartService();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
