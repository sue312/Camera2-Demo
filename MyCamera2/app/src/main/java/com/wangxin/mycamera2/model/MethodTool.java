package com.wangxin.mycamera2.model;

import com.wangxin.mycamera2.tool.BackgroundThread;
import com.wangxin.mycamera2.control.CameraOrientationListener;
import com.wangxin.mycamera2.tool.CustomChildThread;

public class MethodTool {

    private BackgroundThread backgroundThread;
    private CameraOrientationListener orientationListener;
    private CustomChildThread customThread;


    public MethodTool() {
    }

    public BackgroundThread getBackgroundThread() {
        return backgroundThread;
    }

    public void setBackgroundThread(BackgroundThread backgroundThread) {
        this.backgroundThread = backgroundThread;
    }

    public CameraOrientationListener getOrientationListener() {
        return orientationListener;
    }

    public void setOrientationListener(CameraOrientationListener orientationListener) {
        this.orientationListener = orientationListener;
    }

    public CustomChildThread getCustomThread() {
        return customThread;
    }

    public void setCustomThread(CustomChildThread customThread) {
        this.customThread = customThread;
    }

}
