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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnDownload = (Button) findViewById(R.id.downlaod_btn);
        mBtnDownload.setOnClickListener(this);
        mBtnStopDownload = (Button) findViewById(R.id.stop_downlaod_btn);
        mBtnStopDownload.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.downlaod_btn:
                downloadImg();
                break;
            case R.id.stop_downlaod_btn:
                /*if(task == null) {
                    return;
                }
                task.stopDownload();*/
                DownloadManager.getInstance(this).pauseDownload(downloadUrl);
                break;
            default:
                break;
        }
    }
//    DownloadTask task = null;
    String downloadUrl = "http://www.51talk.com/upload/open_pdf/2016/03/01/2016030112545393846.pdf";
    private void downloadImg() {
//        String downloadUrl = "http://vf1.mtime.cn/Video/2012/11/17/flv/121117084047608344.flv";
//        String downloadUrl = "http://www.51talk.com/upload/open_pdf/2016/03/01/2016030112545393846.pdf";
//        task = DownloadTask.buildTask(this, downloadUrl);
//        task.startDownload();

        DownloadManager.getInstance(this).startDownload(downloadUrl, this);
    }

    @Override
    public void onDownloadFinish(DownloadTask task) {
        task.targetFile.renameTo(task.saveFile);
        Toast.makeText(getApplicationContext(), "onDownloadFinish", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDownloadStart(DownloadTask task) {
        Toast.makeText(getApplicationContext(), "onDownloadStart", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDownloadPause(DownloadTask task) {
        Toast.makeText(getApplicationContext(), "onDownloadPause", Toast.LENGTH_SHORT).show();
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
    public void onDownloadProgress(long finishedSize, long totalSize, long speed) {
//        Toast.makeText(context, "onDownloadProgress", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "finishedSize="+finishedSize+"--"+"totalSize"+totalSize);
    }
}
