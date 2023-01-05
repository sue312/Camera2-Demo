package com.wangxin.mycamera2.control;

import static com.wangxin.mycamera2.model.Config.PERMISSIONS_CAMERA;
import static com.wangxin.mycamera2.model.Config.PERMISSIONS_STORAGE;
import static com.wangxin.mycamera2.model.Config.REQUEST_PERMISSION_CODE;
import static com.wangxin.mycamera2.model.Config.STATE_PREVIEW;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.wangxin.mycamera2.model.ActivityTool;
import com.wangxin.mycamera2.model.CameraAttributes;
import com.wangxin.mycamera2.model.MethodTool;
import com.wangxin.mycamera2.tool.ShowToast;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class CameraController {

    //打开相机 wx3
    public static void openCamera(int width, int height, CameraAttributes cameraAttributes, MethodTool methodTool ,
                                  CameraDevice.StateCallback mStateCallback ,ImageReader.OnImageAvailableListener mOnImageAvailableListener) {
        //检查相机服务的访问权限
        if (ContextCompat.checkSelfPermission(cameraAttributes.getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //先判断有没有权限 ，没有就在这里进行权限的申请
            ActivityCompat.requestPermissions((Activity) cameraAttributes.getContext(), PERMISSIONS_CAMERA, REQUEST_PERMISSION_CODE);
            //API21后，向用户请求相机使用权限，然后执行onRequestPermissionsResult回调
            return;
        }
        //检查写权限
        if (ContextCompat.checkSelfPermission(cameraAttributes.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //先判断有没有权限 ，没有就在这里进行权限的申请
            ActivityCompat.requestPermissions((Activity) cameraAttributes.getContext(), PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
            return;
        }

        //获取mCameraId值
        getCameraId(cameraAttributes);

        //图像阅读器
        cameraAttributes.setImageReader(ImageReader.newInstance(width, height, ImageFormat.JPEG,/*maxImages*/7));
        //注册一个侦听器，以便在 ImageReader 中有新图像可用时调用。
        cameraAttributes.getImageReader().setOnImageAvailableListener(mOnImageAvailableListener, methodTool.getBackgroundThread().mBackgroundHandler);

        try {
            if (!cameraAttributes.getCameraOpenCloseLock().tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            if (ActivityCompat.checkSelfPermission(cameraAttributes.getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            cameraAttributes.getCameraManager().openCamera(cameraAttributes.getCameraId(), mStateCallback, methodTool.getBackgroundThread().mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    //获取mCameraId值 wx5
    private static void getCameraId(CameraAttributes cameraAttributes) {
        try {
            //按标识符返回当前连接的相机设备列表，包括可能被其他相机 API 客户端使用的相机
            for (String cameraId : cameraAttributes.getCameraManager().getCameraIdList()) {
                //可获取指定摄像头的相关特性
                CameraCharacteristics characteristics = cameraAttributes.getCameraManager().getCameraCharacteristics(cameraId);
                //判断前后摄像头
                if (!cameraAttributes.isBack()) {
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                        continue;
                    }
                }else {
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                        continue;
                    }
                }
                cameraAttributes.setCameraId(cameraId);
                Log.d("wangxin666","mCameraId = " + cameraAttributes.getCameraId());
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //预览 wx6
    public static void createCameraPreviewSession(CameraAttributes cameraAttributes, MethodTool methodTool,
                                            ActivityTool activityTool,CameraCaptureSession.CaptureCallback mCaptureCallback) {
        try {
            SurfaceTexture texture = activityTool.getTextureView().getSurfaceTexture();

            Log.d("wangxin666","mTextureView.getWidth() = " + activityTool.getTextureView().getWidth()  + " mTextureView.getHeight()" + activityTool.getTextureView().getHeight());
            // 我们将默认缓冲区的大小配置为我们想要的相机预览大小。
            isBack(cameraAttributes,methodTool);

            //判断设备方向
            if ( activityTool.getTextureView().getWidth() >= activityTool.getTextureView().getHeight()) {
                texture.setDefaultBufferSize(activityTool.getTextureView().getWidth(), activityTool.getTextureView().getHeight());
            } else {
                texture.setDefaultBufferSize(activityTool.getTextureView().getHeight(), activityTool.getTextureView().getWidth());
            }

            // 这是我们需要开始预览的输出表面。
            Surface surface = new Surface(texture);

            //创建预览的请求
            cameraAttributes.setPreviewBuilder(cameraAttributes.getCameraDevice().createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW));
            cameraAttributes.getPreviewBuilder().addTarget(surface);//预览时，是将Surface()作为目标
            cameraAttributes.setState(STATE_PREVIEW);
            cameraAttributes.getCameraDevice().createCaptureSession(
                    Arrays.asList(surface, cameraAttributes.getImageReader().getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // 相机已经关闭
                            if (null == cameraAttributes.getCameraDevice()) {
                                return;
                            }
                            cameraAttributes.setCaptureSession(cameraCaptureSession);
                            try {
                                cameraAttributes.getPreviewBuilder().set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                cameraAttributes.getPreviewBuilder().set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON);
                                cameraAttributes.getPreviewBuilder().set(CaptureRequest.FLASH_MODE,
                                        CameraMetadata.FLASH_MODE_OFF);

                                cameraAttributes.setPreviewRequest(cameraAttributes.getPreviewBuilder().build());
                                cameraAttributes.getCaptureSession().setRepeatingRequest(cameraAttributes.getPreviewRequest(), mCaptureCallback, methodTool.getBackgroundThread().mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                                Log.e("linc","set preview builder failed."+e.getMessage());
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            ShowToast.showToast("Camera configuration Failed",cameraAttributes);
                        }
                    }, methodTool.getBackgroundThread().mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //捕捉静态图片  wx8
    public static void captureStillPicture(CameraAttributes cameraAttributes,MethodTool methodTool,CameraCaptureSession.CaptureCallback mCaptureCallback) {
        try {
            if (null == cameraAttributes.getCameraDevice()) {
                return;
            }
            // 这是我们用来拍照的 Capture RequestBuilder。
            final CaptureRequest.Builder captureBuilder =
                    cameraAttributes.getCameraDevice().createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(cameraAttributes.getImageReader().getSurface());//拍照时，是将mImageReader.getSurface()作为目标

            // 使用与预览相同的 AE 和 AF 模式。
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            // 方向,我们将默认缓冲区的大小配置为我们想要的相机预览大小。
            isBack(cameraAttributes,methodTool);

            Log.d("wangxin666", "mOrientation = " + cameraAttributes.getOrientation());

            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, cameraAttributes.getOrientation());

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    ShowToast.showToast("Saved: " + cameraAttributes.getFile() , cameraAttributes);

                    FocusController.unlockFocus(cameraAttributes,methodTool,mCaptureCallback);//恢复预览
                }
            };
            cameraAttributes.getCaptureSession().stopRepeating();
            cameraAttributes.getCaptureSession().abortCaptures();
            cameraAttributes.getCaptureSession().capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 判断使用的是否是前摄
    private static void isBack(CameraAttributes cameraAttributes, MethodTool methodTool) {
        // 我们将默认缓冲区的大小配置为我们想要的相机预览大小。
        if (cameraAttributes.isBack()) {
            //ShowToast.showToast("isBack 1",cameraAttributes);
            cameraAttributes.setOrientation(methodTool.getOrientationListener().startOrientationChangeListener(cameraAttributes.getContext()));
        }else {
            //ShowToast.showToast("isBack 2",cameraAttributes);
            cameraAttributes.setOrientation(methodTool.getOrientationListener().startOrientationChangeListener2(cameraAttributes.getContext()));
        }
    }

}
