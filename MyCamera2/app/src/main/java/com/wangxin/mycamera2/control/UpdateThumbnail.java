package com.wangxin.mycamera2.control;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.wangxin.mycamera2.R;
import com.wangxin.mycamera2.model.ActivityTool;
import com.wangxin.mycamera2.model.CameraAttributes;

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
