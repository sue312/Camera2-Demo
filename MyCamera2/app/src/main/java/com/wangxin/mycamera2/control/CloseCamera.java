package com.wangxin.mycamera2.control;

import com.wangxin.mycamera2.model.CameraAttributes;

public class CloseCamera {

    //关闭Camera  wx0
    public static void closeCamera(CameraAttributes cameraAttributes) {
        try {
            cameraAttributes.getCameraOpenCloseLock().acquire();
            if (null != cameraAttributes.getCaptureSession()) {
                cameraAttributes.getCaptureSession().close();
                cameraAttributes.setCaptureSession(null);
            }
            if (null != cameraAttributes.getCameraDevice()) {
                cameraAttributes.getCameraDevice().close();
                cameraAttributes.setCameraDevice(null);
            }
            if (null != cameraAttributes.getImageReader()) {
                cameraAttributes.getImageReader().close();
                cameraAttributes.setImageReader(null);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            cameraAttributes.getCameraOpenCloseLock().release();
        }
    }

}
