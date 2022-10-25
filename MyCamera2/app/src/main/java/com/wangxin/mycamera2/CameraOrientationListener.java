package com.wangxin.mycamera2;

import android.content.Context;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.OrientationEventListener;

public class CameraOrientationListener extends OrientationEventListener {

    public int mCurrentNormalizedOrientation;

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

}
