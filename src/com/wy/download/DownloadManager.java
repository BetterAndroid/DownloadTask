package com.wy.download;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.URLUtil;

/*
 * 单例的下载管理器，用于管理所有的下载任务
*/
public class DownloadManager implements DownloadListener{
    private static final String TAG = DownloadManager.class.getSimpleName();
    private Context mContext;

    /**
     * 下载的数据库管理器
     */
    private MyDBManager myDb;
    private HashMap<String, DownloadTask> mDownloadMap = new HashMap<String, DownloadTask>();
    private HashMap<String, CopyOnWriteArraySet<DownloadListener>> mDownloadListenerMap = new HashMap<String, CopyOnWriteArraySet<DownloadListener>>();
    ;

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


    private Handler mDbHandler = null;
    private static final int DB_INSERT = 0x1001;
    private static final int DB_DELETE = 0x1002;
    private static final int DB_UPDATE = 0x1003;
    private static final int DB_QUERY_ONE = 0x1004;
    private static final int DB_QUERY_ALL = 0x1005;
    private static final int DB_QUERY_DOWNLOAD = 0x1006;
    private static final int DB_QUERY_UNDOWNLOAD = 0x1007;

    /**
     * 私有的构造方法
     *
     * @param context
     */
    private DownloadManager(Context context) {
        this.mContext = context;
        // 数据库操作对象实例化
        myDb = MyDBManager.getInstance(context);
        HandlerThread ht = new HandlerThread("download-db-handler");
        ht.start();
        mDbHandler = new Handler(ht.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                DownloadTask task;
                String downloadUrl;
                Message uiMsg;
                switch (msg.what) {
                    case DB_INSERT:
                        task = (DownloadTask) msg.obj;
                        myDb.insert(task);
                        uiMsg = Message.obtain();
                        uiMsg.what = UI_INSERT;
                        uiMsg.obj = task;
                        mUiHandler.sendMessage(uiMsg);
                        break;
                    case DB_DELETE:
                        downloadUrl = msg.obj.toString();
                        myDb.delete(downloadUrl);
                        uiMsg = Message.obtain();
                        uiMsg.what = UI_DELETE;
                        uiMsg.obj = downloadUrl;
                        mUiHandler.sendMessage(uiMsg);
                        break;
                    case DB_UPDATE:
                        task = (DownloadTask) msg.obj;
                        myDb.update(task);
                        uiMsg = Message.obtain();
                        uiMsg.what = UI_DELETE;
                        uiMsg.obj = task;
                        mUiHandler.sendMessage(uiMsg);
                        break;
                    case DB_QUERY_ONE:
                        downloadUrl = msg.obj.toString();
                        task = myDb.query(downloadUrl);
                        uiMsg = Message.obtain();
                        uiMsg.what = UI_QUERY_ONE;
                        uiMsg.obj = task;
                        mUiHandler.sendMessage(uiMsg);
                        break;
                    case DB_QUERY_ALL:

                        break;
                    case DB_QUERY_DOWNLOAD:

                        break;
                    case DB_QUERY_UNDOWNLOAD:

                        break;

                }
            }
        };
    }

    private static final int UI_INSERT = 2001;
    private static final int UI_DELETE = 2002;
    private static final int UI_UPDATE = 2003;
    private static final int UI_QUERY_ONE = 2004;
    private static final int UI_QUERY_ALL = 2005;
    private static final int UI_QUERY_DOWNLOAD = 2006;
    private static final int UI_QUERY_UNDOWNLOAD = 2007;

    private final Handler mUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            DownloadTask task;
            String downloadUrl;
            switch (msg.what) {
                case UI_INSERT:
                    task = (DownloadTask) msg.obj;
                    downloadUrl = task.downloadUrl;
                    mDownloadMap.put(downloadUrl, task);
                    task.startDownload();
                    break;
                case UI_DELETE:
                    downloadUrl = msg.obj.toString();
                    task = mDownloadMap.get(downloadUrl);
                    if (task.downloadState != DownloadTask.FINISHED) {
                        CopyOnWriteArraySet<DownloadListener> cs = mDownloadListenerMap.get(downloadUrl);
                        if(cs != null) {
                            Iterator<DownloadListener> iterator = mDownloadListenerMap.get(downloadUrl).iterator();
                            while (iterator.hasNext()) {
                                DownloadListener listener = iterator.next();
                                if(listener == null) {
                                    iterator.remove();
                                } else {
                                    listener.onDownloadStop(task);
                                }
                            }
                            cs.clear();
                        }

                    }
                    mDownloadMap.remove(task.downloadUrl);
                    mDownloadListenerMap.remove(task.downloadUrl);
                    deleteFile(task.dirPath + "/" + task.fileName + "." + DownloadTask.cachSuffix);
                    break;
                case UI_UPDATE:
                    task = (DownloadTask) msg.obj;
                    CopyOnWriteArraySet<DownloadListener> ds = mDownloadListenerMap.get(task.downloadUrl);
                    Iterator<DownloadListener> iterator = ds.iterator();
                    while (iterator.hasNext()) {
                        DownloadListener listener = iterator.next();
                        if (listener == null) {
                            iterator.remove();
                        } else {
                            listener.onDownloadProgress(task.finishSize, task.totalSize, 0);
                        }
                    }
                    break;
                case UI_QUERY_ONE:
                    // 保存到数据库，如果下载任务是有效的，并开始下载。
                    task = (DownloadTask) msg.obj;
                    downloadUrl = task.downloadUrl;
                    if (task == null) {
                        task = DownloadTask.buildTask(mContext, downloadUrl);
                        saveDownloadTask(task);
                    } else {
                        task.targetFile = DownloadTask.setTargetFile(task.dirPath, task.fileName);
                        task.saveFile = DownloadTask.setSaveFile(task.dirPath, task.fileName, downloadUrl);
                        mDownloadMap.put(downloadUrl, task);
                        task.startDownload();
                    }
                    /*if (mDownloadCallback != null && mDownloadCallback.size() > 0) {
                        Iterator<DownloadCallback> iteratorQuery = mDownloadCallback.iterator();
                        while (iteratorQuery.hasNext()) {
                            DownloadCallback cb = iteratorQuery.next();
                            if (cb == null) {
                                iteratorQuery.remove();
                            } else {
                                cb.onQueryTask((DownloadTask) msg.obj);
                            }
                        }
                    }*/
                    break;
                case UI_QUERY_ALL:
                    if (mDownloadCallback != null && mDownloadCallback.size() > 0) {
                        Iterator<DownloadCallback> iteratorQuery = mDownloadCallback.iterator();
                        while (iteratorQuery.hasNext()) {
                            DownloadCallback cb = iteratorQuery.next();
                            if (cb == null) {
                                iteratorQuery.remove();
                            } else {
                                cb.onQueryAll((List<DownloadTask>) msg.obj);
                            }
                        }
                    }
                    break;
                case UI_QUERY_DOWNLOAD:
                    if (mDownloadCallback != null && mDownloadCallback.size() > 0) {
                        Iterator<DownloadCallback> iteratorQuery = mDownloadCallback.iterator();
                        while (iteratorQuery.hasNext()) {
                            DownloadCallback cb = iteratorQuery.next();
                            if (cb == null) {
                                iteratorQuery.remove();
                            } else {
                                cb.onQueryAllDownload((List<DownloadTask>) msg.obj);
                            }
                        }
                    }
                    break;
                case UI_QUERY_UNDOWNLOAD:
                    if (mDownloadCallback != null && mDownloadCallback.size() > 0) {
                        Iterator<DownloadCallback> iteratorQuery = mDownloadCallback.iterator();
                        while (iteratorQuery.hasNext()) {
                            DownloadCallback cb = iteratorQuery.next();
                            if (cb == null) {
                                iteratorQuery.remove();
                            } else {
                                cb.onQueryAllUnDownload((List<DownloadTask>) msg.obj);
                            }
                        }
                    }
                    break;

            }
        }
    };


    /**
     * dd
     * 开始一个下载任务，如果一个相同的下载任务已经存在，将回退出，留下一个“任务存在”的日志
     *
     * @param downloadUrl
     * @param listener
     */
    public void startDownload(String downloadUrl, DownloadListener listener) {
        if (TextUtils.isEmpty(downloadUrl) || !URLUtil.isHttpUrl(downloadUrl)) {
            throw new IllegalArgumentException("invalid http url");
        }
        addDownloadListener(downloadUrl, listener);
        queryDownloadTask(downloadUrl);
    }

    /*
     * 注册downloadlistener到downloadtask。
     * 你可以注册多个downloadlistener在任何时间downloadtask。
     * 如注册一个侦听器来更新您自己的进度条，在文件下载完毕后做某事。
     * @param downloadTask
     * @param listener
    */
    private void addDownloadListener(String downloadUrl, DownloadListener listener) {
        CopyOnWriteArraySet<DownloadListener> cs = mDownloadListenerMap.get(downloadUrl);
        if (cs == null) {
            cs = new CopyOnWriteArraySet<DownloadListener>();
            mDownloadListenerMap.put(downloadUrl, cs);
        }
        cs.add(listener);
    }

    private void removeDownloadListener(String downloadUrl, DownloadListener listener) {
        CopyOnWriteArraySet<DownloadListener> cs = mDownloadListenerMap.get(downloadUrl);
        if (cs == null) {
            return;
        } else {
            if (cs.size() > 1) {
                cs.remove(listener);
            } else {
                mDownloadListenerMap.remove(downloadUrl);
            }
        }
    }

    /**
     * 从downloadtask移除侦听器，您不需要手动调用此方法。
     * @param downloadUrl
     */
    private void removeDownloadListener(String downloadUrl) {
        mDownloadListenerMap.remove(downloadUrl);
    }

    /*
      * 保存下载任务到数据库
      * @param downloadTask
     */
    public void saveDownloadTask(DownloadTask task) {
        Message msg = Message.obtain();
        msg.what = DB_INSERT;
        msg.obj = task;
        mDbHandler.sendMessage(msg);
    }

    /*
     * 更新下载任务到数据库
     * @param downloadTask
    */
    public void updateDownloadTask(DownloadTask task) {
        Message msg = Message.obtain();
        msg.what = DB_UPDATE;
        msg.obj = task;
        mDbHandler.sendMessage(msg);
    }

    /*
     * 删除从下载队列下载任务，删除它的侦听器，并从数据库中删除它。
     * @param downloadTask
    */
    public void deleteDownloadTask(String downloadUrl) {
        Message msg = Message.obtain();
        msg.what = DB_DELETE;
        msg.obj = downloadUrl;
        mDbHandler.sendMessage(msg);
    }

    /*
     * 查询一条下载任务
     * @param downloadTask
    */
    public void queryDownloadTask(String downloadUrl) {
        Message msg = Message.obtain();
        msg.what = DB_QUERY_ONE;
        msg.obj = downloadUrl;
        mDbHandler.sendMessage(msg);
    }

    /*
     * 查询全部下载任务
     * @param downloadTask
    */
    public void queryAllDownloadTask() {
        mDbHandler.sendEmptyMessage(DB_QUERY_ALL);
    }

    /*
     * 查询全部正在下载任务
     * @param downloadTask
    */
    public void queryAllDownloadingTask() {
        mDbHandler.sendEmptyMessage(DB_QUERY_UNDOWNLOAD);
    }

    /*
     * 查询全部下载完成任务
     * @param downloadTask
     */
    public void queryAllDownloadedTask() {
        mDbHandler.sendEmptyMessage(DB_QUERY_DOWNLOAD);
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
        if (TextUtils.isEmpty(downloadUrl) || !URLUtil.isHttpUrl(downloadUrl)) {
            throw new IllegalArgumentException("invalid http url");
        }
        queryDownloadTask(downloadUrl);
    }

    /*
     * 停止任务，这种方法暂时不使用。请使用pausedownload相反。
     * @param downloadTask DownloadTask
    */

    @Deprecated
    public void stopDownload(String downloadUrl) {
        if (mDownloadMap.containsKey(downloadUrl)) {
            mDownloadMap.get(downloadUrl).stopDownload();
            mDownloadMap.remove(downloadUrl);
            deleteDownloadTask(downloadUrl);
        }
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
        DownloadTask task = myDb.query(url);
        if (null != task) {
            if (task.downloadState == DownloadTask.FINISHED) {
                File file = new File(task.dirPath + "/" + task.fileName);
                if (file.exists()) {
                    isExisted = true;
                }
            }
        }
        return isExisted;
    }

    private List<DownloadCallback> mDownloadCallback = null;

    public void addDownloadCallback(DownloadCallback cb) {
        if (cb != null) {
            if (mDownloadCallback == null) {
                mDownloadCallback = new LinkedList<DownloadCallback>();
            }
            mDownloadCallback.add(cb);
        }
    }

    public void removeSocialCallback(DownloadCallback cb) {
        if (cb != null && mDownloadCallback != null) {
            mDownloadCallback.remove(cb);
        }
    }

    @Override
    public void onDownloadFinish(DownloadTask task) {

    }

    @Override
    public void onDownloadStart(DownloadTask task) {

    }

    @Override
    public void onDownloadPause(DownloadTask task) {

    }

    @Override
    public void onDownloadStop(DownloadTask task) {

    }

    @Override
    public void onDownloadFail(DownloadTask task) {

    }

    @Override
    public void onDownloadProgress(long finishedSize, long totalSize, long speed) {

    }

    public static class DownloadCallback {
        public void onQueryTask(DownloadTask task) {
        }

        public void onQueryAll(List<DownloadTask> tasks) {
        }

        public void onQueryAllDownload(List<DownloadTask> tasks) {
        }

        public void onQueryAllUnDownload(List<DownloadTask> tasks) {
        }
    }



}
