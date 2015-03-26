package com.example.vfdev.downloadfilefromurl;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vfdev.downloadfilefromurl.core.Downloader;
import com.example.vfdev.downloadfilefromurl.core.FileDownloadService;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;



// SERVICE OR NOT SERVICE in my App
// SERVICE (as standard download) : + can assign tasks -> start play a track -> destroy main activity and listen
//           - Need notifications to inform about work progress (notifications are automatically removed)
//           - No progress info in the main activity

// NOT SERVICE (as in whatsapp) : + assign tasks and observe work progress
//               - quiting main activity, work is canceled

public class MainActivity2 extends Activity {

    private final static String TAG = MainActivity2.class.getSimpleName();

    // UI
    @InjectView(R.id.url1)
    protected TextView mUrl1;
    @InjectView(R.id.url2)
    protected TextView mUrl2;
    @InjectView(R.id.url3)
    protected TextView mUrl3;

    @InjectView(R.id.download1)
    protected Button mDownload1;
    @InjectView(R.id.download2)
    protected Button mDownload2;
    @InjectView(R.id.download3)
    protected Button mDownload3;

    @InjectView(R.id.progress1)
    protected ProgressBar mProgressBar1;
    @InjectView(R.id.progress2)
    protected ProgressBar mProgressBar2;
    @InjectView(R.id.progress3)
    protected ProgressBar mProgressBar3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity2);

        ButterKnife.inject(this);

        Downloader.getInstance()
                .init(this)
                .setOnErrorListener(dEL)
                .setOnReportListener(dRL);

    }


    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");



        // for dev debug purposes

        stopService(new Intent(this, FileDownloadService.class));



        super.onDestroy();


    }


    @OnClick(R.id.download1)
    public void download1(View v){

        Uri srcUri = Uri.parse(mUrl1.getText().toString());

//        if (mDownload1.getText().equals("Download")) {

        if (Downloader.getInstance().getDownloadStatus(srcUri) < 0) {

            Log.i(TAG, "download url 1");
            mDownload1.setText("Cancel");
            String filename = "track1";
            Downloader.getInstance()
//                .where(Downloader.EXTERNAL_PUBLIC)
                    .download(srcUri, filename);

            int code = Downloader.getInstance().getDownloadStatus(srcUri);
            Log.i(TAG, "Download status code : " + code);


        } else {

            Log.i(TAG, "cancel url 1");
            Downloader.getInstance().cancelDownload(srcUri);
            mDownload1.setText("Download");

        }




//            Intent i = new Intent(FileDownloadService.ACTION_DOWNLOAD);
//            i.setClass(this,FileDownloadService.class);
//            i.setData(Uri.parse(mUrl1.getText().toString()));
//            i.putExtra("Filename", "track1.mp3");
//            i.putExtra("Where", FileDownloadService.EXTERNAL_PUBLIC);
//            startService(i);
//            mDownload1.setText("Cancel");
//
//        } else {
//            Log.i(TAG, "cancel url 1");
//            Intent i = new Intent(FileDownloadService.ACTION_CANCEL);
//            i.setData(Uri.parse(mUrl1.getText().toString()));
//            startService(i);
//            mDownload1.setText("Download");
//        }
    }

    @OnClick(R.id.download2)
    public void download2(View v){

//        if (mDownload2.getText().equals("Download")) {
            Log.i(TAG, "download url 2");
            Intent i = new Intent(FileDownloadService.ACTION_DOWNLOAD);
            i.setClass(this,FileDownloadService.class);
            i.setData(Uri.parse(mUrl2.getText().toString()));
            i.putExtra("Filename", "track2.mp3");
            i.putExtra("Where", FileDownloadService.INTERNAL_APP);
            startService(i);
//            mDownload2.setText("Cancel");
//        } else {
//            Log.i(TAG, "cancel url 2");
//            Intent i = new Intent(FileDownloadService.ACTION_CANCEL);
//            i.putExtra("Url", mUrl2.getText().toString());
//            startService(i);
//            mDownload2.setText("Download");
//        }
    }

    @OnClick(R.id.download3)
    public void download3(View v){
//        if (mDownload3.getText().equals("Download")) {
            Log.i(TAG, "download url 3");
            Intent i = new Intent(FileDownloadService.ACTION_DOWNLOAD);
            i.setClass(this,FileDownloadService.class);
            i.setData(Uri.parse(mUrl3.getText().toString()));
            startService(i);
//            mDownload3.setText("Cancel");
//        } else {
//            Log.i(TAG, "cancel url 3");
//            Intent i = new Intent(FileDownloadService.ACTION_CANCEL);
//            i.putExtra("Url", mUrl3.getText().toString());
//            startService(i);
//            mDownload3.setText("Download");
//        }
    }





    Downloader.OnErrorListener dEL = new Downloader.OnErrorListener() {
        @Override
        public void onError(int code, String errorMessage) {
            Log.e(TAG, errorMessage);
            switch (code) {
                case Downloader.ERROR_DOWNLOAD_FAILED :
                case Downloader.ERROR_NO_STORAGE:
                case Downloader.ERROR_NOT_ENOUGH_FREE_SPACE:
                    Toast.makeText(MainActivity2.this,
                            "Download is failed",
                            Toast.LENGTH_SHORT).show();
                    mDownload1.setText("Download");
                    break;
            }
        }
    };

    Downloader.OnReportListener dRL = new Downloader.OnReportListener() {
        @Override
        public void onReport(int code, String msg) {
            if (code == Downloader.DOWNLOAD_OK) {
                Toast.makeText(MainActivity2.this,
                        "Download is complete",
                        Toast.LENGTH_SHORT).show();
                mDownload1.setText("Download");
            }
            Log.i(TAG, msg);
        }
    };




}
