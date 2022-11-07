package com.wangxin.mycamera2.control;

import android.graphics.Bitmap;

import com.wangxin.mycamera2.model.ActivityTool;
import com.wangxin.mycamera2.model.CameraAttributes;
import com.wangxin.mycamera2.model.MethodTool;

import android.os.Handler;

public class UpdateThumbnail {

    public UpdateThumbnail() {
    }

    public static void updateThumb(CameraAttributes cameraAttributes, ActivityTool activityTool) {
        Bitmap bitmap = FileTool.getThumb(cameraAttributes.getContext());
        if (bitmap != null) {
            activityTool.getViewImageBtn().setImageBitmap(bitmap);
            activityTool.getViewImageBtn().setClickable(true);
        } else {
            activityTool.getViewImageBtn().setClickable(false);
        }
    }

}
