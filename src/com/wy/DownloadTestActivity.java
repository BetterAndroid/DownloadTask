
package com.wy;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.Toast;
import com.wy.download.*;

public class DownloadTestActivity extends Activity implements OnClickListener {

    private static final String SDCARD = Environment.getExternalStorageDirectory().getPath() + "/Download";

    protected static final String TAG = "TestActivity";

    private Context mContext;

    private EditText mUrlTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mContext = this;
        setContentView(R.layout.download_test);

        mUrlTV = (EditText) findViewById(R.id.download_url_text);
        findViewById(R.id.download_add1).setOnClickListener(this);
        findViewById(R.id.download_add2).setOnClickListener(this);
        findViewById(R.id.download_add3).setOnClickListener(this);
        findViewById(R.id.download_add4).setOnClickListener(this);
        findViewById(R.id.download_add5).setOnClickListener(this);
        findViewById(R.id.download_add6).setOnClickListener(this);

        findViewById(R.id.download_list).setOnClickListener(this);
        findViewById(R.id.downloaded_list).setOnClickListener(this);
        Res.getInstance(mContext);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.download_add1) {
            Toast.makeText(mContext,
                    Res.getInstance(mContext).getString("download_deleted_task_ok"), Toast.LENGTH_SHORT).show();
            DownloadTask task = DownloadTask.buildTask(this, "http://apache.etoak.com/ant/ivy/2.3.0-rc1/apache-ivy-2.3.0-rc1-src.zip");
            task.startDownload();
            Toast.makeText(this, R.string.download_task1, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.download_add2) {
            DownloadTask task = DownloadTask.buildTask(this, "http://www.us.apache.org/dist/axis/axis2/java/core/1.6.2/axis2-eclipse-service-plugin-1.6.2.zip");
            task.startDownload();
            Toast.makeText(this, R.string.download_task2, Toast.LENGTH_SHORT).show();

        } else if (id == R.id.download_add3) { // apk
            DownloadTask task = DownloadTask.buildTask(this, "ce4-b5d073894http://d2.eoemarket.com/upload/2012/0220/apps/5631/apks/157006/3edc770c-5d19-d052-b030.apk");
            task.startDownload();
            DownloadManager.getInstance(this).registerListener(task, new DownloadNotificationListener(mContext, task));
            DownloadManager.getInstance(this).registerListener(task,
                    new DownloadListener() {

                        @Override
                        public void onDownloadFinish(final DownloadTask task) {
                            // install apk
                        	DownloadTestActivity.this.runOnUiThread(new Runnable(){

								@Override
								public void run() {
									Intent intent = new Intent();
			                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			                        intent.setAction(android.content.Intent.ACTION_VIEW);
			                        Uri uri = Uri.fromFile(new File(task.dirPath));
			                        intent.setDataAndType(uri, "application/vnd.android.package-archive");
			                        startActivity(intent);									
								}});
   
                        }

                        @Override
                        public void onDownloadStart() {
                            // TODO Auto-generated method stub

                        }

                        @Override
                        public void onDownloadPause() {
                            // TODO Auto-generated method stub

                        }

                        @Override
                        public void onDownloadStop() {
                            // TODO Auto-generated method stub

                        }

                        @Override
                        public void onDownloadFail() {
                            // TODO Auto-generated method stub

                        }

                        @Override
                        public void onDownloadProgress(long finishedSize, long totalSize, long speed) {
                            // TODO Auto-generated method stub

                        }
                    });
//            DownloadManager.getInstance(this).startDownload(task);

        } else if (id == R.id.download_add4) {
            DownloadTask task = DownloadTask.buildTask(this, "http://vf1.mtime.cn/Video/2012/11/17/flv/121117084047608344.flv");
            task.startDownload();


            DownloadManager.getInstance(this).registerListener(task, new DownloadNotificationListener(mContext, task));
//            DownloadManager.getInstance(this).startDownload(task);
        } else if (id == R.id.download_add5) {
            DownloadTask task = DownloadTask.buildTask(this, "http://pic1.nipic.com/2008-09-11/2008911151744470_2.jpg");
            task.startDownload();
            DownloadManager.getInstance(this).registerListener(task, new DownloadNotificationListener(mContext, task));
//            DownloadManager.getInstance(this).startDownload(downloadTask5);
        } else if (id == R.id.download_add6) {
            String url = mUrlTV.getText().toString().trim();
            if (!URLUtil.isHttpUrl(url)) {
                Toast.makeText(mContext, "not valid http url", Toast.LENGTH_SHORT).show();
                return;
            }
            DownloadTask task = DownloadTask.buildTask(this, url);
            task.startDownload();

            DownloadManager.getInstance(this).registerListener(task,
                    new DownloadNotificationListener(mContext, task));
//            DownloadManager.getInstance(this).startDownload(task);
        } else if (id == R.id.download_list) {
            Toast.makeText(this, R.string.go_to_downloading_list, Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, DownloadListActivity.class);
            i.putExtra(DownloadListActivity.DOWNLOADED, false);
            startActivity(i);
        } else if (id == R.id.downloaded_list) {
            Toast.makeText(this, R.string.go_to_downloaded_list, Toast.LENGTH_SHORT).show();
            Intent i2 = new Intent(this, DownloadListActivity.class);
            i2.putExtra(DownloadListActivity.DOWNLOADED, true);
            startActivity(i2);
        }

    }

    public static boolean isNetWorkOn(Context context) {
        ConnectivityManager cManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            info.getTypeName();
            return true;
        } else {
            return false;
        }
    }

}
