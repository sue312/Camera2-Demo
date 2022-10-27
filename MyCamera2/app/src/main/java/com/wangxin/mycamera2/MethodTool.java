package com.wangxin.mycamera2;

public class MethodTool {

    private BackgroundThread backgroundThread;
    private CameraOrientationListener orientationListener;
    private CameraAttributes cameraAttributes;

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

    public CameraAttributes getCameraAttributes() {
        return cameraAttributes;
    }

    public void setCameraAttributes(CameraAttributes cameraAttributes) {
        this.cameraAttributes = cameraAttributes;
    }
}
