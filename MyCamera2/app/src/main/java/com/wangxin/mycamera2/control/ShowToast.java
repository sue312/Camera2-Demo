package com.wangxin.mycamera2.control;

import android.widget.Toast;

import com.wangxin.mycamera2.model.CameraAttributes;

public class ShowToast {

    //Toast弹窗
    public static void showToast(final String text ,CameraAttributes cameraAttributes) {
        Toast.makeText(cameraAttributes.getContext(), text, Toast.LENGTH_SHORT).show();
    }

}
