package com.wangxin.mycamera2;

import static com.wangxin.mycamera2.Config.EXTERNAL_STORAGE_DIRECTORY_ROOT;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class fileTool {

    public fileTool() {
    }

    private static String filePath;

    //获取文件夹名 wx6
    public static String getPhotoFileName() {
        String status = Environment.getExternalStorageState();
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            //showToast(R.string.camera_error_no_sdcard);
        } else {
            try {
                filePath = EXTERNAL_STORAGE_DIRECTORY_ROOT + getWorkCirlePhotoFileName();
            }catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return filePath ;
    }
    //给每张照片命名 wx6
    public static String getWorkCirlePhotoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "'IMG'_yyyy-MM-dd-HH-mm-ss", Locale.CHINA);
        return dateFormat.format(date) + ".jpg";
    }
    //创建新的文件夹 wx6
    public static File createNewFile(String path) {
        File file = new File(path);
        try {
           /* if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }*/

            if (!file.exists()) {
                file.mkdirs();
            }
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
        } catch (IOException e) {
            //LogUtil.e(TAG, e.getMessage());
        }
        return file;
    }

}
