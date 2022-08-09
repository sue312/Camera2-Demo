package com.wangxin.mycamera2;

import android.Manifest;
import android.content.Context;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class customCarmeraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private static final int STATE_PREVIEW = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private int mState = STATE_PREVIEW;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    ///为了使照片竖直显示
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private CameraManager mCameraManagerm;
    private CameraDevice mCameraDevice;
    private String mCameraId;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private TextureView mTextureView;

    private ImageReader mImageReader;
    private File mFile;

    private CaptureRequest.Builder mPreviewBuilder;
    private CaptureRequest mPreviewRequest;
    private CameraCaptureSession mCaptureSession;


    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
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

    /**
     *
     * @TextureView.SurfaceTextureListener：public static interface
     * @onSurfaceTextureAvailable：Invoked when a TextureView's SurfaceTexture is ready for use.
     * @onSurfaceTextureSizeChanged：Invoked when the SurfaceTexture's buffers size changed.
     * @onSurfaceTextureDestroyed：Invoked when the specified SurfaceTexture is about to be destroyed.
     * @onSurfaceTextureUpdated：Invoked when the specified SurfaceTexture is updated through updateTexImage().
     */
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

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(customCarmeraActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        try {
            if (null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());//拍照时，是将mImageReader.getSurface()作为目标

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    showToast("Saved: " + mFile);

                    //Log.d("customCarmeraActivity", mFile.toString());
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

    private CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                               TotalCaptureResult result) {
                    //Log.d("linc","mSessionCaptureCallback, onCaptureCompleted");
                    mCaptureSession = session;
                    checkState(result);
                }

                @Override
                public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                                CaptureResult partialResult) {
                    Log.d("linc","mSessionCaptureCallback,  onCaptureProgressed");
                    mCaptureSession = session;
                    checkState(partialResult);
                }

                private void checkState(CaptureResult result) {
                    switch (mState) {
                        case STATE_PREVIEW:
                            // We have nothing to do when the camera preview is working normally.
                            break;
                        case STATE_WAITING_PRECAPTURE:
                            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                            if (afState == null) {
                                captureStillPicture();
                            } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                                // CONTROL_AE_STATE can be null on some devices
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
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            //assert(texture != null);

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mTextureView.getWidth(), mTextureView.getHeight());
            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

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
                            Toast.makeText(customCarmeraActivity.this, "Camera configuration Failed", Toast.LENGTH_SHORT).show();
                        }
                    },mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @CameraDevice.StateCallback：public static abstract class
     * @onOpened：This method is called when the camera is opened.  We start camera preview here.
     * @onDisconnected：The method called when a camera device is no longer available for use.
     * @onError：The method called when a camera device has encountered a serious error.
     */
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

    //
    private void getCameraId() {
        try {
            //Return the list of currently connected camera devices by identifier, including cameras that may be in use by other camera API clients
            for (String cameraId : mCameraManagerm.getCameraIdList()) {
                //Query the capabilities of a camera device
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

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

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
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
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

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            //showToast("onImageAvailable: " );
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
        }
    };

    //TODO 执行完上面的请求权限后，系统会弹出提示框让用户选择是否允许改权限。选择的结果可以在回到接口中得知：
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //用户允许改权限，0表示允许，-1表示拒绝 PERMISSION_GRANTED = 0， PERMISSION_DENIED = -1
                //permission was granted, yay! Do the contacts-related task you need to do.
                //这里进行授权被允许的处理
            } else {
                //permission denied, boo! Disable the functionality that depends on this permission.
                //这里进行权限被拒绝的处理
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Opens the camera specified by
     */
    private void openCamera(int width, int height) {
        //检查相机服务的访问权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(this,"Lacking privileges to access camera service, please request permission first",Toast.LENGTH_SHORT).show();
            //Log.e("customCarmeraActivity.openCamera","Lacking privileges to access camera service, please request permission first");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);//API21后，向用户请求相机使用权限，然后执行onRequestPermissionsResult回调
            return;
        }

        getCameraId();
        //assert(mCameraId != null);

        mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG,/*maxImages*/7);
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

    /**
     * Closes the current {@link CameraDevice}.
     */
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
    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
     /*       mCaptureSession.capture(mPreviewBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);*/
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mCameraManagerm = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        mTextureView = findViewById(R.id.texture);

        mFile = new File(getExternalFilesDir(null), "pic.jpg");

        Button tackPictureBtn = findViewById(R.id.captureButton);
        tackPictureBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                //Log.i("linc", "take picture");
                lockFocus();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();

        if(mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else{
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
        stopBackgroundThread();
    }
}
