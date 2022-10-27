package com.wangxin.mycamera2;

import static com.wangxin.mycamera2.Config.STATE_PREVIEW;
import static com.wangxin.mycamera2.Config.STATE_WAITING_PRECAPTURE;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.util.Log;


import androidx.annotation.NonNull;


public class CameraMeans {

    MethodTool methodTool = new MethodTool();
    private int mOrientation = 0;

    //锁定焦点 wx1
    public void lockFocus() {
        try {
            // 相机锁定焦点的方法
            methodTool.getCameraAttributes().getPreviewBuilder().set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            //告诉 #mCaptureCallback 等待锁定。
            methodTool.getCameraAttributes().setState(STATE_WAITING_PRECAPTURE);
            methodTool.getCameraAttributes().getCaptureSession().capture(methodTool.getCameraAttributes().getPreviewBuilder().build(), mCaptureCallback,
                    methodTool.getBackgroundThread().mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //捕获回调 wx7
    public CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                               TotalCaptureResult result) {
                    methodTool.getCameraAttributes().setCaptureSession(session);
                    checkState(result);
                }

                @Override
                public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                                CaptureResult partialResult) {
                    methodTool.getCameraAttributes().setCaptureSession(session);
                    checkState(partialResult);
                }

                private void checkState(CaptureResult result) {
                    switch (methodTool.getCameraAttributes().getState()) {
                        case STATE_PREVIEW:
                            // 当相机预览正常工作时，我们无事可做。
                            break;
                        case STATE_WAITING_PRECAPTURE:
                            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                            if (afState == null) {
                                captureStillPicture();
                            } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                                // CONTROL_AE_STATE 在某些设备上可以为 null
                                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                                if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                                    //mState = STATE_PICTURE_TAKEN;
                                    captureStillPicture();
                                } else {
                                    //runPrecaptureSequence();//视频拍摄
                                }
                            }
                            break;
                    }
                }

            };

    //捕捉静态图片  wx8
    private void captureStillPicture() {
        try {
            if (null == methodTool.getCameraAttributes().getCameraDevice()) {
                return;
            }
            // 这是我们用来拍照的 Capture RequestBuilder。
            final CaptureRequest.Builder captureBuilder =
                    methodTool.getCameraAttributes().getCameraDevice().createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(methodTool.getCameraAttributes().getImageReader().getSurface());//拍照时，是将mImageReader.getSurface()作为目标

            // 使用与预览相同的 AE 和 AF 模式。
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 方向
            mOrientation = methodTool.getOrientationListener().startOrientationChangeListener(methodTool.getCameraAttributes().getContext());
            Log.d("wangxin666","mOrientation = " + mOrientation);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, mOrientation);

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    unlockFocus();//恢复预览
                }
            };
            methodTool.getCameraAttributes().getCaptureSession().stopRepeating();
            methodTool.getCameraAttributes().getCaptureSession().abortCaptures();
            methodTool.getCameraAttributes().getCaptureSession().capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //解锁焦点  wx9
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            methodTool.getCameraAttributes().getPreviewBuilder().set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            methodTool.getCameraAttributes().getPreviewBuilder().set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            methodTool.getCameraAttributes().setState(STATE_PREVIEW);
            methodTool.getCameraAttributes().getCaptureSession().setRepeatingRequest(methodTool.getCameraAttributes().getPreviewBuilder().build(), mCaptureCallback, methodTool.getBackgroundThread().mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //关闭Camera  wx0
    private void closeCamera() {
        try {
            methodTool.getCameraAttributes().getCameraOpenCloseLock().acquire();
            if (null != methodTool.getCameraAttributes().getCaptureSession()) {
                methodTool.getCameraAttributes().getCaptureSession().close();
                methodTool.getCameraAttributes().setCaptureSession(null);
            }
            if (null != methodTool.getCameraAttributes().getCameraDevice()) {
                methodTool.getCameraAttributes().getCameraDevice().close();
                methodTool.getCameraAttributes().setCameraDevice(null);
            }
            if (null != methodTool.getCameraAttributes().getImageReader()) {
                methodTool.getCameraAttributes().getImageReader().close();
                methodTool.getCameraAttributes().setImageReader(null);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            methodTool.getCameraAttributes().getCameraOpenCloseLock().release();
        }
    }

}
