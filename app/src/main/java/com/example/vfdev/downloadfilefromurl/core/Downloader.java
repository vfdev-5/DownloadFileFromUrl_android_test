package com.example.vfdev.downloadfilefromurl.core;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.HashMap;

/**
 * Created by vfomin on 3/25/15.
 */
public class Downloader {

    private Context mContext;
    DownloadManager mManager;
    private static final int MIN_FREE_SPACE = 10*1024*1024;

    public static final int EXTERNAL_PUBLIC = 0;
    public static final int EXTERNAL_APP = 1;

    public static final int DOWNLOAD_OK = 0;
    public static final int CANCELED = 1;

    public static final int ERROR_NOT_INIT = 0;
    public static final int ERROR_NOT_ENOUGH_FREE_SPACE = 1;
    public static final int ERROR_FAILED_CLEAN = 2;
    public static final int ERROR_STORAGE_NOT_AVAILABLE = 3;
    public static final int ERROR_NO_STORAGE = 4;
    public static final int ERROR_DOWNLOAD_FAILED = 5;
    public static final int ERROR_CANCEL_FAILED = 6;

    private static Downloader mInstance;
    private File mStorage;
    private File mTarget;

    private HashMap<Uri, Long> mRequests;

    private OnErrorListener mOnErrorListener;
    private OnReportListener mOnReportListener;

    private _CountDownTimer mTimer;

    // ----- Public methods

    static public Downloader getInstance() {
        if (mInstance == null) {
            mInstance = new Downloader();
        }
        return mInstance;
    }

    public Downloader init(Context context) {
        mContext = context;
        mRequests = new HashMap<>();
        mManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        // 60 minutes max progress timeout, count down every second
        mTimer = new _CountDownTimer(60 * 60 * 1000, 1000);
        return this;
    }

    public Downloader where(File storage) {
        mStorage = storage;
        return this;
    }

    public Downloader where(int where) {
        if (!checkValid()) return this;
        mStorage = getStorage(where);
        return this;
    }

    public void  download(Uri sourceUri, String outputName) {
        if (!checkValid()) return;

        if (mStorage == null) {
            mStorage = getAvailableStorage();
            if (mStorage == null) {
                logError(ERROR_NO_STORAGE,
                        "No available storage");
                return;
            }
        } else if (!testStorage(mStorage)) {
            logError(ERROR_STORAGE_NOT_AVAILABLE,
                    "Specified storage is not available");
            return;
        }

        if (outputName.isEmpty()) {
            outputName = sourceUri.getLastPathSegment();
        }

        mManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);

        mTarget = new File(mStorage, outputName);
        cleanTarget(mTarget);

        Uri dstUri = Uri.fromFile(mTarget);
        DownloadManager.Request request = new DownloadManager.Request(sourceUri);
        request.setTitle("Download : " + outputName)
                .setAllowedOverRoaming(false)
                .setDescription(sourceUri.getPath())
                .setDestinationUri(dstUri);
        long id = mManager.enqueue(request);
        mRequests.put(sourceUri, id);
        mTimer.start();
    }

    /**
     * Return int code of DownloadManager status : STATUS_FAILED, STATUS_PAUSED, STATUS_PENDING,
     * STATUS_RUNNING and STATUS_SUCCESSFUL
     */
    public int getDownloadStatus(Uri uri) {
        if (!checkValid()) return -1;

        if (!mRequests.containsKey(uri)) return -1;
        long id = mRequests.get(uri);

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);
        Cursor c = mManager.query(query);
        if (c == null) return -1;
        if (c.getCount() == 0) {
            c.close();
            return -1;
        }
        c.moveToFirst();
        int index = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS);
        if (index > -1) {
            index = c.getInt(index);
        }
        c.close();
        return index;
    }

    public void cancelDownload(Uri uri) {
        if (!checkValid()) return;

        if (!mRequests.containsKey(uri)) return;
        long id = mRequests.get(uri);

        cancelDownload(id, uri);

    }

    // ------ Public helper methods

    public Downloader setOnErrorListener( OnErrorListener listener) {
        mOnErrorListener = listener;
        return this;
    }

    public Downloader setOnReportListener( OnReportListener listener) {
        mOnReportListener = listener;
        return this;
    }

    public boolean testStorage(File storage) {
        if (storage == null) return false;

        if (!storage.exists()) {
            return false;
        } else if (!storage.canWrite()) {
            return false;
        } else if (storage.getFreeSpace() < MIN_FREE_SPACE) {
            logError(ERROR_NOT_ENOUGH_FREE_SPACE,
                    "Storage \'" + storage.getAbsolutePath() + "\' has not enough free space");
            return false;
        }
        return true;
    }

    public boolean testStorage(int where) {
        if (!checkValid()) return false;
        return testStorage(getStorage(where));
    }

    // ----- Private methods
    private Downloader() { }

    /**
     * Method to log errors. OnErrorListener is called
     * @param code is one of values :
     * @param error message to report
     */
    private void logError(int code, String error) {
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(code, error);
        } else {
            Log.e("Downloader", error);
        }
    }

    /**
     * Method to report messages. OnReportListener is called
     * @param code is one of values :
     * @param msg to report
     */
    private void report(int code, String msg) {
        if (mOnReportListener != null) {
            mOnReportListener.onReport(code, msg);
        } else {
            Log.i("Downloader", msg);
        }
    }

    private boolean checkValid() {
        if (mContext == null) {
            logError(ERROR_NOT_INIT, "FileLoader should be initialized");
            return false;
        }
        return true;
    }

    private File getStorage(int where) {
        // DownloadManager can use only external storages
        if (where == EXTERNAL_PUBLIC) {
            return Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
        } else if (where == EXTERNAL_APP) {
            return mContext.getExternalFilesDir(null);
        }
        return null;
    }

    private File getAvailableStorage() {
        // DownloadManager can use only external storages
        if (testStorage(EXTERNAL_PUBLIC)) {
            return getStorage(EXTERNAL_PUBLIC);
        }
        if (testStorage(EXTERNAL_APP)) {
            return getStorage(EXTERNAL_APP);
        }
        return null;
    }

    private boolean cleanTarget(File f) {
        if (f.exists()) {
            if (!f.delete()) {
                logError(ERROR_FAILED_CLEAN,
                        "Failed to remove output file on clean");
                return false;
            }
        }
        return true;
    }

    private void cancelDownload(long id, Uri uri) {

        mManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        if (mManager.remove(id) > 0) {
            mRequests.remove(uri);
        }
    }


    // private CountDownTimer class
    private class _CountDownTimer extends CountDownTimer {

        _CountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        public void onTick(long millisUntilFinished) {

            Log.i("Downloader", "onTick");

            if (mRequests.size() == 0) {
                // stop timer
                mTimer.cancel();
                Log.i("Downloader", "cancel timer");
                return;
            }

            DownloadManager.Query q = new DownloadManager.Query();
            long[] ids = new long[mRequests.size()];
            int count=0;
            for (Long l : mRequests.values()) {
                ids[count] = l;
                count++;
            }
            q.setFilterById(ids);
            Cursor c = mManager.query(q);
            if (c == null) return;
            if (c.getCount() == 0) {
                c.close();
                Log.i("Donwloader", "cursor is empty");
                return;
            }

            c.moveToFirst();
            int idIndex = c.getColumnIndex(DownloadManager.COLUMN_ID);
            int titleIndex = c.getColumnIndex(DownloadManager.COLUMN_TITLE);
            int statusIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int uriIndex = c.getColumnIndex(DownloadManager.COLUMN_URI);
            if (idIndex < 0 || titleIndex < 0
                    || statusIndex < 0 || uriIndex < 0) {
                c.close();
                Log.i("Donwloader", "idIndex < 0 || titleIndex < 0 || statusIndex < 0");
                return;
            }

            do {
                int code = c.getInt(statusIndex);
                long id = c.getLong(idIndex);


                if (code == DownloadManager.STATUS_SUCCESSFUL) {
                    report(DOWNLOAD_OK, "");
                    mTimer.cancel();
                } else if (code == DownloadManager.STATUS_FAILED) {
                    logError(ERROR_DOWNLOAD_FAILED, "");
                    Uri uri = Uri.parse(c.getString(uriIndex));
                    cancelDownload(id, uri);
                    mTimer.cancel();
                }
                String msg = code + " , " + id + " : " + c.getString(titleIndex);
                Log.i("Downloader", msg);

            } while (c.moveToNext());
            c.close();

        }

        public void onFinish() {
            Log.i("Download", "onFinish");
            Log.i("Download", "mRequests.size = " + mRequests.size());
//            if (mRequests.size() > 0) {
//                start();
//            }
        }


    }


    // ------ On Error Listener

    public interface OnErrorListener {
        public void onError(int code, String errorMessage);
    }

    // ------- On Result Listener

    public interface OnReportListener {
        public void onReport(int code, String msg);

    }

}
