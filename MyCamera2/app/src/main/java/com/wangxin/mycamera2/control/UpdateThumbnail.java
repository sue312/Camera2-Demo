package com.wangxin.mycamera2.control;

import static com.wangxin.mycamera2.model.Config.EXTERNAL_STORAGE_DIRECTORY_ROOT;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;

import com.wangxin.mycamera2.model.ActivityTool;
import com.wangxin.mycamera2.model.CameraAttributes;
import com.wangxin.mycamera2.model.MethodTool;

import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;

public class UpdateThumbnail {

    public UpdateThumbnail() {
    }

    public static void updateThumb(CameraAttributes cameraAttributes, ActivityTool activityTool) {
        Bitmap bitmap = getThumb(cameraAttributes.getContext());
        if (bitmap != null) {
            activityTool.getViewImageBtn().setImageBitmap(bitmap);
            activityTool.getViewImageBtn().setClickable(true);
        } else {
            activityTool.getViewImageBtn().setClickable(false);
        }
    }

    //获取缩略图
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
    //打开相册最后一张照片
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
