package com.wangxin.mycamera2.control;

import static com.wangxin.mycamera2.model.Config.STATE_PREVIEW;
import static com.wangxin.mycamera2.model.Config.STATE_WAITING_PRECAPTURE;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;

import com.wangxin.mycamera2.model.CameraAttributes;
import com.wangxin.mycamera2.model.MethodTool;

public class FocusController {

    //锁定焦点 wx1
    public static void lockFocus(CameraAttributes cameraAttributes,MethodTool methodTool,CameraCaptureSession.CaptureCallback mCaptureCallback) {
        try {
            // 相机锁定焦点的方法
            cameraAttributes.getPreviewBuilder().set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            //告诉 #mCaptureCallback 等待锁定。
            cameraAttributes.setState(STATE_WAITING_PRECAPTURE);
            cameraAttributes.getCaptureSession().capture(cameraAttributes.getPreviewBuilder().build(), mCaptureCallback,
                    methodTool.getBackgroundThread().mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //解锁焦点  wx9
    public static void unlockFocus(CameraAttributes cameraAttributes,MethodTool methodTool,CameraCaptureSession.CaptureCallback mCaptureCallback) {
        try {
            // Reset the auto-focus trigger
            cameraAttributes.getPreviewBuilder().set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            cameraAttributes.getPreviewBuilder().set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            cameraAttributes.setState(STATE_PREVIEW);
            cameraAttributes.getCaptureSession().setRepeatingRequest(cameraAttributes.getPreviewBuilder().build(), mCaptureCallback, methodTool.getBackgroundThread().mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

}
