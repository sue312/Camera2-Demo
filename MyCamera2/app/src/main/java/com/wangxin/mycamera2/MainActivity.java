package com.wangxin.mycamera2;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

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
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int STATE_PREVIEW = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    //读写权限
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    //请求状态码
    private static int REQUEST_PERMISSION_CODE = 1;
    private int mState = STATE_PREVIEW;

    private String mCameraId;
    private static  String filePath = null;
    private   String  path ;
    private File cameraFile ;
    /** 外置存储卡根路径 */
    public static final String SEPARATOR = System.getProperty("file.separator");
    public static final String EXTERNAL_STORAGE_DIRECTORY_ROOT =
           Environment.getExternalStorageDirectory().getAbsolutePath()+ SEPARATOR + "DCIM" + SEPARATOR + "LUOSHUICHAO" + SEPARATOR ;

    private CameraManager mCameraManagerm;
    private CameraDevice mCameraDevice;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private TextureView mTextureView;
    private ImageReader mImageReader;
    private static File mFile;

    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest mPreviewRequest;

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //取消状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        //获取Camera服务
        mCameraManagerm = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        //预览界面TextureView
        mTextureView = findViewById(R.id.texture);

        //拍照点击事件
        Button tackPictureBtn = findViewById(R.id.captureButton);
        tackPictureBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                lockFocus();
            }
        });
    }

    @Override

    protected void onResume() {
        super.onResume();
        //启动后台线程
        startBackgroundThread();
        //如果mTextureView可用
        if(mTextureView.isAvailable()) {
            //打开相机
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else{
            //监听mTextureView可用，打开相机
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }
    //启动后台线程
    private void startBackgroundThread() {
        //后台线程
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        //后台处理程序
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    //监听mTextureView可用，打开相机
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

    //动态申请权限
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //摄像头
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //用户允许改权限，0表示允许，-1表示拒绝 PERMISSION_GRANTED = 0， PERMISSION_DENIED = -1
                //这里进行授权被允许的处理
            } else {
                //这里进行权限被拒绝的处理
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        //读写
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                Log.d("MainActivity1", "申请的权限为：" + permissions[i] + ",申请结果：" + grantResults[i]);
            }
        }
    }

    //监听图像可用
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            path  = getPhotoFileName();
            cameraFile = createNewFile(path);
            //文件储存
            mFile = new File(cameraFile.toURI());

            Intent intent = new Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(cameraFile));
            sendBroadcast(intent);

            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
        }
    };
    //将图像保存到的文件。
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile ;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                Log.d("TAG", "run: output");
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                Log.d("TAG", "run: IOException");
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    //给每张照片命名
    public static String getWorkCirlePhotoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "'IMG'_yyyy-MM-dd-HH-mm-ss", Locale.CHINA);
        return dateFormat.format(date) + ".jpg";
    }
    //路径
    public static File createNewFile(String path) {
        File file = new File(path);
        try {
           /* if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }*/

            if (!file.exists()) {
                file.mkdirs();
            }
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
        } catch (IOException e) {
            //LogUtil.e(TAG, e.getMessage());
        }
        return file;
    }
    public String getPhotoFileName() {
        String status = Environment.getExternalStorageState();
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            //showToast(R.string.camera_error_no_sdcard);
        } else {
            try {
                filePath = EXTERNAL_STORAGE_DIRECTORY_ROOT + getWorkCirlePhotoFileName();
            }catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return filePath ;
    }

    //打开相机
    private void openCamera(int width, int height) {
        //检查相机服务的访问权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);//API21后，向用户请求相机使用权限，然后执行onRequestPermissionsResult回调
            return;
        }
        //检查写权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
            return;
        }

        //获取mCameraId值
        getCameraId();

        //图像阅读器
        mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG,/*maxImages*/7);
        //注册一个侦听器，以便在 ImageReader 中有新图像可用时调用。
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            mCameraManagerm.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    //获取mCameraId值
    private void getCameraId() {
        try {
            //Return the list of currently connected camera devices by identifier, including cameras that may be in use by other camera API clients
            for (String cameraId : mCameraManagerm.getCameraIdList()) {
                //可获取指定摄像头的相关特性
                CameraCharacteristics characteristics = mCameraManagerm.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //获取mStateCallback
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

    //预览
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            //assert(texture != null);

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mTextureView.getWidth(), mTextureView.getHeight());
            // This is the output Surface we need to start preview.
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
                            // The camera is already closed
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
                                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                                Log.e("linc","set preview builder failed."+e.getMessage());
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(MainActivity.this, "Camera configuration Failed", Toast.LENGTH_SHORT).show();
                        }
                    },mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //捕获回调
    private CameraCaptureSession.CaptureCallback mCaptureCallback =
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

    //捕捉静态图片
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
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

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

    //Toast弹窗
    private void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
    //解锁焦点
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    //锁定焦点
    private void lockFocus() {
        try {
            // 相机锁定焦点的方法
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            //告诉 #mCaptureCallback 等待锁定。
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
        stopBackgroundThread();
    }
    //关闭Camera
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
    //停止后台线程
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}