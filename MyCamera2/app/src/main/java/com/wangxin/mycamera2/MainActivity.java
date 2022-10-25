package com.wangxin.mycamera2;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

import static com.wangxin.mycamera2.Config.EXTERNAL_STORAGE_DIRECTORY_ROOT;
import static com.wangxin.mycamera2.Config.PERMISSIONS_CAMERA;
import static com.wangxin.mycamera2.Config.PERMISSIONS_STORAGE;
import static com.wangxin.mycamera2.Config.REQUEST_CAMERA_PERMISSION;
import static com.wangxin.mycamera2.Config.REQUEST_PERMISSION_CODE;
import static com.wangxin.mycamera2.Config.STATE_PREVIEW;
import static com.wangxin.mycamera2.Config.STATE_WAITING_PRECAPTURE;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private int mState = STATE_PREVIEW;

    private String mCameraId, path;
    private File mFile, cameraFile;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private TextureView mTextureView;
    private Button tackPictureBtn;
    private ImageReader mImageReader;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest mPreviewRequest;
    private BackgroundThread backgroundThread;
    private int mOrientation = 0;

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //取消状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        //获取Camera服务
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        //预览界面TextureView
        mTextureView = findViewById(R.id.texture);
        tackPictureBtn = findViewById(R.id.captureButton);

        //拍照方向矫正
        CameraOrientationListener orientationListener = new CameraOrientationListener(this);
        orientationListener.enable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //启动后台线程
        backgroundThread = new BackgroundThread();
        backgroundThread.startBackgroundThread();

        //如果mTextureView可用
        if (mTextureView.isAvailable()) {
            //打开相机
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            //监听mTextureView可用，打开相机
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

        //拍照点击事件
        tackPictureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lockFocus();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
        backgroundThread.stopBackgroundThread();
    }

    //锁定焦点 wx1
    private void lockFocus() {
        try {
            // 相机锁定焦点的方法
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            //告诉 #mCaptureCallback 等待锁定。
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewBuilder.build(), mCaptureCallback,
                    backgroundThread.mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

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
        mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG,/*maxImages*/7);
        //注册一个侦听器，以便在 ImageReader 中有新图像可用时调用。
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, backgroundThread.mBackgroundHandler);

        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mCameraManager.openCamera(mCameraId, mStateCallback, backgroundThread.mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    //动态申请权限 wx4
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //摄像头
        if (requestCode == REQUEST_PERMISSION_CODE) {
            //用户允许改权限，0表示允许，-1表示拒绝 PERMISSION_GRANTED = 0， PERMISSION_DENIED = -1
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //这里进行授权被允许的处理
                for (int i = 0; i < permissions.length; i++) {
                    Log.d("MainActivity1", "申请的权限为：" + permissions[i] + ",申请结果：" + grantResults[i]);
                }
            } else {
                //这里进行权限被拒绝的处理
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

    //获取mCameraId值 wx5
    private void getCameraId() {
        try {
            //按标识符返回当前连接的相机设备列表，包括可能被其他相机 API 客户端使用的相机
            for (String cameraId : mCameraManager.getCameraIdList()) {
                //可获取指定摄像头的相关特性
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                mCameraId = cameraId;
                Log.d("wangxin666","mCameraId = " + mCameraId);
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
            path  = fileTool.getPhotoFileName();
            cameraFile = fileTool.createNewFile(path);
            //文件储存
            mFile = new File(cameraFile.toURI());

            Intent intent = new Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(cameraFile));
            sendBroadcast(intent);

            backgroundThread.mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
        }
    };

    //获取mStateCallback wx5
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }
    };

    //预览 wx6
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            //assert(texture != null);

            Log.d("wangxin666","mTextureView.getWidth() = " + mTextureView.getWidth()  + " mTextureView.getHeight()" + mTextureView.getHeight());
            // 我们将默认缓冲区的大小配置为我们想要的相机预览大小。
            startOrientationChangeListener();
            if ( mOrientation == 90 || mOrientation == 270) {
                texture.setDefaultBufferSize(mTextureView.getWidth(), mTextureView.getHeight());
            } else {
                texture.setDefaultBufferSize(mTextureView.getHeight(), mTextureView.getWidth());
            }
            // 这是我们需要开始预览的输出表面。
            Surface surface = new Surface(texture);

            //创建预览的请求
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(surface);//预览时，是将Surface()作为目标
            mState = STATE_PREVIEW;
            mCameraDevice.createCaptureSession(
                    Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // 相机已经关闭
                            if (null == mCameraDevice) {
                                return;
                            }
                            mCaptureSession = cameraCaptureSession;
                            try {
                                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                                mPreviewRequest = mPreviewBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, backgroundThread.mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                                Log.e("linc","set preview builder failed."+e.getMessage());
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(MainActivity.this, "Camera configuration Failed", Toast.LENGTH_SHORT).show();
                        }
                    },backgroundThread.mBackgroundHandler);
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
                    mCaptureSession = session;
                    checkState(result);
                }

                @Override
                public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                                CaptureResult partialResult) {
                    mCaptureSession = session;
                    checkState(partialResult);
                }

                private void checkState(CaptureResult result) {
                    switch (mState) {
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
            if (null == mCameraDevice) {
                return;
            }
            // 这是我们用来拍照的 Capture RequestBuilder。
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());//拍照时，是将mImageReader.getSurface()作为目标

            // 使用与预览相同的 AE 和 AF 模式。
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 方向
            startOrientationChangeListener();

            Log.d("wangxin666","mOrientation = " + mOrientation);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, mOrientation);

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    showToast("Saved: " + mFile);

                    unlockFocus();//恢复预览
                }
            };
            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //解锁焦点  wx9
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), mCaptureCallback, backgroundThread.mBackgroundHandler);
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


    //关闭Camera  wx0
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    //拍照方向矫正
    private void startOrientationChangeListener() {
        OrientationEventListener mOrEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int rotation) {
                if (((rotation >= 0) && (rotation <= 45)) || (rotation > 315)) {
                    rotation = 90;
                } else if ((rotation > 45) && (rotation <= 135)) {
                    rotation = 180;
                } else if ((rotation > 135) && (rotation <= 225)) {
                    rotation = 270;
                } else if ((rotation > 225) && (rotation <= 315)) {
                    rotation = 0;
                } else {
                    rotation = 0;
                }
                if (rotation == mOrientation) {
                    return;
                }
                mOrientation = rotation;
            }
        };
        mOrEventListener.enable();
    }

}