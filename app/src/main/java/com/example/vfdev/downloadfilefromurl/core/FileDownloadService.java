package com.example.vfdev.downloadfilefromurl.core;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.example.vfdev.downloadfilefromurl.R;
import com.koushikdutta.ion.ProgressCallback;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *  Service to download files from URL
 * 1) Start multiple downloads (as 1 worker on a queue)
 * 2) Cancel specified download
 * 3) Settings :
 *      a) Define storage: external public or package, internal package
 * 4) Show progress of each download in main UI
 * 5) Service notification is optional
*/
public class FileDownloadService extends Service {

    // debug logging
    private static final String TAG = "FileDownloadService";
    private boolean debug = true;
    public void debug(boolean v) {
        debug = v;
    }
    private void logDebug(String s) {
        if (debug) {
            Log.d(TAG, s);
        }
    }
    private void logError(Exception e, String s) {
        if (debug) {
            if (e != null) {
                Log.e(TAG, s + ". " + e.getMessage() );
            } else {
                Log.e(TAG, s);
            }
        }
    }

    // Service properties :
    public static final String ACTION_DOWNLOAD = "com.example.vfdev.downloadfilefromurl.DOWNLOAD";
    public static final String ACTION_CANCEL = "com.example.vfdev.downloadfilefromurl.CANCEL";

    public static final int EXTERNAL_PUBLIC = FileDownloaderIon.EXTERNAL_PUBLIC;
    public static final int EXTERNAL_APP = FileDownloaderIon.EXTERNAL_APP;
    public static final int INTERNAL_APP = FileDownloaderIon.INTERNAL_APP;


    private ArrayList<String> mTaskList = new ArrayList<String>();

    // Notifications:
    private HashMap<String, Integer> mTaskNotificationMap = new HashMap<String, Integer>();
    private HashMap<Integer, String> mNotificationTaskMap = new HashMap<Integer, String>();
    private NotificationManager mNotificationManager;

    // Wifi manager
    private WifiManager.WifiLock mWifiLock;


    public FileDownloadService() {
    }

    // -------- Service methods

    @Override
    public void onCreate() {
        logDebug("Creating service");

        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "MY_WIFI_LOCK");
        mWifiLock.acquire();


        FileDownloaderIon.getInstance()
                .init(getApplicationContext())
                .setOnReportListener(fdIonRL)
                .setOnErrorListener(fdIonEL);

//        notificationView = new RemoteViews(getPackageName(), R.layout.notification);

    }

    /**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logDebug("onStartCommand");

        if (intent != null) {
            Uri url = intent.getData();
            logDebug("Data : " + url.toString());
            String action = intent.getAction();
            if (action.equals(ACTION_DOWNLOAD)){
                logDebug("Action : download");

                int where = intent.getIntExtra("Where", -1);
                String filename = intent.getStringExtra("Filename");

                startTask(url.toString(), filename, where);
            } else
            if (action.equals(ACTION_CANCEL)){
                logDebug("Action : cancel");
                cancelTask(url.toString());
            }
        }
        return START_STICKY; // for services that are explicitly started and stopped as needed,
    }

    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        logDebug("Destroy service");
        releaseResources();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        logDebug("onBind");
        return null;
    }


    // ------------ Other methods

    private void releaseResources() {

        // close all notifications:
        for (int id : mTaskNotificationMap.values()) {
            mNotificationManager.cancel(id);
        }

        stopDownloading();

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) mWifiLock.release();

    }


    private void startTask(String url, String filename, int where) {

        mTaskList.add(url);

        showNotification(url);
        startDownloading(url, filename, where);
    }

    private void cancelTask(String url) {
        FileDownloaderIon.getInstance()
//                        .cancel();
                .cancelAndClean();
        if (mTaskNotificationMap.containsKey(url)) {
            int nId = mTaskNotificationMap.get(url);
            mNotificationManager.cancel(nId);
        } else {
            logDebug("Task is not found for url : " + url);
        }
    }

    private void showNotification(String url) {

        logDebug("Show notification");

        int nId = url.hashCode();
        mTaskNotificationMap.put(url, nId);
        mNotificationTaskMap.put(nId, url);


        RemoteViews nView = new RemoteViews(getPackageName(), R.layout.notification);
        nView.setTextViewText(R.id.notification_url, url);

        Intent i = new Intent(FileDownloadService.ACTION_DOWNLOAD);
        i.setData(Uri.parse(url));
        PendingIntent pi = PendingIntent.getService(getApplicationContext(), 0, i, PendingIntent.FLAG_ONE_SHOT);
        nView.setOnClickPendingIntent(R.id.notification_close, pi);

        // Set the icon, scrolling text and timestamp
        Notification n = new Notification.Builder(getApplicationContext())
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true) // Notification can not be removed
                .build();

        n.bigContentView = nView;

        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        mNotificationManager.notify(nId, n);

    }


    private void startDownloading(String url , String filename, int where) {
        logDebug("startDownloading : " + url);

        if (!FileDownloaderIon.getInstance().downloading()) {

            String outputFileName = filename.isEmpty() ? "file_" + url.hashCode() : filename;
            logDebug("Download to file : " + outputFileName + " into storage " + where);


            FileDownloaderIon.getInstance()
                    .progress(new ProgressCallback() {
                        @Override
                        public void onProgress(long downloaded, long total) {
                            logDebug("onProgess : " + String.valueOf(downloaded) + "/" + String.valueOf(total));
                            int value = (int) (100 * (downloaded * 1.0 / total));
//                            notificationView.setProgressBar(R.id.notification_progressBar, 100, value, false);
//                            mNotificationManager.notify(notificationId, notification);
                        }
                    });
            if (where >= 0) {
                FileDownloaderIon.getInstance()
                        .where(where);
            }
            FileDownloaderIon.getInstance().
                    download(url, outputFileName);
        }
        else {
//            logDebug("Cancel");
//            FileDownloaderIon.getInstance()
////                        .cancel();
//                    .cancelAndClean();
        }
    }


    private void stopDownloading() {

        if (FileDownloaderIon.getInstance().downloading()) {
            logDebug("Cancel");
            FileDownloaderIon.getInstance()
                    .cancelAndClean();
        }

    }


    FileDownloaderIon.OnErrorListener fdIonEL = new FileDownloaderIon.OnErrorListener() {
        @Override
        public void onError(int code, String errorMessage) {
            if (code == FileDownloaderIon.ERROR_DOWNLOAD_FAILED) {
                logError(null, errorMessage);



            }
        }
    };

    FileDownloaderIon.OnReportListener fdIonRL = new FileDownloaderIon.OnReportListener() {
        @Override
        public void onReport(int code, String msg) {
            if (code == FileDownloaderIon.DOWNLOAD_OK ||
                    code == FileDownloaderIon.CANCELED) {
                logDebug(msg);



            }
        }
    };

}
