package com.wangxin.mycamera2.control;

import static com.wangxin.mycamera2.model.Config.EXTERNAL_STORAGE_DIRECTORY_ROOT;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileTool {

    public FileTool() {
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

    public static Bitmap getThumb(Context context) {
        String selection = MediaStore.Images.Media.DATA + " like ?";
        String path = EXTERNAL_STORAGE_DIRECTORY_ROOT;
        String[] selectionArgs = {path + "%"};
        Uri originalUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(originalUri, null, selection, selectionArgs,
                MediaStore.Images.Media.DATE_TAKEN + " desc");
        Bitmap bitmap = null;
        if (cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range") long thumbNailsId = cursor.getLong(cursor.getColumnIndex("_ID"));
            //generate uri
            Uri mCurrentUri = Uri.parse("content://media/external/images/media/");
            mCurrentUri = ContentUris.withAppendedId(mCurrentUri, thumbNailsId);

            bitmap = MediaStore.Images.Thumbnails.getThumbnail(cr,
                    thumbNailsId, MediaStore.Images.Thumbnails.MICRO_KIND, null);

        }
        cursor.close();
        return bitmap;
    }

    public static void getImage(Context context) {
        String selection = MediaStore.Images.Media.DATA + " like ?";
        String path = EXTERNAL_STORAGE_DIRECTORY_ROOT;
        String[] selectionArgs = {path + "%"};
        Uri originalUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver cr = context.getContentResolver();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Cursor cursor = cr.query(originalUri, null, selection, selectionArgs,
                MediaStore.Images.Media.DATE_TAKEN + " desc");
        Bitmap bitmap = null;
        if (cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range") long thumbNailsId = cursor.getLong(cursor.getColumnIndex("_ID"));
            //generate uri
            Uri mCurrentUri = Uri.parse("content://media/external/images/media/");
            mCurrentUri = ContentUris.withAppendedId(mCurrentUri, thumbNailsId);
            intent.setDataAndType(mCurrentUri,"image/*");
            context.startActivity(intent);
        }
    }

}
