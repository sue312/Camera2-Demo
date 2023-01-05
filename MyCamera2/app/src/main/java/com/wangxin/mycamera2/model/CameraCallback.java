package com.wangxin.mycamera2.model;

import static com.wangxin.mycamera2.model.Config.STATE_PREVIEW;
import static com.wangxin.mycamera2.model.Config.STATE_WAITING_PRECAPTURE;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.net.Uri;
import android.view.TextureView;

import com.wangxin.mycamera2.control.CameraController;
import com.wangxin.mycamera2.tool.FileTool;
import com.wangxin.mycamera2.tool.ImageSaver;

import java.io.File;

public class CameraCallback {

    private CameraAttributes cameraAttributes;
    private MethodTool methodTool;
    private ActivityTool activityTool;

    public CameraCallback(CameraAttributes cameraAttributes, MethodTool methodTool, ActivityTool activityTool) {
        this.cameraAttributes = cameraAttributes;
        this.methodTool = methodTool;
        this.activityTool = activityTool;
    }

    //监听mTextureView可用，打开相机 wx3
    public final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            CameraController.openCamera(width, height,cameraAttributes,methodTool,mStateCallback,mOnImageAvailableListener);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            //configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    //监听图像可用 wx5
    public final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            cameraAttributes.setPath(FileTool.getPhotoFileName());
            cameraAttributes.setCameraFile(FileTool.createNewFile(cameraAttributes.getPath()));
            //文件储存
            cameraAttributes.setFile(new File(cameraAttributes.getCameraFile().toURI()));

            Intent intent = new Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(cameraAttributes.getCameraFile()));
            cameraAttributes.getContext().sendBroadcast(intent);

            methodTool.getBackgroundThread().mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), cameraAttributes.getFile()));
        }
    };

    //获取mStateCallback wx5
    public final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            cameraAttributes.getCameraOpenCloseLock().release();
            cameraAttributes.setCameraDevice(cameraDevice);
            CameraController.createCameraPreviewSession(cameraAttributes,methodTool,activityTool,mCaptureCallback);
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraAttributes.getCameraOpenCloseLock().release();
            cameraDevice.close();
            cameraAttributes.setCameraDevice(null);
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraAttributes.getCameraOpenCloseLock().release();
            cameraDevice.close();
            cameraAttributes.setCameraDevice(null);
        }
    };

    //捕获回调 wx7
    public final CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                               TotalCaptureResult result) {
                    cameraAttributes.setCaptureSession(session);
                    checkState(result);
                }

                @Override
                public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                                CaptureResult partialResult) {
                    cameraAttributes.setCaptureSession(session);
                    checkState(partialResult);
                }

                private void checkState(CaptureResult result) {
                    switch (cameraAttributes.getState()) {
                        case STATE_PREVIEW:
                            // 当相机预览正常工作时，我们无事可做。
                            break;
                        case STATE_WAITING_PRECAPTURE:
                            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                            if (afState == null) {
                                CameraController.captureStillPicture(cameraAttributes,methodTool,mCaptureCallback);
                            } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                                // CONTROL_AE_STATE 在某些设备上可以为 null
                                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                                if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                                    //mState = STATE_PICTURE_TAKEN;
                                    CameraController.captureStillPicture(cameraAttributes,methodTool,mCaptureCallback);
                                } else {
                                    //runPrecaptureSequence();//视频拍摄
                                }
                            }
                            break;
                    }
                }

            };

    //检查并打开camera
    public void checkCamera() {
        //如果mTextureView可用
        if (activityTool.getTextureView().isAvailable()) {
            //打开相机
            CameraController.openCamera(activityTool.getTextureView().getWidth(), activityTool.getTextureView().getHeight(),cameraAttributes,methodTool,mStateCallback,mOnImageAvailableListener);
        } else {
            //监听mTextureView可用，打开相机
            activityTool.getTextureView().setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

}
