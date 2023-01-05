package com.wangxin.mycamera2.model;

import android.view.TextureView;
import android.widget.Button;
import android.widget.ImageView;

import com.wangxin.mycamera2.UI.CircleImageView;

public class ActivityTool {

    private TextureView mTextureView;
    private ImageView tackPictureBtn;
    private CircleImageView viewImageBtn;
    private ImageView flipBtn;
    private ImageView flashBtn;

    public ActivityTool() {
    }

    public TextureView getTextureView() {
        return mTextureView;
    }

    public void setTextureView(TextureView mTextureView) {
        this.mTextureView = mTextureView;
    }

    public ImageView getTackPictureBtn() {
        return tackPictureBtn;
    }

    public void setTackPictureBtn(ImageView tackPictureBtn) {
        this.tackPictureBtn = tackPictureBtn;
    }

    public CircleImageView getViewImageBtn() {
        return viewImageBtn;
    }

    public void setViewImageBtn(CircleImageView viewImageBtn) {
        this.viewImageBtn = viewImageBtn;
    }

    public ImageView getFlipBtn() {
        return flipBtn;
    }

    public void setFlipBtn(ImageView flipBtn) {
        this.flipBtn = flipBtn;
    }

    public ImageView getFlashBtn() {
        return flashBtn;
    }

    public void setFlashBtn(ImageView flashBtn) {
        this.flashBtn = flashBtn;
    }
}
