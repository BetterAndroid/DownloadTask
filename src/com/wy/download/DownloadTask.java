
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

    /**
     * init
     */
    public static final int INITIALIZE = 0;

    /**
     * downloading
     */
    public static final int DOWNLOADING = 1;
    /**
     * download failed, the reason may be network error, file io error etc.
     */
    private static final int FAILED = 2;
    /**
     * download finished
     */
    public static final int FINISHED = 3;

    /**
     * download paused
     */
    public static final int PAUSE = 4;

    /**
     * download stoped
     */
    public static final int STOP = 5;


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
    private static final String DEFAULT_PATH = Environment.getExternalStorageDirectory().getPath() + File.separator +"Download";
    public String dirPath = DEFAULT_PATH;

    public File saveFile;
    public File targetFile;
    public static final String cachSuffix = "cache";
    public String mimeType;
    public long finishSize = 0;
    public long totalSize;
    public int downloadState = INITIALIZE;//下载的状态

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

    public static File setTargetFile(String dirPath, String fileName) {
        File dir = new File(dirPath);
        if(!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dirPath, fileName + "." + cachSuffix);
    }

    public static File setSaveFile(String dirPath, String fileName, String downloadUrl) {
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
        downloadState = INITIALIZE;
        onDownloadStart(this);
    }

    @Override
    protected Void doInBackground(Executor... params) {
        try {
            downloadState = DOWNLOADING;
            doDownload();
        } catch (IOException e) {
            e.printStackTrace();
            downloadState = FAILED;
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        super.onProgressUpdate(values);
        long total = values[0];
        long finished = values[1];
        long speed = values[2];
//        Log.d("DownloadTask", "total="+total+"--finished"+finished+"---speed"+speed);
        onDownloadProgress(this, finished, total, speed);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if(downloadState == FINISHED) {
            onDownloadFinish(this);
        } else if(downloadState == FAILED) {
            onDownloadFail(this);
        } else if(downloadState == PAUSE) {
            onDownloadPause(this);
        } else if(downloadState == STOP) {
            onDownloadStop(this);
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
        //从数据库中获取
        HttpURLConnection connection = getConnection();
        connection.setRequestMethod("GET");
        if(this.totalSize != 0) {
            connection.setRequestProperty("Range","bytes=" + finishSize + "-" + totalSize);// 设置获取实体数据的范围
        } else {
            this.totalSize = connection.getContentLength();
        }
        connection.connect();
        mimeType = connection.getContentType();
        RandomAccessFile raf = new RandomAccessFile(targetFile, "rwd");
        raf.seek(finishSize);
        InputStream is = connection.getInputStream();
        byte[] buffer = new byte[BUFFER_SIZE];

        long speed = 0;
        int count = 0;
        long downloadSize = finishSize;
        long prevTime = System.currentTimeMillis();
        long achieveSize = downloadSize;
        while ((count = is.read(buffer)) != -1) {
            if(mPause) {
                downloadState = PAUSE;
                //更新数据库
                break;
            }
            if(mStop) {
                downloadState = STOP;
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
            downloadState = FINISHED;
        }
    }

    public void startDownload() {
        if(saveFile.exists()) {
            onDownloadFinish(this);
            return;
        }
        mPause = false;
        mStop = false;
        this.execute();
//        this.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
    public void onDownloadFinish(DownloadTask task) {
        targetFile.renameTo(saveFile);
        finishSize = totalSize;
        DownloadManager.getInstance(context).updateDownloadTask(task);
        DownloadManager.getInstance(context).onDownloadFinish(task);
    }

    @Override
    public void onDownloadStart(DownloadTask task) {
        DownloadManager.getInstance(context).onDownloadStart(task);
    }

    @Override
    public void onDownloadPause(DownloadTask task) {
        DownloadManager.getInstance(context).updateDownloadTask(task);
        DownloadManager.getInstance(context).onDownloadPause(task);
    }

    @Override
    public void onDownloadStop(DownloadTask task) {
        DownloadManager.getInstance(context).updateDownloadTask(task);
        DownloadManager.getInstance(context).onDownloadStop(task);
    }

    @Override
    public void onDownloadFail(DownloadTask task) {
        DownloadManager.getInstance(context).onDownloadFail(task);
    }

    @Override
    public void onDownloadProgress(DownloadTask task, long finishedSize, long totalSize, long speed) {
        this.finishSize = finishedSize;
//        Toast.makeText(context, "onDownloadProgress", Toast.LENGTH_SHORT).show();
//        Log.d(TAG, "finishedSize="+finishedSize + "---totalSize="+totalSize);
        DownloadManager.getInstance(context).updateDownloadTask(this);
    }
}
