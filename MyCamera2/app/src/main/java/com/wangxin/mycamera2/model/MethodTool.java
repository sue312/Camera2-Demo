package com.wangxin.mycamera2.model;

import com.wangxin.mycamera2.control.BackgroundThread;
import com.wangxin.mycamera2.control.CameraOrientationListener;
import com.wangxin.mycamera2.control.CloseCamera;

public class MethodTool {

    private BackgroundThread backgroundThread;
    private CameraOrientationListener orientationListener;
    private CloseCamera closeCamera;


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

    public CloseCamera getCloseCamera() {
        return closeCamera;
    }

    public void setCloseCamera(CloseCamera closeCamera) {
        this.closeCamera = closeCamera;
    }

}
