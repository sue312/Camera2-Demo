package com.wangxin.mycamera2.control;

import android.content.Context;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.OrientationEventListener;

public class CameraOrientationListener extends OrientationEventListener {

    public int mCurrentNormalizedOrientation;
    private int mOrientation = 0;

    public CameraOrientationListener(Context context) {
        super(context, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onOrientationChanged(final int orientation) {
        //Log.i("wangxin666", "当前屏幕手持角度:" + orientation + "°");
        if (orientation != ORIENTATION_UNKNOWN) {
            mCurrentNormalizedOrientation = normalize(orientation);
            Log.d("wangxin666","mCurrentNormalizedOrientation = " + mCurrentNormalizedOrientation);
        }
    }

    private int normalize(int degrees) {
        if (degrees > 315 || degrees <= 45) {
            return 0;
        }
        if (degrees > 45 && degrees <= 135) {
            return 90;
        }
        if (degrees > 135 && degrees <= 225) {
            return 180;
        }
        if (degrees > 225 && degrees <= 315) {
            return 270;
        }
        throw new RuntimeException("The physics as we know them are no more. Watch out for anomalies.");
    }

    //拍照方向矫正
    public int startOrientationChangeListener(Context context) {
        OrientationEventListener mOrEventListener = new OrientationEventListener(context) {
            @Override
            public void onOrientationChanged(int rotation) {
                if (((rotation >= 0) && (rotation <= 45)) || (rotation > 315)) {
                    rotation = 90;
                } else if ((rotation > 45) && (rotation <= 135)) {
                    rotation = 180;
                } else if ((rotation > 135) && (rotation <= 225)) {
                    rotation = 270;
                } else if ((rotation > 225) && (rotation <= 315)) {
                    rotation = 0;
                } else {
                    rotation = 0;
                }
                if (rotation == mOrientation) {
                    return;
                }
                mOrientation = rotation;
            }
        };
        mOrEventListener.enable();
        return mOrientation;
    }

}
