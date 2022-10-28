package com.wangxin.mycamera2.model;

import android.view.TextureView;
import android.widget.Button;

public class ActivityTool {

    private TextureView mTextureView;
    private Button tackPictureBtn;

    public ActivityTool() {
    }

    public TextureView getTextureView() {
        return mTextureView;
    }

    public void setTextureView(TextureView mTextureView) {
        this.mTextureView = mTextureView;
    }

    public Button getTackPictureBtn() {
        return tackPictureBtn;
    }

    public void setTackPictureBtn(Button tackPictureBtn) {
        this.tackPictureBtn = tackPictureBtn;
    }
}
