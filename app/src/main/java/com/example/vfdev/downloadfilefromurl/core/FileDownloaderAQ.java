package com.example.vfdev.downloadfilefromurl.core;

import android.app.Activity;
import android.os.Environment;
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import android.widget.ProgressBar;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.util.AQUtility;

import java.io.File;

/**
 */
public class FileDownloaderAQ {

    private File mStorage;
    private File target;
    private boolean downloading = false;
    private boolean canceled = false;
    private int progressViewId = -1;

    private _AjaxCallback ajaxCallback;
    private AQuery aq;

    public static final int DOWNLOAD_OK = 0;
    public static final int CANCELED = 1;

    public static final int ERROR_NOT_INIT = 0;
    public static final int ERROR_NOT_ENOUGH_FREE_SPACE = 1;
    public static final int ERROR_FAILED_CLEAN = 2;
    public static final int ERROR_STORAGE_NOT_AVAILABLE = 3;
    public static final int ERROR_NO_STORAGE = 4;
    public static final int ERROR_DOWNLOAD_FAILED = 5;


    private OnErrorListener mOnErrorListener;
    private OnReportListener mOnReportListener;


    private static final int MIN_FREE_SPACE = 10*1024*1024;

    public static final int EXTERNAL_PUBLIC = 0;
    public static final int EXTERNAL_APP = 1;
    public static final int INTERNAL_APP = 2;

    private static FileDownloaderAQ mInstance;

    // ----- Public methods

    static public FileDownloaderAQ getInstance() {
        if (mInstance == null) {
            mInstance = new FileDownloaderAQ();
        }
        return mInstance;
    }

    public FileDownloaderAQ init(Activity activity) {
        aq = new AQuery(activity);
        AQUtility.setDebug(true);
        return this;
    }

    static public void release() {
        mInstance.cleanUp();
        mInstance = null;
    }

    public boolean downloading() {
        return downloading;
    }

    public FileDownloaderAQ progress(int id) {
        if (!checkValidAq()) return this;
        aq.progress(id);
        progressViewId = id;
        return this;
    }

    public FileDownloaderAQ where(File storage) {
        if (!checkValidAq()) return this;
        mStorage = storage;
        return this;
    }

    public FileDownloaderAQ where(int where) {
        if (!checkValidAq()) return this;
        mStorage = getStorage(where);
        return this;
    }

    public void download(String url, String output) {
        if (!checkValidAq()) return;

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
        target = new File(mStorage, output);
        cleanTarget(target);
        ajaxCallback = new _AjaxCallback();
        aq.download(url, target, ajaxCallback);
        downloading = true;
    }

    public void cancel() {
        if (downloading) {
            try {
                ajaxCallback.abort();
                ajaxCallback = null;
            } catch (NetworkOnMainThreadException exception) {
                // on abort this exception is always raised
//              report(-1, "");
            }
            report(CANCELED, "File download canceled");
            downloading = false;
            canceled = true;
        }
    }

    public void cancelAndClean() {
        if (downloading) {
            cancel();
            cleanTarget(target);
        }
    }

    public void reset() {
        if (downloading) {
            cancel();
        }
        mStorage = null;
        progressViewId = -1;
        mOnErrorListener = null;
        mOnReportListener = null;
    }


    // ------ Public helper methods

    public FileDownloaderAQ setOnErrorListener( OnErrorListener listener) {
        mOnErrorListener = listener;
        return this;
    }

    public FileDownloaderAQ setOnReportListener( OnReportListener listener) {
        mOnReportListener = listener;
        return this;
    }

    public boolean testStorage(int where) {
        if (!checkValidAq()) return false;
        return testStorage(getStorage(where));
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

    // ----- Private methods

    private FileDownloaderAQ() { }

    private void logError(int code, String error) {
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(code, error);
        } else {
            Log.e("FileDownloaderAQ", error);
        }
    }

    private void report(int code, String msg) {
        if (mOnReportListener != null) {
            mOnReportListener.onReport(code, msg);
        } else {
            Log.i("FileDownloaderAQ", msg);
        }
    }

    private boolean checkValidAq() {
        if (aq == null) {
            logError(ERROR_NOT_INIT, "FileLoader should be initialized");
            return false;
        }
        return true;
    }

    private File getStorage(int where) {
        if (where == EXTERNAL_PUBLIC) {
            return Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
        } else if (where == EXTERNAL_APP) {
            return aq.getContext().getExternalFilesDir(null);
        } else if (where == INTERNAL_APP) {
            return aq.getContext().getFilesDir();
        }
        return null;
    }


    private File getAvailableStorage() {
        if (testStorage(EXTERNAL_PUBLIC)) {
            return getStorage(EXTERNAL_PUBLIC);
        }
        if (testStorage(EXTERNAL_APP)) {
            return getStorage(EXTERNAL_APP);
        }
        if (testStorage(INTERNAL_APP)) {
            return getStorage(INTERNAL_APP);
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


    void resetProgressBar() {
        if (progressViewId >= 0) {
            ProgressBar pb = aq.id(progressViewId).getProgressBar();
            if (pb != null) {
                pb.setProgress(0);
            }
        }
    }

    void cleanUp() {
        Log.i("FileDownloaderAQ", "Clean up");
        aq.ajaxCancel();
        cancel();
        AQUtility.cleanCacheAsync(aq.getContext());
        aq = null;
    }

    // ------ Ajax callback

    private class _AjaxCallback extends AjaxCallback<File> {
        public void callback(String url, File file, AjaxStatus status) {
            if (canceled) {
                resetProgressBar();
                canceled = false;
                status.invalidate();
                return;
            }

            if (file != null) {
                report(DOWNLOAD_OK, "File downloaded . Status : " + status.getMessage());
            } else {
                logError(ERROR_DOWNLOAD_FAILED, status.getMessage() + ". Code : " + status.getCode());
                resetProgressBar();
                status.invalidate();
            }
            downloading = false;
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
