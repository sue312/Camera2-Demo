package com.wangxin.mycamera2.control;

import static com.wangxin.mycamera2.model.Config.CURRENT_PROCESS_KEY;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class CustomChildThread extends Thread{

    private Handler mHandler;

    public CustomChildThread(Handler handler) {
        this.mHandler = handler;
    }

    @Override
    public void run() {
        try {
            //让当前执行的线程（即 CustomChildThread）睡眠 1s
            Thread.sleep(2000);

            //在子线程中创建一个消息对象
            Message childThreadMessage = new Message();
            childThreadMessage.obj = "modify_ui";
            //将该消息放入主线程的消息队列中
            mHandler.sendMessage(childThreadMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



}
