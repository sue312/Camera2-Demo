package com.wangxin.mycamera2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;

import com.wangxin.mycamera2.control.BackgroundThread;
import com.wangxin.mycamera2.control.CloseCamera;
import com.wangxin.mycamera2.control.FocusController;
import com.wangxin.mycamera2.control.CameraOrientationListener;
import com.wangxin.mycamera2.control.CustomChildThread;
import com.wangxin.mycamera2.control.FileTool;
import com.wangxin.mycamera2.control.UpdateThumbnail;
import com.wangxin.mycamera2.model.ActivityTool;
import com.wangxin.mycamera2.model.CameraAttributes;
import com.wangxin.mycamera2.model.CameraCallback;
import com.wangxin.mycamera2.model.MethodTool;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    MethodTool methodTool = new MethodTool();
    ActivityTool activityTool = new ActivityTool();
    CameraAttributes cameraAttributes = new CameraAttributes();
    CameraCallback cameraCallback = new CameraCallback(cameraAttributes,methodTool,activityTool);

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
        activityTool.setFlipBtn(findViewById(R.id.flipButton));
        activityTool.getFlipBtn().setOnClickListener(this);
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

        methodTool.setCustomThread(new CustomChildThread(mHandler));

        //更新小窗口图片
        UpdateThumbnail.updateThumb(cameraAttributes,activityTool);

        cameraCallback.checkCamera();

    }

    @Override
    protected void onPause() {
        super.onPause();
        CloseCamera.closeCamera(cameraAttributes);
        methodTool.getBackgroundThread().stopBackgroundThread();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.captureButton:
                FocusController.lockFocus(cameraAttributes,methodTool,cameraCallback.mCaptureCallback);
                if (!methodTool.getCustomThread().isAlive()) {
                    methodTool.setCustomThread(new CustomChildThread(mHandler));
                    methodTool.getCustomThread().start();
                }
                break;
            case R.id.imageButton:
                FileTool.getImage(cameraAttributes.getContext());
                break;
            case R.id.flipButton:
                switchCamera(cameraAttributes);
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

}