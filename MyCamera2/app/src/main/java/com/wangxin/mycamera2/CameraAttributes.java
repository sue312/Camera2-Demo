package com.wangxin.mycamera2;

import static com.wangxin.mycamera2.Config.STATE_PREVIEW;

import android.content.Context;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;

import java.util.concurrent.Semaphore;

public class CameraAttributes {

    private Context context;
    private CameraCaptureSession.CaptureCallback mCaptureCallback;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest mPreviewRequest;
    private ImageReader mImageReader;
    private int mState = STATE_PREVIEW;

    public CameraAttributes() {
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public CameraCaptureSession.CaptureCallback getCaptureCallback() {
        return mCaptureCallback;
    }

    public void setCaptureCallback(CameraCaptureSession.CaptureCallback mCaptureCallback) {
        this.mCaptureCallback = mCaptureCallback;
    }

    public CameraDevice getCameraDevice() {
        return mCameraDevice;
    }

    public void setCameraDevice(CameraDevice mCameraDevice) {
        this.mCameraDevice = mCameraDevice;
    }

    public Semaphore getCameraOpenCloseLock() {
        return mCameraOpenCloseLock;
    }

    public void setCameraOpenCloseLock(Semaphore mCameraOpenCloseLock) {
        this.mCameraOpenCloseLock = mCameraOpenCloseLock;
    }

    public CameraManager getCameraManager() {
        return mCameraManager;
    }

    public void setCameraManager(CameraManager mCameraManager) {
        this.mCameraManager = mCameraManager;
    }

    public CaptureRequest.Builder getPreviewBuilder() {
        return mPreviewBuilder;
    }

    public void setPreviewBuilder(CaptureRequest.Builder mPreviewBuilder) {
        this.mPreviewBuilder = mPreviewBuilder;
    }

    public CameraCaptureSession getCaptureSession() {
        return mCaptureSession;
    }

    public void setCaptureSession(CameraCaptureSession mCaptureSession) {
        this.mCaptureSession = mCaptureSession;
    }

    public CaptureRequest getPreviewRequest() {
        return mPreviewRequest;
    }

    public void setPreviewRequest(CaptureRequest mPreviewRequest) {
        this.mPreviewRequest = mPreviewRequest;
    }

    public ImageReader getImageReader() {
        return mImageReader;
    }

    public void setImageReader(ImageReader mImageReader) {
        this.mImageReader = mImageReader;
    }

    public int getState() {
        return mState;
    }

    public void setState(int mState) {
        this.mState = mState;
    }
}
