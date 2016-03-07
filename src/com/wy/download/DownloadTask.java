
package com.wy.download;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;

/**
 * DownloadTask class 用于描述一个下载任务
 */
public class DownloadTask extends AsyncTask<Executor, Long, Void> implements DownloadListener {
    private static final String TAG = DownloadTask.class.getSimpleName();
    private Context context;
    /**
     * 下载的地址
     */
    public String downloadUrl;

    /**
     * 文件名称
     */
    public String fileName;

    /**
     * 保存下载文件到本地，默认的地址为/sdcard/download
     */
    public String dirPath = Environment.getExternalStorageDirectory().getPath() + File.separator +"Download";

    public File saveFile;
    public File targetFile;
    public String cachSuffix = "cache";
    public String mimeType;

    /**
     * 下载的状态
     */
    public volatile DownloadState downloadState;

    public DownloadTask(Context context) {
        this.context = context;
    }

    public static DownloadTask buildTask(Context context, String downloadUrl){
            return buildTask(context,downloadUrl,"");
    }

     public static DownloadTask buildTask(Context context, String downloadUrl, String dirPath) {
        DownloadTask task = new DownloadTask(context);
        task.downloadUrl = downloadUrl;
        task.fileName = setFileName(downloadUrl);
        if(!TextUtils.isEmpty(dirPath)) {
            task.dirPath = dirPath;
        }
        task.targetFile = setTargetFile(task.dirPath, task.fileName);
        task.saveFile = setSaveFile(task.dirPath,task.fileName,downloadUrl);
        return task;
    }

    private static String setFileName(String downloadUrl) {
        return downloadUrl.substring(downloadUrl.lastIndexOf("/"), downloadUrl.lastIndexOf("."));
    }

    private static File setTargetFile(String dirPath, String fileName) {
        File dir = new File(dirPath);
        if(!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dirPath, fileName + ".cache");
    }

    private static File setSaveFile(String dirPath, String fileName, String downloadUrl) {
        File dir = new File(dirPath);
        if(!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dirPath, fileName + downloadUrl.substring(downloadUrl.lastIndexOf(".")));
    }

    private static final int UPDATE_DB_PER_SIZE = 102400;
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        downloadState = DownloadState.INITIALIZE;
        onDownloadStart();
    }

    @Override
    protected Void doInBackground(Executor... params) {
        try {
            downloadState = DownloadState.DOWNLOADING;
            doDownload();
        } catch (IOException e) {
            e.printStackTrace();
            downloadState = DownloadState.FAILED;
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        super.onProgressUpdate(values);
        long total = values[0];
        long finished = values[1];
        long speed = values[2];
        Log.d("DownloadTask", "total="+total+"--finished"+finished+"---speed"+speed);
        onDownloadProgress(finished, total, speed);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if(downloadState == DownloadState.FINISHED) {
            onDownloadFinish(dirPath+File.separator+fileName);
        } else if(downloadState == DownloadState.FAILED) {
            onDownloadFail();
        } else if(downloadState == DownloadState.PAUSE) {
            onDownloadPause();
        } else if(downloadState == DownloadState.STOP) {
            onDownloadStop();
        }
    }

    private HttpURLConnection getConnection() throws IOException {
        URL url = new URL(downloadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setUseCaches(true);
        return conn;
    }
    private static final int BUFFER_SIZE = 8192;

    /**
     * pause flag
     */
    private volatile boolean mPause = false;

    /**
     * stop flag, not used now.
     */
    private volatile boolean mStop = false;

    private void doDownload() throws IOException {
        HttpURLConnection connection = getConnection();
        connection.connect();
        int totalSize = connection.getContentLength();
        mimeType = connection.getContentType();
        RandomAccessFile raf = new RandomAccessFile(targetFile, "rw");
        InputStream is = connection.getInputStream();
        byte[] buffer = new byte[BUFFER_SIZE];

        long speed = 0;
        int count = 0;
        long downloadSize = 0;
        long prevTime = System.currentTimeMillis();
        long achieveSize = downloadSize;
        while ((count = is.read(buffer)) != -1) {
            if(mPause) {
                downloadState = DownloadState.PAUSE;
                //更新数据库
                break;
            }
            if(mStop) {
                downloadState = DownloadState.STOP;
                //删除数据库
                break;
            }
            raf.write(buffer, 0, count);
            downloadSize += count;
            long tempSize = downloadSize - achieveSize;
            if (tempSize > UPDATE_DB_PER_SIZE) {
                long tempTime = System.currentTimeMillis() - prevTime;
//                long speed = tempSize * 1000 / tempTime;
                achieveSize = downloadSize;
                prevTime = System.currentTimeMillis();
                speed =  (downloadSize/(System.currentTimeMillis() + 1 - prevTime));
                publishProgress(Long.valueOf(totalSize), Long.valueOf(downloadSize), Long.valueOf(speed));
            }
        }
        if(!mPause && ! mStop) {
            downloadState = DownloadState.FINISHED;
        }
    }

    public void startDownload() {
        if(saveFile.exists()) {
            onDownloadFinish(dirPath + fileName);
            return;
        }
        mPause = false;
        mStop = false;
//        this.execute();
        this.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//        this.execute(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void pauseDownload() {
        mPause = true;
        mStop = false;
    }

    public void stopDownload() {
        mPause = false;
        mStop = true;
    }

    @Override
    public void onDownloadFinish(String filepath) {
        targetFile.renameTo(saveFile);
        Toast.makeText(context, "onDownloadFinish", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDownloadStart() {
        Toast.makeText(context, "onDownloadStart", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDownloadPause() {
        Toast.makeText(context, "onDownloadPause", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDownloadStop() {
        Toast.makeText(context, "onDownloadStop", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDownloadFail() {
        Toast.makeText(context, "onDownloadFail", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDownloadProgress(long finishedSize, long totalSize, long speed) {
//        Toast.makeText(context, "onDownloadProgress", Toast.LENGTH_SHORT).show();
    }
}
