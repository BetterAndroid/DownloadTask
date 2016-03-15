
package com.wy.download;

/**
 * 您可以添加多个侦听器来下载任务。和
 * 侦听器可以在下载任务完成后自动删除或删除
 * 下载任务手动。
 */
public interface DownloadListener {
    /**
     * 下载完成
     * @param task
     */
    void onDownloadFinish(DownloadTask task);

    /**
     * 开始下载
     */
    void onDownloadStart(DownloadTask task);

    /**
     * 暂停下载
     */
    void onDownloadPause(DownloadTask task);

    /**
     * 停止下载
     */
    void onDownloadStop(DownloadTask task);

    /**
     * 下载失败
     */
    void onDownloadFail(DownloadTask task);

    /**
     * 下载进度
     * @param finishedSize 已完成的大小
     * @param totalSize 下载的总大小
     * @param speed 下载速度
     */
    void onDownloadProgress(DownloadTask task, long finishedSize, long totalSize, long speed);
}
