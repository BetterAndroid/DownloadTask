package com.wy;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.wy.download.DownloadListener;
import com.wy.download.DownloadTask;
import com.zxt.download2.R;

/**
 * Created by 51talk on 2016/2/20.
 */
public class MainActivity extends Activity implements View.OnClickListener {

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
                if(task == null) {
                    return;
                }
                task.stopDownload();
                break;
            default:
                break;
        }
    }
    DownloadTask task = null;
    private void downloadImg() {
//        String downloadUrl = "http://vf1.mtime.cn/Video/2012/11/17/flv/121117084047608344.flv";
        String downloadUrl = "http://www.51talk.com/upload/open_pdf/2016/03/01/2016030112545393846.pdf";
        task = DownloadTask.buildTask(this, downloadUrl);
        task.startDownload();
    }
}
