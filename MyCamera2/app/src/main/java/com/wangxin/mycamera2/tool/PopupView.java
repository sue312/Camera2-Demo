package com.wangxin.mycamera2.tool;

import static com.wangxin.mycamera2.model.Config.STATE_PREVIEW;

import android.app.Activity;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.wangxin.mycamera2.R;
import com.wangxin.mycamera2.model.ActivityTool;
import com.wangxin.mycamera2.model.CameraAttributes;
import com.wangxin.mycamera2.model.MethodTool;

public class PopupView {

    public static PopupWindow popupWindow;

    public static void popupView(CameraAttributes cameraAttributes, ActivityTool activityTool, CameraCaptureSession.CaptureCallback mCaptureCallback, MethodTool methodTool, View v) {
        Activity getActivity = (Activity)  cameraAttributes.getContext();
        //绑定布局文件
        View popupView = getActivity.getLayoutInflater().inflate(R.layout.popup_view, null);

        ImageView btn1 = popupView.findViewById(R.id.btn_pop1);
        ImageView btn2 = popupView.findViewById(R.id.btn_pop2);
        ImageView btn3 = popupView.findViewById(R.id.btn_pop3);
        ImageView btn4 = popupView.findViewById(R.id.btn_pop4);

        //设置弹出框大小，点击空白取消
        popupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);

        popupWindow.showAsDropDown(v,v.getWidth()/5*2,0);

        //按钮
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setImage(activityTool,R.drawable.ic_vector_flash_off);

                setMode(cameraAttributes,mCaptureCallback,methodTool,CameraMetadata.FLASH_MODE_OFF);
            }
        });
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setImage(activityTool,R.drawable.ic_vector_flash_on);

                setMode(cameraAttributes,mCaptureCallback,methodTool,CameraMetadata.FLASH_MODE_OFF);
                cameraAttributes.getPreviewBuilder().set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
            }
        });
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setImage(activityTool,R.drawable.ic_vector_flash_torch);

                setMode(cameraAttributes,mCaptureCallback,methodTool,CameraMetadata.FLASH_MODE_TORCH);
            }
        });
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setImage(activityTool,R.drawable.ic_vector_flash_auto);

                setMode(cameraAttributes,mCaptureCallback,methodTool,CameraMetadata.FLASH_MODE_OFF);
                cameraAttributes.getPreviewBuilder().set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            }
        });
    }

    private static void setImage(ActivityTool activityTool, int resId) {
        activityTool.getFlashBtn().setImageResource(resId);
        popupWindow.dismiss();
    }

    private static void setMode(CameraAttributes cameraAttributes, CameraCaptureSession.CaptureCallback mCaptureCallback,MethodTool methodTool,int flashMode) {
        cameraAttributes.getPreviewBuilder().set(CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON);
        cameraAttributes.getPreviewBuilder().set(CaptureRequest.FLASH_MODE,
                flashMode);
        try {
            cameraAttributes.setState(STATE_PREVIEW);
            cameraAttributes.getCaptureSession().setRepeatingRequest(cameraAttributes.getPreviewBuilder().build(), mCaptureCallback, methodTool.getBackgroundThread().mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

}
