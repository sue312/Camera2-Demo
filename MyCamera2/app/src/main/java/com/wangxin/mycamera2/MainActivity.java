package com.wangxin.mycamera2;

import static com.wangxin.mycamera2.model.Config.EXTERNAL_STORAGE_DIRECTORY_ROOT;
import static com.wangxin.mycamera2.model.Config.PERMISSIONS_CAMERA;
import static com.wangxin.mycamera2.model.Config.PERMISSIONS_STORAGE;
import static com.wangxin.mycamera2.model.Config.REQUEST_PERMISSION_CODE;
import static com.wangxin.mycamera2.model.Config.STATE_PREVIEW;
import static com.wangxin.mycamera2.model.Config.STATE_WAITING_PRECAPTURE;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.wangxin.mycamera2.control.BackgroundThread;
import com.wangxin.mycamera2.control.CameraOrientationListener;
import com.wangxin.mycamera2.control.CloseCamera;
import com.wangxin.mycamera2.control.CustomChildThread;
import com.wangxin.mycamera2.control.FileTool;
import com.wangxin.mycamera2.control.ImageSaver;
import com.wangxin.mycamera2.control.UpdateThumbnail;
import com.wangxin.mycamera2.model.ActivityTool;
import com.wangxin.mycamera2.model.CameraAttributes;
import com.wangxin.mycamera2.model.Config;
import com.wangxin.mycamera2.model.MethodTool;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    MethodTool methodTool = new MethodTool();
    ActivityTool activityTool = new ActivityTool();
    CameraAttributes cameraAttributes = new CameraAttributes();

    //创建 Handler对象，并关联主线程消息队列
    public Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String type = (String) msg.obj;
            switch (type) {
                case "modify_ui":
                    //更新小窗口图片
                    UpdateThumbnail.updateThumb(cameraAttributes,activityTool);
                    break;
                default:
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //取消状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        //预览界面TextureView
        activityTool.setTextureView(findViewById(R.id.texture));
        activityTool.setTackPictureBtn(findViewById(R.id.captureButton));
        activityTool.getTackPictureBtn().setOnClickListener(this);
        activityTool.setViewImageBtn(findViewById(R.id.imageButton));
        activityTool.getViewImageBtn().setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //拍照方向矫正
        methodTool.setOrientationListener(new CameraOrientationListener(this));
        methodTool.getOrientationListener().enable();

        //获取Camera服务
        cameraAttributes.setCameraManager((CameraManager)getSystemService(Context.CAMERA_SERVICE));
        cameraAttributes.setContext(this);

        //启动后台线程
        methodTool.setBackgroundThread(new BackgroundThread());
        methodTool.getBackgroundThread().startBackgroundThread();

        methodTool.setCloseCamera(new CloseCamera());
        methodTool.setCustomThread(new CustomChildThread(mHandler));

        //更新小窗口图片
        UpdateThumbnail.updateThumb(cameraAttributes,activityTool);

        //如果mTextureView可用
        if (activityTool.getTextureView().isAvailable()) {
            //打开相机
            openCamera(activityTool.getTextureView().getWidth(), activityTool.getTextureView().getHeight());
        } else {
            //监听mTextureView可用，打开相机
            activityTool.getTextureView().setSurfaceTextureListener(mSurfaceTextureListener);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        methodTool.getCloseCamera().closeCamera();
        methodTool.getBackgroundThread().stopBackgroundThread();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.captureButton:
                lockFocus();
                if (!methodTool.getCustomThread().isAlive()) {
                    methodTool.setCustomThread(new CustomChildThread(mHandler));
                    methodTool.getCustomThread().start();
                }
                break;
            case R.id.imageButton:
                FileTool.getImage(cameraAttributes.getContext());
                break;
            default:
                break;
        }
    }

    //锁定焦点 wx1
    private void lockFocus() {
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

    //监听mTextureView可用，打开相机 wx3
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
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

    //打开相机 wx3
    private void openCamera(int width, int height) {
        //检查相机服务的访问权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //先判断有没有权限 ，没有就在这里进行权限的申请
            ActivityCompat.requestPermissions(this, PERMISSIONS_CAMERA, REQUEST_PERMISSION_CODE);
            //API21后，向用户请求相机使用权限，然后执行onRequestPermissionsResult回调
            return;
        }
        //检查写权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //先判断有没有权限 ，没有就在这里进行权限的申请
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
            return;
        }

        //获取mCameraId值
        getCameraId();

        //图像阅读器
        cameraAttributes.setImageReader(ImageReader.newInstance(width, height, ImageFormat.JPEG,/*maxImages*/7));
        //注册一个侦听器，以便在 ImageReader 中有新图像可用时调用。
        cameraAttributes.getImageReader().setOnImageAvailableListener(mOnImageAvailableListener, methodTool.getBackgroundThread().mBackgroundHandler);

        try {
            if (!cameraAttributes.getCameraOpenCloseLock().tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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
    private void getCameraId() {
        try {
            //按标识符返回当前连接的相机设备列表，包括可能被其他相机 API 客户端使用的相机
            for (String cameraId : cameraAttributes.getCameraManager().getCameraIdList()) {
                //可获取指定摄像头的相关特性
                CameraCharacteristics characteristics = cameraAttributes.getCameraManager().getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                cameraAttributes.setCameraId(cameraId);
                Log.d("wangxin666","mCameraId = " + cameraAttributes.getCameraId());
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //监听图像可用 wx5
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            cameraAttributes.setPath(FileTool.getPhotoFileName());
            cameraAttributes.setCameraFile(FileTool.createNewFile(cameraAttributes.getPath()));
            //文件储存
            cameraAttributes.setFile(new File(cameraAttributes.getCameraFile().toURI()));

            Intent intent = new Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(cameraAttributes.getCameraFile()));
            sendBroadcast(intent);

            methodTool.getBackgroundThread().mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), cameraAttributes.getFile()));
        }
    };

    //获取mStateCallback wx5
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            cameraAttributes.getCameraOpenCloseLock().release();
            cameraAttributes.setCameraDevice(cameraDevice);
            createCameraPreviewSession();
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

    //预览 wx6
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = activityTool.getTextureView().getSurfaceTexture();
            //assert(texture != null);

            Log.d("wangxin666","mTextureView.getWidth() = " + activityTool.getTextureView().getWidth()  + " mTextureView.getHeight()" + activityTool.getTextureView().getHeight());
            // 我们将默认缓冲区的大小配置为我们想要的相机预览大小。
            cameraAttributes.setOrientation(methodTool.getOrientationListener().startOrientationChangeListener(this));
            if ( cameraAttributes.getOrientation() == 90 || cameraAttributes.getOrientation() == 270) {
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
                                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                                cameraAttributes.setPreviewRequest(cameraAttributes.getPreviewBuilder().build());
                                cameraAttributes.getCaptureSession().setRepeatingRequest(cameraAttributes.getPreviewRequest(), mCaptureCallback, methodTool.getBackgroundThread().mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                                Log.e("linc","set preview builder failed."+e.getMessage());
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(MainActivity.this, "Camera configuration Failed", Toast.LENGTH_SHORT).show();
                        }
                    }, methodTool.getBackgroundThread().mBackgroundHandler);
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
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 方向
            cameraAttributes.setOrientation(methodTool.getOrientationListener().startOrientationChangeListener(this));
            Log.d("wangxin666","mOrientation = " + cameraAttributes.getOrientation());
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, cameraAttributes.getOrientation());

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    showToast("Saved: " + cameraAttributes.getFile());

                    unlockFocus();//恢复预览
                }
            };
            cameraAttributes.getCaptureSession().stopRepeating();
            cameraAttributes.getCaptureSession().abortCaptures();
            cameraAttributes.getCaptureSession().capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //解锁焦点  wx9
    private void unlockFocus() {
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

    //Toast弹窗
    public void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

}