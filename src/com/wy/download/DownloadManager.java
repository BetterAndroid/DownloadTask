package com.wy.download;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
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
    private static final String DOWNLOADDB = "download.db";
    private Context mContext;

    //    private static int mMaxTask = 0;
    /**
     * 下载的数据库管理器
     */
    private DownloadDBHelper mDownloadDBHelper;
    private HashMap<String, DownloadTask> mDownloadMap = new HashMap<String, DownloadTask>();
    private HashMap<String, CopyOnWriteArraySet<DownloadListener>> mDownloadListenerMap = new HashMap<String, CopyOnWriteArraySet<DownloadListener>>();;

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
        // 数据库操作对象实例化
        mDownloadDBHelper = new DownloadDBHelper(context, DOWNLOADDB);
    }

    /**
     * 开始一个下载任务，如果一个相同的下载任务已经存在，将回退出，留下一个“任务存在”的日志
     * @param downloadUrl
     * @param listener
     */
    public void startDownload(String downloadUrl, DownloadListener listener) {
        if(parseDownloadUrl(downloadUrl)) {
            return;
        }
        if(mDownloadListenerMap.get(downloadUrl) == null) {
            CopyOnWriteArraySet<DownloadListener> dl = new CopyOnWriteArraySet<DownloadListener>();
            mDownloadListenerMap.put(downloadUrl, dl);
        }
        mDownloadListenerMap.get(downloadUrl).add(listener);

        // 保存到数据库，如果下载任务是有效的，并开始下载。
        DownloadTask task = mDownloadDBHelper.query(downloadUrl);
        if(task == null) {
            task = DownloadTask.buildTask(mContext, downloadUrl);
            mDownloadDBHelper.insert(task);
        } else {
            task.targetFile = DownloadTask.setTargetFile(task.dirPath, task.fileName);
            task.saveFile = DownloadTask.setSaveFile(task.dirPath,task.fileName,downloadUrl);
        }
        mDownloadMap.put(downloadUrl, task);
        task.startDownload();
    }

    /**
     * 根据URL判断的状态
     * @param downloadUrl
     * @return
     */
    private boolean parseDownloadUrl(String downloadUrl) {
        if(TextUtils.isEmpty(downloadUrl) || !URLUtil.isHttpUrl(downloadUrl)) {
            throw new IllegalArgumentException("invalid http url");
        }
        if (mDownloadMap.containsKey(downloadUrl)) {
            return true;
        }
        /*if (mMaxTask > 0 && mDownloadMap.size() > mMaxTask) {
            return true;
        }*/
        if (null == mDownloadListenerMap.get(downloadUrl)) {
            CopyOnWriteArraySet<DownloadListener> set = new CopyOnWriteArraySet<DownloadListener>();
            mDownloadListenerMap.put(downloadUrl, set);
        }
        return false;
    }


    /*
     * 暂停下载任务
     * @param downloadTask DownloadTask
    */
    public void pauseDownload(String downloadUrl) {
        if (mDownloadMap.containsKey(downloadUrl)) {
            mDownloadMap.get(downloadUrl).pauseDownload();
            mDownloadMap.remove(downloadUrl);
        }
    }

    /*
     * 继续或者重新开始一个下载任务
     * @param downloadUrl
    */
    public void continueDownload(String downloadUrl) {
        parseDownloadUrl(downloadUrl);
        //保存到数据库，如果下载任务是有效的，并开始下载。
        DownloadTask task = mDownloadDBHelper.query(downloadUrl);
        if(task == null) {
            task = DownloadTask.buildTask(mContext, downloadUrl);
            mDownloadDBHelper.insert(task);
        }
        mDownloadMap.put(downloadUrl, task);
        task.startDownload();
    }

    /*
     * 停止任务，这种方法暂时不使用。请使用pausedownload相反。
     * @param downloadTask DownloadTask
    */

    @Deprecated
    public void stopDownload(String downloadUrl) {
        mDownloadMap.get(downloadUrl).stopDownload();
        mDownloadMap.remove(downloadUrl);
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
     * @return DownloadTask list
    */

    public List<DownloadTask> getFinishedDownloadTask() {
        return mDownloadDBHelper.queryDownloaded();
    }

    /*
     * 更新下载任务到数据库
     * @param downloadTask
    */
    public void updateDownloadTask(DownloadTask downloadTask) {
        mDownloadDBHelper.update(downloadTask);
        CopyOnWriteArraySet<DownloadListener> ds = mDownloadListenerMap.get(downloadTask.downloadUrl);
        Iterator<DownloadListener> iterator = ds.iterator();
        while (iterator.hasNext()) {
            DownloadListener listener = iterator.next();
            if(listener == null) {
                iterator.remove();
            } else {
                listener.onDownloadProgress(downloadTask.finishSize, downloadTask.totalSize, 0);
            }
        }
    }

    /*
     * 删除从下载队列下载任务，删除它的侦听器，并从数据库中删除它。
     * @param downloadTask
    */
    public void deleteDownloadTask(DownloadTask task) {
        if (task.downloadState != DownloadState.FINISHED) {
            for (DownloadListener l : getListeners(task.downloadUrl)) {
                l.onDownloadStop();
            }
            getListeners(task.downloadUrl).clear();
        }
        mDownloadMap.remove(task.downloadUrl);
        mDownloadListenerMap.remove(task.downloadUrl);
        mDownloadDBHelper.delete(task.downloadUrl);
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
     * 查询下载任务已运行。
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
    public void removeDownloadListener(DownloadTask downloadTask) {
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
        boolean isExisted = false;
        DownloadTask task = mDownloadDBHelper.query(url);
        if (null != task) {
            if (task.downloadState == DownloadState.FINISHED) {
                File file = new File(task.dirPath + "/" + task.fileName);
                if (file.exists()) {
                    isExisted = true;
                }
            }
        }
        return isExisted;
    }
}
