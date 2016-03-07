
package com.wy.download;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DownloadDBHelper extends SQLiteOpenHelper {
    /*
     * debug tag.
    */
    private static final String TAG = "DownloadDBHelper";

    /*
     * 表名:download
    */
    private static final String TABLE_NAME = "download";

    /*
     * 表中字段[插入数据库时系统生成的id]
    */
    private static final String FIELD_ID = "_id";

    /*
     * 表中字段[下载url]
    */
    private static final String FIELD_URL = "url";

    /*
     * 表中字段[下载状态]
    */
    private static final String FIELD_DOWNLOAD_STATE = "downloadState";

    /*
     * 表中字段[文件放置路径]
    */
    private static final String FIELD_FILEPATH = "filepath";

    /*
     * 表中字段[文件名]
    */
    private static final String FIELD_FILENAME = "filename";

    /*
     * 表中字段[已完成文件大小]
    */
    private static final String FIELD_FINISHED_SIZE = "finishedSize";

    /*
     * 表中字段[文件总大小]
    */
    private static final String FIELD_TOTAL_SIZE = "totalSize";

    /*
     * Constructor
     * @param context Context
     * @param name 数据库文件名（.db）由调用者提供
    */
    private Context mContext;
    public DownloadDBHelper(Context context, String name) {
        super(context, name, null, 1);
        mContext = context;
    }

    /*
     * 当数据库被首次创建时执行该方法<BR>
     * 创建表等初始化操作在该方法中执行，调用execSQL方法创建表
     * @param db SQLiteDatabase
     * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
    */
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "create download table.");
        StringBuffer buffer = new StringBuffer("create table ");
        buffer.append(TABLE_NAME);
        buffer.append("(");
        buffer.append(FIELD_ID);
        buffer.append(" integer primary key autoincrement, ");
        buffer.append(FIELD_URL);
        buffer.append(" text unique, ");
        buffer.append(FIELD_DOWNLOAD_STATE);
        buffer.append(" text,");
        buffer.append(FIELD_FILEPATH);
        buffer.append(" text, ");
        buffer.append(FIELD_FILENAME);
        buffer.append(" text, ");
        buffer.append(FIELD_FINISHED_SIZE);
        buffer.append(" integer, ");
        buffer.append(FIELD_TOTAL_SIZE);
        buffer.append(" integer)");
        String sql = buffer.toString();
        Log.i(TAG, sql);
        db.execSQL(sql);
    }

    /*
     * 当打开数据库时传入的版本号与当前的版本号不同时会调用该方法。<BR>
     * @param db The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version
     * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase,
     *      int, int)
    */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /*
     * 存入一条下载任务（直接存入数据库）<BR>
     * @param downloadTask DownloadTask
    */
    public void insert(DownloadTask downloadTask) {
        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_NAME, null, task2ContentValues(downloadTask));
    }

    /*
     * 根据url查询数据库中相应的下载任务<BR>
     * @param url
     * @return DownloadTask
    */
    public DownloadTask query(String url) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME,
                new String[]{FIELD_URL, FIELD_DOWNLOAD_STATE, FIELD_FILEPATH, FIELD_FILENAME, FIELD_FINISHED_SIZE, FIELD_TOTAL_SIZE },
                FIELD_URL + "=?", new String[]{url}, null, null, null);
        if(cursor == null) {
            return null;
        }
        DownloadTask task = null;
        if (cursor.moveToNext()) {
            task = cursor2Task(cursor);
        }
        cursor.close();
        return task;
    }

    /**
     * 查询到的数据构建成为task
     * @param cursor
     * @return
     */
    private DownloadTask cursor2Task(Cursor cursor) {
        DownloadTask task = new DownloadTask(mContext);
        task.downloadUrl = cursor.getString(cursor.getColumnIndex(FIELD_URL));
        task.downState = cursor.getString(cursor.getColumnIndex(FIELD_DOWNLOAD_STATE));
        task.dirPath = cursor.getString(cursor.getColumnIndex(FIELD_FILEPATH));
        task.fileName = cursor.getString(cursor.getColumnIndex(FIELD_FILENAME));
        task.finishSize = cursor.getInt(cursor.getColumnIndex(FIELD_FINISHED_SIZE));
        task.totalSize = cursor.getInt(cursor.getColumnIndex(FIELD_TOTAL_SIZE));
        return task;
    }

    /*
     * 查询数据库中所有下载任务集合<BR>
     * @return 下载任务List
    */
    public List<DownloadTask> queryAll() {
        List<DownloadTask> tasks = new ArrayList<DownloadTask>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{
                FIELD_URL, FIELD_DOWNLOAD_STATE, FIELD_FILEPATH, FIELD_FILENAME, FIELD_FINISHED_SIZE, FIELD_TOTAL_SIZE
        }, null, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DownloadTask task = cursor2Task(cursor);
                tasks.add(task);
            }
            cursor.close();
        }
        return tasks;
    }

    public List<DownloadTask> queryDownloaded() {
        List<DownloadTask> tasks = new ArrayList<DownloadTask>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{
                FIELD_URL, FIELD_DOWNLOAD_STATE, FIELD_FILEPATH, FIELD_FILENAME, FIELD_FINISHED_SIZE, FIELD_TOTAL_SIZE
        }, FIELD_DOWNLOAD_STATE + "='FINISHED'", null, null, null, "_id desc");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DownloadTask task = cursor2Task(cursor);
                tasks.add(task);
            }
            cursor.close();
        }

        return tasks;
    }

    public List<DownloadTask> queryUnDownloaded() {
        List<DownloadTask> tasks = new ArrayList<DownloadTask>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{
                FIELD_URL, FIELD_DOWNLOAD_STATE, FIELD_FILEPATH, FIELD_FILENAME, FIELD_FINISHED_SIZE, FIELD_TOTAL_SIZE
        }, FIELD_DOWNLOAD_STATE + "<> 'FINISHED'", null, null, null, "_id desc");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DownloadTask task = cursor2Task(cursor);
                tasks.add(task);
            }
            cursor.close();
        }

        return tasks;
    }

    /*
     * 更新下载任务<BR>
     * @param downloadTask DownloadTask
    */
    public void update(DownloadTask downloadTask) {
        SQLiteDatabase db = getWritableDatabase();
        db.update(TABLE_NAME, task2ContentValues(downloadTask), FIELD_URL + "=?", new String[]{downloadTask.downloadUrl });
    }

    /*
     * 从数据库中删除一条下载任务<BR>
     * @param downloadTask DownloadTask
    */
    public void delete(DownloadTask downloadTask) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, FIELD_URL + "=?", new String[]{downloadTask.downloadUrl });
    }

    /*
     * 将DownloadTask转化成ContentValues<BR>
     * @param downloadTask DownloadTask
     * @return ContentValues
     */
    private ContentValues task2ContentValues(DownloadTask downloadTask) {
        ContentValues values = new ContentValues();
        values.put(FIELD_URL, downloadTask.downloadUrl);
        values.put(FIELD_DOWNLOAD_STATE, downloadTask.downloadState.toString());
        values.put(FIELD_FILEPATH, downloadTask.dirPath);
        values.put(FIELD_FILENAME, downloadTask.fileName);
        values.put(FIELD_FINISHED_SIZE, downloadTask.finishSize);
        values.put(FIELD_TOTAL_SIZE, downloadTask.totalSize);
        return values;
    }
}
