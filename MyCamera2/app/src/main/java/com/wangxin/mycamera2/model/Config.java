package com.wangxin.mycamera2.model;

import android.Manifest;
import android.os.Environment;

public class Config {

    public static final int STATE_WAITING_PRECAPTURE = 2;
    public static final int STATE_PREVIEW = 1;

    //读写权限
    public static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    public static String[] PERMISSIONS_CAMERA = {
            Manifest.permission.CAMERA};
    //请求状态码
    public static final int REQUEST_PERMISSION_CODE = 1;

    /** 外置存储卡根路径 */
    public static final String SEPARATOR = System.getProperty("file.separator");
    public static final String EXTERNAL_STORAGE_DIRECTORY_ROOT =
            Environment.getExternalStorageDirectory().getAbsolutePath()+ SEPARATOR + "DCIM" + SEPARATOR + "Camera2" + SEPARATOR ;

    public static final String CURRENT_PROCESS_KEY = "CURRENT_PROCESS";
}
