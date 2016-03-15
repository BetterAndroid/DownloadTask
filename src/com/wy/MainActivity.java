package com.wy;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.wy.download.DownloadListener;
import com.wy.download.DownloadManager;
import com.wy.download.DownloadTask;

/**
 * Created by 51talk on 2016/2/20.
 */
public class MainActivity extends Activity implements View.OnClickListener, DownloadListener {

    private static final String TAG = "MainActivity";

    private Button mBtnDownload;
    private Button mBtnStopDownload;
    private Button mBtnDownload1;
    private Button mBtnStopDownload1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnDownload = (Button) findViewById(R.id.downlaod_btn);
        mBtnDownload.setOnClickListener(this);
        mBtnStopDownload = (Button) findViewById(R.id.stop_downlaod_btn);
        mBtnStopDownload.setOnClickListener(this);
        mBtnDownload1 = (Button) findViewById(R.id.downlaod_btn1);
        mBtnDownload1.setOnClickListener(this);
        mBtnStopDownload1 = (Button) findViewById(R.id.stop_downlaod_btn1);
        mBtnStopDownload1.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.downlaod_btn:
                DownloadManager.getInstance(this).startDownload(downloadUrl, this);
                break;
            case R.id.stop_downlaod_btn:
                /*if(task == null) {
                    return;
                }
                task.stopDownload();*/
                DownloadManager.getInstance(this).pauseDownload(downloadUrl);
                break;
            case R.id.downlaod_btn1:
                DownloadManager.getInstance(this).startDownload(flvUrl, this);
                break;
            case R.id.stop_downlaod_btn1:
                /*if(task == null) {
                    return;
                }
                task.stopDownload();*/
                DownloadManager.getInstance(this).pauseDownload(flvUrl);
                break;
            default:
                break;
        }
    }
//    DownloadTask task = null;
    String downloadUrl = "http://www.51talk.com/upload/open_pdf/2016/03/01/2016030112545393846.pdf";
    String flvUrl = "http://vf1.mtime.cn/Video/2012/11/17/flv/121117084047608344.flv";

    @Override
    public void onDownloadFinish(DownloadTask task) {
        Toast.makeText(getApplicationContext(), "onDownloadFinish="+task.downloadUrl, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onDownloadFinish===downloadUrl="+task.downloadUrl);
    }

    @Override
    public void onDownloadStart(DownloadTask task) {
        Toast.makeText(getApplicationContext(), "onDownloadStart", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onDownloadStart===downloadUrl="+task.downloadUrl);
    }

    @Override
    public void onDownloadPause(DownloadTask task) {
        Toast.makeText(getApplicationContext(), "onDownloadPause", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onDownloadPause===downloadUrl="+task.downloadUrl);
    }

    @Override
    public void onDownloadStop(DownloadTask task) {
        Toast.makeText(getApplicationContext(), "onDownloadStop", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDownloadFail(DownloadTask task) {
        Toast.makeText(getApplicationContext(), "onDownloadFail", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDownloadProgress(DownloadTask task, long finishedSize, long totalSize, long speed) {
//        Toast.makeText(context, "onDownloadProgress", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "finishedSize="+finishedSize+"-"+task.downloadUrl+"-"+"totalSize"+totalSize);
    }
}
