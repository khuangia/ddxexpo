package com.kr.apkexpo.comm;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import com.kr.apkexpo.BuildConfig;

public class JDebug {

    private static class JLogHandler extends Handler {

        public JLogHandler() {
        }

        public JLogHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            super.handleMessage(msg);

            String textStr1 = msg.getData().getString("msg");
            mTxtView.append(textStr1);
        }
    }

    private static TextView mTxtView;
    private static JLogHandler mLogHandler;

    public static void init (TextView view) {

        mLogHandler = new JLogHandler();
        mTxtView = view;
    }

    public static void d(String stringInfo) {

        if (null != stringInfo) {
            try {
                Bundle msgBundle = new Bundle();
                msgBundle.putString("msg", stringInfo);

                Message msg = new Message();
                msg.setData(msgBundle);

                mLogHandler.sendMessage(msg);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
