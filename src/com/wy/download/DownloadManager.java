package com.wy.download;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.URLUtil;

/*
 * 单例的下载管理器，用于管理所有的下载任务
*/

public class DownloadManager {
    private static final String TAG = "DownloadManager";
    private Context mContext;
    private static DownloadManager instance;
     /* *
     * 获得一个单例的下载管理器
     * @param context Context
     * @return DownloadManager instance
    */
    public static DownloadManager getInstance(Context context) {
        if (instance == null) {
            synchronized (DownloadManager.class) {
                if (instance == null) {
                    instance = new DownloadManager(context);
                }
            }
        }
        return instance;
    }
    /**
     * 私有的构造方法
     * @param context
     */
    private DownloadManager(Context context) {
        this.mContext = context;
        mDownloadMap = new HashMap<String, DownloadTask>();
        mDownloadListenerMap = new HashMap<String, CopyOnWriteArraySet<DownloadListener>>();
        // 数据库操作对象实例化
        mDownloadDBHelper = new DownloadDBHelper(context, "download.db");
    }

    private static int mMaxTask = 0;
    /**
     * 下载的数据库管理器
     */
    private DownloadDBHelper mDownloadDBHelper;
    private HashMap<String, DownloadTask> mDownloadMap;
    private HashMap<String, CopyOnWriteArraySet<DownloadListener>> mDownloadListenerMap;

    /**
     * 开始一个下载任务，如果一个相同的下载任务已经存在，将回退出，留下一个“任务存在”的日志
     * @param downloadUrl
     * @param listener
     */
    public void startDownload(String downloadUrl, DownloadListener listener) {
        if (TextUtils.isEmpty(downloadUrl) || !URLUtil.isHttpUrl(downloadUrl)) {
            Log.w(TAG, "invalid http url: " + downloadUrl);
            throw new IllegalArgumentException("invalid http url");
        }
        if (mDownloadMap.containsKey(downloadUrl)) {
            Log.w(TAG, "task existed");
            return;
        }
        if (mMaxTask > 0 && mDownloadMap.size() > mMaxTask) {
            Log.w(TAG, "trial version can only add " + mMaxTask + " download task, please buy  a lincense");
            return;
        }
        if (null == mDownloadListenerMap.get(downloadUrl)) {
            CopyOnWriteArraySet<DownloadListener> set = new CopyOnWriteArraySet<DownloadListener>();
            mDownloadListenerMap.put(downloadUrl, set);
        }
        // 保存到数据库，如果下载任务是有效的，并开始下载。
        /*if (!downloadTask.equals(queryDownloadTask(downloadUrl))) {
            insertDownloadTask(downloadTask);
        }*/

        DownloadTask task = DownloadTask.buildTask(mContext, downloadUrl);
        mDownloadMap.put(downloadUrl, task);
        task.startDownload();
    }

    /*
     * 暂停下载任务
     *
     * @param downloadTask DownloadTask
    */
    public void pauseDownload(String downloadUrl) {
        if (mDownloadMap.containsKey(downloadUrl)) {
            mDownloadMap.get(downloadUrl).pauseDownload();
            //mDownloadMap.remove(downloadTask);
        }
    }

    /*
     * 继续或者重新开始一个下载任务
     * @param downloadUrl
    */
    public void continueDownload(String downloadUrl) {
        if (null == downloadUrl || !URLUtil.isHttpUrl(downloadUrl)) {
            Log.w(TAG, "invalid http url: " + downloadUrl);
            throw new IllegalArgumentException("invalid http url");
        }
        if (null == mDownloadListenerMap.get(downloadUrl)) {
            CopyOnWriteArraySet<DownloadListener> set = new CopyOnWriteArraySet<DownloadListener>();
            mDownloadListenerMap.put(downloadUrl, set);
        }
        //保存到数据库，如果下载任务是有效的，并开始下载。
        /*if (!downloadTask.equals(queryDownloadTask(downloadTask.url))) {
            insertDownloadTask(downloadTask);
        }*/
        DownloadTask task = DownloadTask.buildTask(mContext, downloadUrl);
        mDownloadMap.put(downloadUrl, task);
        task.startDownload();
    }

    /*
     * 停止任务，这种方法不使用。请使用pausedownload相反。
     * @param downloadTask DownloadTask
    */

    @Deprecated
    public void stopDownload(String downloadUrl) {
        mDownloadMap.get(downloadUrl).stopDownload();
        mDownloadMap.remove(downloadUrl);
    }

    /*
     * 从数据库中获取所有的下载任务
     *
     * @return DownloadTask list
    */
    public List<DownloadTask> getAllDownloadTask() {
        return mDownloadDBHelper.queryAll();
    }

    /*
     * 从数据库中获取所有的下载任务
     * @return DownloadTask list
    */
    public List<DownloadTask> getDownloadingTask() {
        return mDownloadDBHelper.queryUnDownloaded();
    }

    /*
     * 从数据库中获取所有的下载任务
     *
     * @return DownloadTask list
    */

    public List<DownloadTask> getFinishedDownloadTask() {
        return mDownloadDBHelper.queryDownloaded();
    }

    /*
     * 将下载任务插入到数据库中
     *
     * @param downloadTask
    */

    void insertDownloadTask(DownloadTask downloadTask) {
        mDownloadDBHelper.insert(downloadTask);
    }

    /*
     * 更新下载任务到数据库
     *
     * @param downloadTask
    */
    void updateDownloadTask(DownloadTask downloadTask) {
        mDownloadDBHelper.update(downloadTask);
    }

    /*
     * 删除从下载队列下载任务，删除它的侦听器，并从数据库中删除它。
     *
     * @param downloadTask
    */
    public void deleteDownloadTask(DownloadTask downloadTask) {
        if (downloadTask.downloadState != DownloadState.FINISHED) {
            for (DownloadListener l : getListeners(downloadTask.downloadUrl)) {
                l.onDownloadStop();
            }
            getListeners(downloadTask.downloadUrl).clear();
        }
        mDownloadMap.remove(downloadTask);
        mDownloadListenerMap.remove(downloadTask);
        mDownloadDBHelper.delete(downloadTask);
    }

    /*
     * 删除下载任务的下载文件。
     *
     * @param downloadTask
    */
    public void deleteDownloadTaskFile(DownloadTask downloadTask) {
        deleteFile(downloadTask.dirPath + "/" + downloadTask.fileName);
    }

    /*
     * 查询从数据库中下载的任务，根据网址。
     *
     * @param url 下载url
     * @return DownloadTask
    */
    DownloadTask queryDownloadTask(String url) {
        return mDownloadDBHelper.query(url);
    }

    /*
     * 查询下载任务已运行。
     *
     * @param downloadTask
     * @return
    */
    public boolean existRunningTask(DownloadTask downloadTask) {
        return mDownloadMap.containsKey(downloadTask);
    }

    /*
     * 获得所有的下载任务的侦听器
     * @param downloadUrl
     * @return
    */
    public CopyOnWriteArraySet<DownloadListener> getListeners(String downloadUrl) {
        if (null != mDownloadListenerMap.get(downloadUrl)) {
            return mDownloadListenerMap.get(downloadUrl);
        } else {
            return new CopyOnWriteArraySet<DownloadListener>();//avoid null pointer exception
        }
    }

    /*
     * 注册downloadlistener到downloadtask。
     * 你可以注册多个downloadlistener在任何时间downloadtask。
     * 如注册一个侦听器来更新您自己的进度条，在文件下载完毕后做某事。
     * @param downloadTask
     * @param listener
    */
    public void registerListener(DownloadTask downloadTask, DownloadListener listener) {
        if (null != mDownloadListenerMap.get(downloadTask)) {
            mDownloadListenerMap.get(downloadTask).add(listener);
            Log.d(TAG, downloadTask.fileName + " addListener ");
        } else {
            CopyOnWriteArraySet<DownloadListener> set = new CopyOnWriteArraySet<DownloadListener>();
            mDownloadListenerMap.put(downloadTask.downloadUrl, set);
            mDownloadListenerMap.get(downloadTask).add(listener);
        }
    }

    /*
     * 从downloadtask移除侦听器，您不需要手动调用此方法。
     * @param downloadTask
    */
    public void removeListener(DownloadTask downloadTask) {
        mDownloadListenerMap.remove(downloadTask);
    }

    /*
     * 删除一个文件
     * @param filePath
     * @return
    */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    /*
     * 如果数据库中存在的网址和下载状态是完成的，并且该文件存在，返回true
     * @param url
     * @return
    */
    public boolean isUrlDownloaded(String url) {
        boolean re = false;
        DownloadTask task = mDownloadDBHelper.query(url);
        if (null != task) {
            if (task.downloadState == DownloadState.FINISHED) {
                File file = new File(task.dirPath + "/" + task.fileName);
                if (file.exists()) {
                    re = true;
                }
            }
        }
        return re;
    }
}
