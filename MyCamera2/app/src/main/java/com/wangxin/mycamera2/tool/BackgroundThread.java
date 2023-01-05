package com.wangxin.mycamera2.tool;

import android.os.Handler;
import android.os.HandlerThread;

public class BackgroundThread {

    public HandlerThread mBackgroundThread;
    public Handler mBackgroundHandler;

    public BackgroundThread() {
    }

    //启动后台线程   wx2
    public void startBackgroundThread() {
        //后台线程
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        //后台处理程序
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    //停止后台线程 wx0
    public void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
