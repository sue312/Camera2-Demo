package com.wangxin.mycamera2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;


import com.wangxin.mycamera2.tool.BackgroundThread;
import com.wangxin.mycamera2.tool.CloseCamera;
import com.wangxin.mycamera2.control.FocusController;
import com.wangxin.mycamera2.control.CameraOrientationListener;
import com.wangxin.mycamera2.tool.CustomChildThread;
import com.wangxin.mycamera2.control.UpdateThumbnail;
import com.wangxin.mycamera2.model.ActivityTool;
import com.wangxin.mycamera2.model.CameraAttributes;
import com.wangxin.mycamera2.model.CameraCallback;
import com.wangxin.mycamera2.model.MethodTool;
import com.wangxin.mycamera2.tool.PopupView;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    MethodTool methodTool = new MethodTool();
    ActivityTool activityTool = new ActivityTool();
    CameraAttributes cameraAttributes = new CameraAttributes();
    CameraCallback cameraCallback = new CameraCallback(cameraAttributes,methodTool,activityTool);

    //创建 Handler对象，并关联主线程消息队列
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
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
        activityTool.setFlipBtn(findViewById(R.id.flipButton));
        activityTool.getFlipBtn().setOnClickListener(this);
        activityTool.setFlashBtn(findViewById(R.id.btn_flash));
        activityTool.getFlashBtn().setOnClickListener(this);

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
        //设置子线程，刷新缩略图
        methodTool.setCustomThread(new CustomChildThread(mHandler));
        UpdateThumbnail.updateThumb(cameraAttributes,activityTool);

        //检查并打开camera
        cameraCallback.checkCamera();

    }

    @Override
    protected void onPause() {
        super.onPause();
        //关闭Camera,关闭后台线程
        CloseCamera.closeCamera(cameraAttributes);
        methodTool.getBackgroundThread().stopBackgroundThread();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.captureButton:
                //锁定焦点
                FocusController.lockFocus(cameraAttributes,methodTool,cameraCallback.mCaptureCallback);
                //启动子线程，刷新缩略图刷新缩略图
                StartCustomThread();
                break;
            case R.id.imageButton:
                //刷新缩略图
                UpdateThumbnail.getImage(cameraAttributes.getContext());
                break;
            case R.id.flipButton:
                //切换前后摄
                switchCamera(cameraAttributes);
                break;
            case R.id.btn_flash:
                //切换闪光灯模式
                PopupView.popupView(cameraAttributes,activityTool,cameraCallback.mCaptureCallback,methodTool,v);
                break;
            default:
                break;
        }
    }

    //切换前后摄
    private void switchCamera(CameraAttributes cameraAttributes) {
        cameraAttributes.setBack(!cameraAttributes.isBack());
        CloseCamera.closeCamera(cameraAttributes);
        cameraCallback.checkCamera();
    }

    //启动子线程，刷新缩略图刷新缩略图
    private void StartCustomThread() {
        if (!methodTool.getCustomThread().isAlive()) {
            methodTool.setCustomThread(new CustomChildThread(mHandler));
            methodTool.getCustomThread().start();
        }
    }



}