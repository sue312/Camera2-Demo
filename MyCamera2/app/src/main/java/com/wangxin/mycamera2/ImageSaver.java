package com.wangxin.mycamera2;

import android.content.Intent;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

//将图像保存到的文件。 wx6
public class ImageSaver implements Runnable {

    /**
     * The JPEG image
     */
    private final Image mImage;

    /**
     * The file we save the image into.
     */
    private File mFile;

    ImageSaver(Image image, File file) {
        mImage = image;
        mFile = file;
    }

    @Override
    public void run() {
        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        FileOutputStream output = null;
        try {
            Log.d("TAG", "run: output");
            output = new FileOutputStream(mFile);
            output.write(bytes);
        } catch (IOException e) {
            Log.d("TAG", "run: IOException");
            e.printStackTrace();
        } finally {
            mImage.close();
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
