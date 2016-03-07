
package com.wy;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import com.wy.download.*;

public class DownloadListActivity extends Activity  {
    public static final String DOWNLOADED = "isDownloaded";

    private static final String TAG = "DownloadListActivity";

    private ListView mDownloadingListView;

    private ListView mDownloadedListView;

    private Context mContext;

    private Button mDownloadedBtn;

    private Button mDownloadingBtn;

    List<DownloadTask> mDownloadinglist;

    List<DownloadTask> mDownloadedlist;

    DownloadingAdapter mDownloadingAdapter;

    DownloadingAdapter mDownloadedAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(Res.getInstance(mContext).getLayout("download_list"));

        mContext = this;

        mDownloadingBtn = (Button) findViewById(Res.getInstance(mContext).getId("buttonDownloading"));
        mDownloadedBtn = (Button) findViewById(Res.getInstance(mContext).getId("buttonDownloaded"));
        mDownloadedBtn.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View v) {
                toggleView(true);
            }});
        mDownloadingBtn.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View v) {
                toggleView(false);
            }});

        mDownloadingListView = (ListView) findViewById(Res.getInstance(mContext).getId("downloadingListView"));
        mDownloadedListView = (ListView) findViewById(Res.getInstance(mContext).getId("downloadedListView"));

        toggleView(getIntent().getBooleanExtra(DOWNLOADED, false));

        mDownloadedlist = DownloadManager.getInstance(mContext).getFinishedDownloadTask();
        mDownloadedAdapter = new DownloadingAdapter(DownloadListActivity.this, 0, mDownloadedlist);

        mDownloadedListView.setAdapter(mDownloadedAdapter);
        mDownloadedListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Log.d(TAG, "arg2" + arg2 + " mDownloadedlist" + mDownloadedlist.size());
                onDownloadFinishedClick(mDownloadedlist.get(arg2));
            }
        });
        mDownloadedListView.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int arg2,
                    final long arg3) {
                new AlertDialog.Builder(mContext)
                        .setItems(
                                new String[] {
                                        mContext.getString(Res.getInstance(mContext).getString("download_delete_task")),
                                        mContext.getString(Res.getInstance(mContext).getString("download_delete_task_file"))
                                }, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == 0) {
                                            Toast.makeText(mContext,
                                                    Res.getInstance(mContext).getString("download_deleted_task_ok"),
                                                    Toast.LENGTH_LONG).show();
                                            DownloadManager.getInstance(mContext)
                                                    .deleteDownloadTask(mDownloadedlist.get(arg2));
                                            mDownloadedlist.remove(arg2);
                                            mDownloadedAdapter.notifyDataSetChanged();
                                        } else if (which == 1) {
                                            Toast.makeText(mContext,
                                                    Res.getInstance(mContext).getString("download_deleted_task_file_ok"),
                                                    Toast.LENGTH_LONG).show();
                                            DownloadManager.getInstance(mContext)
                                                    .deleteDownloadTask(mDownloadedlist.get(arg2));
                                            DownloadManager.getInstance(mContext)
                                                    .deleteDownloadTaskFile(mDownloadedlist.get(arg2));
                                            mDownloadedlist.remove(arg2);
                                            mDownloadedAdapter.notifyDataSetChanged();
                                        }

                                    }
                                }).create().show();
                return false;
            }
        });

        // downloading list
        mDownloadinglist = DownloadManager.getInstance(mContext).getDownloadingTask();
        mDownloadingAdapter = new DownloadingAdapter(DownloadListActivity.this, 0, mDownloadinglist);

        mDownloadingListView.setAdapter(mDownloadingAdapter);
        mDownloadingListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                DownloadTask task = mDownloadinglist.get(arg2);
                String mFileName = task.fileName;
                switch (task.downloadState) {
                    case PAUSE:
                        Log.i(TAG, "PAUSE continue " + mFileName);
//                        DownloadManager.getInstance(mContext).continueDownload(task);
                        //addListener(task);
                        break;
                    case FAILED:
                        Log.i(TAG, "FAILED continue " + mFileName);
//                        DownloadManager.getInstance(mContext).continueDownload(task);
                        //addListener(task);
                        break;
                    case DOWNLOADING:
                        Log.i(TAG, "DOWNLOADING pause " + mFileName);
                        DownloadManager.getInstance(mContext).pauseDownload(task.downloadUrl);

                        break;
                    case FINISHED:
                        onDownloadFinishedClick(task);
                        break;
                    case INITIALIZE:

                        break;
                    default:
                        break;
                }

            }
        });

        mDownloadingListView.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int arg2,
                    final long arg3) {
                new AlertDialog.Builder(mContext)
                        .setItems(
                                new String[] {
                                        mContext.getString(Res.getInstance(mContext).getString("download_delete_task")),
                                        mContext.getString(Res.getInstance(mContext).getString("download_delete_task_file"))
                                }, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == 0) {
                                            Toast.makeText(mContext,
                                                    Res.getInstance(mContext).getString("download_deleted_task_ok"),
                                                    Toast.LENGTH_LONG).show();
                                            DownloadManager.getInstance(mContext)
                                                    .deleteDownloadTask(mDownloadinglist.get(arg2));
                                            mDownloadinglist.remove(arg2);
                                            mDownloadingAdapter.notifyDataSetChanged();
                                        } else if (which == 1) {
                                            Toast.makeText(mContext,
                                                    Res.getInstance(mContext).getString("download_deleted_task_file_ok"),
                                                    Toast.LENGTH_LONG).show();
                                            DownloadManager.getInstance(mContext)
                                                    .deleteDownloadTask(mDownloadinglist.get(arg2));
                                            DownloadManager.getInstance(mContext)
                                                    .deleteDownloadTaskFile(mDownloadinglist.get(arg2));
                                            mDownloadinglist.remove(arg2);
                                            mDownloadingAdapter.notifyDataSetChanged();
                                        }

                                    }
                                }).create().show();
                return false;
            }
        });

        for (final DownloadTask task : mDownloadinglist) {
            if (!task.downloadState.equals(DownloadState.FINISHED)) {
                Log.d(TAG, "add listener");
                addListener(task);
            }
        }
        
        //DownloadThread.check(mContext);
    }

    private void toggleView(boolean isShowDownloaded) {
        if (isShowDownloaded) {
            mDownloadedBtn.setBackgroundResource(Res.getInstance(mContext).getDrawable("download_right_tab_selected"));
            mDownloadingBtn.setBackgroundResource(Res.getInstance(mContext).getDrawable("download_left_tab"));
            mDownloadedListView.setVisibility(View.VISIBLE);
            mDownloadingListView.setVisibility(View.GONE);
        } else {
            mDownloadedBtn.setBackgroundResource(Res.getInstance(mContext).getDrawable("download_right_tab"));
            mDownloadingBtn.setBackgroundResource(Res.getInstance(mContext).getDrawable("download_left_tab_selected"));
            mDownloadedListView.setVisibility(View.GONE);
            mDownloadingListView.setVisibility(View.VISIBLE);
        }
    }

    class MyDownloadListener implements DownloadListener {
        private DownloadTask task;

        public MyDownloadListener(DownloadTask downloadTask) {
            task = downloadTask;
        }

        @Override
        public void onDownloadFinish(DownloadTask task) {
            Log.d(TAG, "onDownloadFinish");
            task.downloadState = DownloadState.FINISHED;
            /*task.finishedSize = task.finishedSize;
            task.percent = 1000;*/
            DownloadListActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDownloadingAdapter.notifyDataSetChanged();
//                    mDownloadedAdapter.add(task);
//                    mDownloadingAdapter.remove(task);
                    // toggleView(true);
                }
            });

        }

        @Override
        public void onDownloadStart() {
            task.downloadState = DownloadState.INITIALIZE;
            DownloadListActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDownloadingAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onDownloadPause() {
            Log.d(TAG, "onDownloadPause");
            task.downloadState = DownloadState.PAUSE;
            DownloadListActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDownloadingAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onDownloadStop() {
            Log.d(TAG, "onDownloadStop");
            task.downloadState = DownloadState.PAUSE;
            DownloadListActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDownloadingAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onDownloadFail() {
            Log.d(TAG, "onDownloadFail");
            task.downloadState = DownloadState.FAILED;
            DownloadListActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDownloadingAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onDownloadProgress(long finishedSize, long totalSize, long speed) {
            // Log.d(TAG, "download " + finishedSize);
            task.downloadState = DownloadState.DOWNLOADING;
            /*task.finishedSize = finishedSize;
            task.totalSize = totalSize;
            task.percent = finishedSize*100/totalSize;
            task.speed = speed;*/
            DownloadListActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDownloadingAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    public void addListener(DownloadTask task) {
        DownloadManager.getInstance(mContext).registerListener(task, new MyDownloadListener(task));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        toggleView(intent.getBooleanExtra(DOWNLOADED, false));

        super.onNewIntent(intent);
    }

    /**
     * You can overwrite this method to implement what you want do after download task item is clicked.
     * @param task
     * */


    public void onDownloadFinishedClick(DownloadTask task) {
        Log.d(TAG, task.dirPath + "/"+ task.fileName);
        Intent intent = DownloadOpenFile.openFile(task.dirPath + "/"+ task.fileName);
        if (null == intent) {
            Toast.makeText(mContext, R.string.download_file_not_exist, Toast.LENGTH_LONG).show();
        } else {
            mContext.startActivity(intent);
        }
    }
    

    
    
}
