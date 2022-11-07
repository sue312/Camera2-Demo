package com.wangxin.mycamera2.model;

import static com.wangxin.mycamera2.model.Config.STATE_PREVIEW;

import android.content.Context;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;

import java.io.File;
import java.util.concurrent.Semaphore;

public class CameraAttributes {

    private Context context;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest mPreviewRequest;
    private ImageReader mImageReader;
    private int mState = STATE_PREVIEW;
    private int mOrientation = 0;
    private File mFile, cameraFile;
    private String mCameraId, path;
    private boolean isBack = true;

    public CameraAttributes() {
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
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

    public int getOrientation() {
        return mOrientation;
    }

    public void setOrientation(int mOrientation) {
        this.mOrientation = mOrientation;
    }

    public File getFile() {
        return mFile;
    }

    public void setFile(File mFile) {
        this.mFile = mFile;
    }

    public File getCameraFile() {
        return cameraFile;
    }

    public void setCameraFile(File cameraFile) {
        this.cameraFile = cameraFile;
    }

    public String getCameraId() {
        return mCameraId;
    }

    public void setCameraId(String mCameraId) {
        this.mCameraId = mCameraId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isBack() {
        return isBack;
    }

    public void setBack(boolean back) {
        isBack = back;
    }
}
