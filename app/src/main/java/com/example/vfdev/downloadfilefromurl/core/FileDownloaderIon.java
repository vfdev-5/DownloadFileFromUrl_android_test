package com.example.vfdev.downloadfilefromurl.core;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.ProgressBar;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;
import com.koushikdutta.ion.builder.Builders;

import java.io.File;

/**
 */
public class FileDownloaderIon {


    private File mStorage;
    private File mTarget;
    private boolean downloading = false;

    private Context mContext;
    private Object mProgressIndicator; // Bar or Dialog
    private ProgressCallback mProgressCallback;

    private Ion ion;
    private _FutureCallback mCallback;
    private Future<File> mFuture;

    public static final int DOWNLOAD_OK = 0;
    public static final int CANCELED = 1;

    public static final int ERROR_NOT_INIT = 0;
    public static final int ERROR_NOT_ENOUGH_FREE_SPACE = 1;
    public static final int ERROR_FAILED_CLEAN = 2;
    public static final int ERROR_STORAGE_NOT_AVAILABLE = 3;
    public static final int ERROR_NO_STORAGE = 4;
    public static final int ERROR_DOWNLOAD_FAILED = 5;
    public static final int ERROR_CANCEL_FAILED = 6;


    private OnErrorListener mOnErrorListener;
    private OnReportListener mOnReportListener;


    private static final int MIN_FREE_SPACE = 10*1024*1024;

    public static final int EXTERNAL_PUBLIC = 0;
    public static final int EXTERNAL_APP = 1;
    public static final int INTERNAL_APP = 2;

    private static FileDownloaderIon mInstance;

    // ----- Public methods

    static public FileDownloaderIon getInstance() {
        if (mInstance == null) {
            mInstance = new FileDownloaderIon();
        }
        return mInstance;
    }

    public FileDownloaderIon init(Context context) {
        mContext = context;
        ion = Ion.getInstance(mContext, "FileDownloaderIon");
        // Enable global Ion logging
        ion.configure().setLogging("FileDownloaderIon", Log.DEBUG);
        return this;
    }

    public boolean downloading() {
        return downloading;
    }

    public FileDownloaderIon progress(ProgressBar p) {
        mProgressIndicator = p;
        return this;
    }

    public FileDownloaderIon progress(ProgressDialog p) {
        mProgressIndicator = p;
        return this;
    }

    public FileDownloaderIon progress(ProgressCallback callback) {
        mProgressCallback = callback;
        return this;
    }

    public FileDownloaderIon where(File storage) {
        mStorage = storage;
        return this;
    }

    public FileDownloaderIon where(int where) {
        if (!checkValid()) return this;
        mStorage = getStorage(where);
        return this;
    }

    public void download(String url, String output) {
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
        mTarget = new File(mStorage, output);
        cleanTarget(mTarget);

        mCallback = new _FutureCallback();

        Builders.Any.B lb = ion.build(mContext)
                .load(url);
        if (mProgressIndicator != null) {
            if (mProgressIndicator instanceof ProgressBar) {
                lb=lb.progressBar((ProgressBar)mProgressIndicator);
            } else if (mProgressIndicator instanceof ProgressDialog) {
                lb=lb.progressDialog((ProgressDialog) mProgressIndicator);
            }
        } else if (mProgressCallback != null) {
            lb=lb.progress(mProgressCallback);
        }
        mFuture = lb.write(mTarget)
                .setCallback(mCallback);
        downloading = true;
    }

    public void cancel() {
        if (downloading) {
            if (!mFuture.cancel(true)) {
                logError(ERROR_CANCEL_FAILED, "Failed to cancel downloading");
            }
        }
    }

    public void cancelAndClean() {
        if (downloading) {
            cancel();
            cleanTarget(mTarget);
        }
    }

    // ------ Public helper methods

    public FileDownloaderIon setOnErrorListener( OnErrorListener listener) {
        mOnErrorListener = listener;
        return this;
    }

    public FileDownloaderIon setOnReportListener( OnReportListener listener) {
        mOnReportListener = listener;
        return this;
    }

    public boolean testStorage(int where) {
        if (!checkValid()) return false;
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

    private FileDownloaderIon() {    }

    private void logError(int code, String error) {
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(code, error);
        } else {
            Log.e("FileDownloaderIon", error);
        }
    }

    private void report(int code, String msg) {
        if (mOnReportListener != null) {
            mOnReportListener.onReport(code, msg);
        } else {
            Log.i("FileDownloaderIon", msg);
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
        if (where == EXTERNAL_PUBLIC) {
            return Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
        } else if (where == EXTERNAL_APP) {
            return mContext.getExternalFilesDir(null);
        } else if (where == INTERNAL_APP) {
            return mContext.getFilesDir();
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

    private void resetProgressBar() {
        if (mProgressIndicator != null) {
            if (mProgressIndicator instanceof ProgressBar) {
                ProgressBar pb = (ProgressBar) mProgressIndicator;
                pb.setProgress(0);
            } else if (mProgressIndicator instanceof ProgressDialog) {
                ProgressDialog pd = (ProgressDialog) mProgressIndicator;
                pd.setProgress(0);
                pd.hide();
            }
        }
    }

    // ------ Ajax callback

    private class _FutureCallback implements FutureCallback<File> {

        @Override
        public void onCompleted(Exception e, File file) {
            if (mFuture.isDone() && file != null) {
                report(DOWNLOAD_OK, "File downloaded");
            } else if (mFuture.isCancelled()) {
                report(CANCELED, "File download canceled");
                resetProgressBar();
            } else {
                logError(ERROR_DOWNLOAD_FAILED, "Download failed : " + e.getMessage());
                resetProgressBar();
            }
            downloading = false;

            // clean cache
            ion.getCache().clear();

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
