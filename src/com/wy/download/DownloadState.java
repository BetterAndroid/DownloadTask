
package com.wy.download;

/**
 * 下载状态标记
 */
public enum DownloadState {
    /**
     * init
     */
    INITIALIZE,

    /**
     * downloading
     */
    DOWNLOADING,
    /**
     * download failed, the reason may be network error, file io error etc.
     */
    FAILED,
    /**
     * download finished
     */
    FINISHED,

    /**
     * download paused
     */
    PAUSE,

    /**
     * download stoped
     */
    STOP
}
