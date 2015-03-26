package com.example.vfdev.downloadfilefromurl;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vfdev.downloadfilefromurl.core.FileDownloadService;
import com.example.vfdev.downloadfilefromurl.core.FileDownloaderAQ;
import com.example.vfdev.downloadfilefromurl.core.FileDownloaderIon;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class MainActivity extends Activity
{

    // UI
    @InjectView(R.id.url)
    protected TextView mUrl;

    @InjectView(R.id.downloadAQ)
    protected Button mDownloadAQ;

    @InjectView(R.id.downloadIon)
    protected Button mDownloadIon;

    @InjectView(R.id.downloadAQService)
    protected Button mDownloadAQService;

    @InjectView(R.id.downloadIonService)
    protected Button mDownloadIonService;

    @InjectView(R.id.progress)
    protected ProgressBar mProgressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        FileDownloaderAQ.getInstance()
                .init(this)
                .setOnErrorListener(fdAQEL)
                .setOnReportListener(fdAQRL);

        FileDownloaderIon.getInstance()
                .init(this)
                .setOnErrorListener(fdIonEL)
                .setOnReportListener(fdIonRL);

        mDownloadAQ.setText("Download with AQuery");
        mDownloadIon.setText("Download with Ion");

    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        FileDownloaderAQ.release();

        stopService(new Intent(this, FileDownloadService.class));

        super.onDestroy();
    }


    // ----- DOWNLOAD
    private final static String TAG = MainActivity.class.getSimpleName();

    boolean isDownloading = false;

    @OnClick(R.id.downloadAQ)
    public void onDownloadClicked(View view) {

        if (!FileDownloaderAQ.getInstance().downloading()) {


            String url = mUrl.getText().toString();
            Log.w(TAG, "URL = " + url);

            String outputFileName="test/track.mp3";

            FileDownloaderAQ.getInstance()
                    .progress(R.id.progress)
                    .download(url, outputFileName);
            mDownloadAQ.setText("Cancel");

        } else {
            FileDownloaderAQ.getInstance()
                    .cancel();
//                    .cancelAndClean();
            mDownloadAQ.setText("Download with AQuery");
        }
    }

    FileDownloaderAQ.OnErrorListener fdAQEL = new FileDownloaderAQ.OnErrorListener() {
        @Override
        public void onError(int code, String errorMessage) {
            Log.e(TAG, errorMessage);
            if (code == FileDownloaderAQ.ERROR_DOWNLOAD_FAILED) {
                Toast.makeText(MainActivity.this,
                        "Download is failed",
                        Toast.LENGTH_SHORT).show();
                mDownloadAQ.setText("Download with AQuery");
            }
        }
    };

    FileDownloaderAQ.OnReportListener fdAQRL = new FileDownloaderAQ.OnReportListener() {
        @Override
        public void onReport(int code, String msg) {
            if (code == FileDownloaderAQ.DOWNLOAD_OK) {
                Toast.makeText(MainActivity.this,
                        "Download is complete",
                        Toast.LENGTH_SHORT).show();
                mDownloadAQ.setText("Download with AQuery");
            }
            Log.i(TAG, msg);
        }
    };





    @OnClick(R.id.downloadIon)
    public void onDownloadClicked2(View view) {

        if (!FileDownloaderIon.getInstance().downloading()) {

            String url = mUrl.getText().toString();
            Log.i(TAG, "URL = " + url);

            String outputFileName="test/track.mp3";

            FileDownloaderIon.getInstance()
                    .progress(mProgressBar)
                    .download(url, outputFileName);
            mDownloadIon.setText("Cancel");

        } else {
            FileDownloaderIon.getInstance()
                    .cancel();
//                    .cancelAndClean();
            mDownloadIon.setText("Download with Ion");
        }
    }


    FileDownloaderIon.OnErrorListener fdIonEL = new FileDownloaderIon.OnErrorListener() {
        @Override
        public void onError(int code, String errorMessage) {
            Log.e(TAG, errorMessage);
            if (code == FileDownloaderIon.ERROR_DOWNLOAD_FAILED) {
                Toast.makeText(MainActivity.this,
                        "Download is failed",
                        Toast.LENGTH_SHORT).show();
                mDownloadIon.setText("Download with Ion");
            }
        }
    };

    FileDownloaderIon.OnReportListener fdIonRL = new FileDownloaderIon.OnReportListener() {
        @Override
        public void onReport(int code, String msg) {
            if (code == FileDownloaderIon.DOWNLOAD_OK) {
                Toast.makeText(MainActivity.this,
                        "Download is complete",
                        Toast.LENGTH_SHORT).show();
                mDownloadIon.setText("Download with Ion");
            }
            Log.i(TAG, msg);
        }
    };



    @OnClick(R.id.downloadIonService)
    public void onDownloadIonService(View v){

        Log.i(TAG, "onDownloadIonService");
        Intent i = new Intent(this, FileDownloadService.class);
        i.putExtra("Url", mUrl.getText().toString());
//        i.putExtra("Type", FileDownloadService.USE_ION);
        startService(i);

    }


}
